package com.paytm.urlshortener.repository;

import com.paytm.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for UrlMapping entities.
 *
 * Methods return Optional where a mapping may be absent — this encourages callers
 * to explicitly handle the "not found" case and avoids returning null.
 */
@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Find a UrlMapping by its short code token/alias.
     * Returns Optional.empty() if not found.
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Find a UrlMapping by the original long URL.
     * Useful to deduplicate or provide idempotent shortening behaviour.
     */
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);

    /**
     * Check existence of a mapping by short code. Useful for validating custom aliases
     * before attempting to create a new mapping.
     */
    boolean existsByShortCode(String shortCode);
}
