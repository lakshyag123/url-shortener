package com.paytm.urlshortener.repository;

import com.paytm.urlshortener.model.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.enabled=false")
class UrlRepositoryTest {

    @Autowired
    UrlRepository urlRepository;

    @Test
    void saveAndFindByShortCode() {
        UrlMapping m = UrlMapping.builder().originalUrl("https://repo.example").shortCode("r1").createdAt(Instant.now()).clickCount(0L).build();
        UrlMapping saved = urlRepository.save(m);
        Optional<UrlMapping> found = urlRepository.findByShortCode("r1");
        assertThat(found).isPresent();
        assertThat(found.get().getOriginalUrl()).isEqualTo("https://repo.example");
    }

    @Test
    void existsByShortCodeWorks() {
        UrlMapping m = UrlMapping.builder().originalUrl("https://repo2.example").shortCode("r2").createdAt(Instant.now()).clickCount(0L).build();
        urlRepository.save(m);
        assertThat(urlRepository.existsByShortCode("r2")).isTrue();
    }
}

