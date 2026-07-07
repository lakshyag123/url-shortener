package com.paytm.urlshortener.service.impl;

import com.paytm.urlshortener.dto.CreateShortUrlRequest;
import com.paytm.urlshortener.dto.CreateShortUrlResponse;
import com.paytm.urlshortener.dto.UrlStatsResponse;
import com.paytm.urlshortener.exception.DuplicateAliasException;
import com.paytm.urlshortener.exception.ResourceNotFoundException;
import com.paytm.urlshortener.model.UrlMapping;
import com.paytm.urlshortener.repository.UrlRepository;
import com.paytm.urlshortener.util.Base62;
import com.paytm.urlshortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Production-ready UrlService implementation.
 *
 * Design decisions:
 * - Reuse existing mapping by originalUrl when no custom alias is requested (idempotency).
 * - Use a dedicated DB sequence (url_mapping_seq) to allocate compact numeric IDs for Base62 encoding.
 *   The code creates the sequence if absent. This avoids relying on IDENTITY or explicit id inserts.
 * - Prevent duplicate aliases by checking existence before insert.
 * - Increment click_count using an atomic DB UPDATE to avoid lost increments under concurrency.
 * - Methods are transactional where they perform writes to ensure consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final JdbcTemplate jdbcTemplate;

    // Allowed custom alias pattern (must match DB CHECK constraint)
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,64}$");

    private static final String SEQ_NAME = "url_mapping_seq";

    /**
     * Shortens the provided original URL.
     * Steps:
     * 1. If customAlias provided: validate, ensure uniqueness, persist and return.
     * 2. Else, if an existing mapping for the original URL exists, return it (reuse).
     * 3. Otherwise, obtain next value from a dedicated sequence, Base62-encode it and
     *    ensure short_code uniqueness (loop if collision), persist new mapping and return.
     *
     * Transactional: writing to DB requires a transaction to persist the new mapping atomically.
     */
    @Override
    @Transactional
    public CreateShortUrlResponse shortenUrl(CreateShortUrlRequest request) {
        String originalUrl = request.originalUrl().trim();
        String customAlias = request.customAlias();

        if (customAlias != null && !customAlias.isBlank()) {
            customAlias = customAlias.trim();
            if (!ALIAS_PATTERN.matcher(customAlias).matches()) {
                throw new IllegalArgumentException("customAlias contains invalid characters or length");
            }
            if (urlRepository.existsByShortCode(customAlias)) {
                throw new DuplicateAliasException("Alias already in use: " + customAlias);
            }
            UrlMapping mapping = UrlMapping.builder()
                    .originalUrl(originalUrl)
                    .shortCode(customAlias)
                    .build();
            UrlMapping saved = urlRepository.save(mapping);
            return toResponse(saved);
        }

        // Reuse existing mapping for idempotency
        Optional<UrlMapping> existing = urlRepository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Ensure sequence exists (safe to call repeatedly)
        jdbcTemplate.execute(String.format("CREATE SEQUENCE IF NOT EXISTS %s START 1", SEQ_NAME));

        String shortCode;
        // Loop until we generate a shortCode that is not currently used (collision avoidance)
        do {
            Long seq = jdbcTemplate.queryForObject(String.format("SELECT nextval('%s')", SEQ_NAME), Long.class);
            if (seq == null) throw new IllegalStateException("Failed to obtain sequence value");
            shortCode = Base62.encode(seq);
        } while (urlRepository.existsByShortCode(shortCode));

        UrlMapping mapping = UrlMapping.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .build();
        UrlMapping saved = urlRepository.save(mapping);
        return toResponse(saved);
    }

    /**
     * Resolve original URL and increment click count.
     * Uses an atomic DB UPDATE to increment click_count to avoid lost updates under concurrent redirects.
     */
    @Override
    @Transactional
    public String redirect(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));

        // Atomic increment in DB avoids read-modify-write lost-update issues
        int updated = jdbcTemplate.update("UPDATE url_mapping SET click_count = click_count + 1 WHERE short_code = ?", shortCode);
        if (updated == 0) {
            log.warn("Failed to increment click_count for shortCode={}", shortCode);
        }

        return mapping.getOriginalUrl();
    }

    /**
     * Simple analytics: returns total clicks and creation time. For more detailed analytics,
     * implementations may query click event tables or aggregates.
     */
    @Override
    public UrlStatsResponse analytics(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));
        // For now return empty dailyClicks map; background jobs or event store can populate time-series.
        return new UrlStatsResponse(mapping.getShortCode(), mapping.getClickCount(), mapping.getCreatedAt(), Map.of());
    }

    private CreateShortUrlResponse toResponse(UrlMapping m) {
        String shortUrl = "/" + m.getShortCode(); // application can replace with external base URL when configured
        return new CreateShortUrlResponse(m.getShortCode(), shortUrl, m.getCreatedAt());
    }
}
