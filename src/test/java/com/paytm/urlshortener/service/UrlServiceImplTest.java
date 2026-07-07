package com.paytm.urlshortener.service;

import com.paytm.urlshortener.dto.CreateShortUrlRequest;
import com.paytm.urlshortener.dto.CreateShortUrlResponse;
import com.paytm.urlshortener.exception.DuplicateAliasException;
import com.paytm.urlshortener.exception.ResourceNotFoundException;
import com.paytm.urlshortener.model.UrlMapping;
import com.paytm.urlshortener.repository.UrlRepository;
import com.paytm.urlshortener.service.impl.UrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    UrlRepository urlRepository;

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    UrlServiceImpl urlService;

    @Captor
    ArgumentCaptor<UrlMapping> mappingCaptor;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shorten_customAlias_success() {
        CreateShortUrlRequest req = new CreateShortUrlRequest("https://example.com/foo", "my-alias");
        when(urlRepository.existsByShortCode("my-alias")).thenReturn(false);
        UrlMapping saved = UrlMapping.builder().id(1L).originalUrl(req.originalUrl()).shortCode("my-alias").createdAt(Instant.now()).clickCount(0L).build();
        when(urlRepository.save(any())).thenReturn(saved);

        CreateShortUrlResponse resp = urlService.shortenUrl(req);

        assertEquals("my-alias", resp.shortCode());
        verify(urlRepository).existsByShortCode("my-alias");
        verify(urlRepository).save(mappingCaptor.capture());
        UrlMapping captured = mappingCaptor.getValue();
        assertEquals(req.originalUrl(), captured.getOriginalUrl());
    }

    @Test
    void shorten_customAlias_duplicate() {
        CreateShortUrlRequest req = new CreateShortUrlRequest("https://example.com/foo", "taken-alias");
        when(urlRepository.existsByShortCode("taken-alias")).thenReturn(true);
        assertThrows(DuplicateAliasException.class, () -> urlService.shortenUrl(req));
        verify(urlRepository).existsByShortCode("taken-alias");
        verify(urlRepository, never()).save(any());
    }

    @Test
    void shorten_reuseExisting() {
        CreateShortUrlRequest req = new CreateShortUrlRequest("https://example.com/dup", null);
        UrlMapping existing = UrlMapping.builder().id(2L).originalUrl(req.originalUrl()).shortCode("abc").createdAt(Instant.now()).clickCount(5L).build();
        when(urlRepository.findByOriginalUrl(req.originalUrl())).thenReturn(Optional.of(existing));

        CreateShortUrlResponse resp = urlService.shortenUrl(req);

        assertEquals("abc", resp.shortCode());
        verify(urlRepository, never()).save(any());
    }

    @Test
    void shorten_generateBase62() {
        CreateShortUrlRequest req = new CreateShortUrlRequest("https://example.com/new", null);
        when(urlRepository.findByOriginalUrl(req.originalUrl())).thenReturn(Optional.empty());
        // simulate sequence returns
        when(jdbcTemplate.queryForObject("SELECT nextval('url_mapping_seq')", Long.class)).thenReturn(123L);
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        UrlMapping saved = UrlMapping.builder().id(10L).originalUrl(req.originalUrl()).shortCode("" + 123L).createdAt(Instant.now()).clickCount(0L).build();
        when(urlRepository.save(any())).thenAnswer(inv -> {
            UrlMapping m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        CreateShortUrlResponse resp = urlService.shortenUrl(req);

        assertNotNull(resp.shortCode());
        verify(jdbcTemplate).execute("CREATE SEQUENCE IF NOT EXISTS url_mapping_seq START 1");
        verify(jdbcTemplate).queryForObject("SELECT nextval('url_mapping_seq')", Long.class);
        verify(urlRepository).save(any());
    }

    @Test
    void redirect_success_increments() {
        UrlMapping m = UrlMapping.builder().id(5L).originalUrl("https://x").shortCode("c1").clickCount(0L).createdAt(Instant.now()).build();
        when(urlRepository.findByShortCode("c1")).thenReturn(Optional.of(m));
        when(jdbcTemplate.update(anyString(), any())).thenReturn(1);

        String target = urlService.redirect("c1");
        assertEquals("https://x", target);
        verify(jdbcTemplate).update("UPDATE url_mapping SET click_count = click_count + 1 WHERE short_code = ?", "c1");
    }

    @Test
    void redirect_notFound() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> urlService.redirect("nope"));
    }
}
