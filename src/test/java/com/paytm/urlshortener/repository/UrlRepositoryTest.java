package com.paytm.urlshortener.repository;

import com.paytm.urlshortener.model.UrlMapping;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UrlRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    @Test
    void saveAndFindByShortCode() {

        UrlMapping mapping = UrlMapping.builder()
                .originalUrl("https://repo.example")
                .shortCode("repo123")
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        urlRepository.save(mapping);

        Optional<UrlMapping> found =
                urlRepository.findByShortCode("repo123");

        assertThat(found).isPresent();
        assertThat(found.get().getOriginalUrl())
                .isEqualTo("https://repo.example");
    }

    @Test
    void existsByShortCodeWorks() {

        UrlMapping mapping = UrlMapping.builder()
                .originalUrl("https://repo2.example")
                .shortCode("repo456")
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        urlRepository.save(mapping);

        assertThat(urlRepository.existsByShortCode("repo456"))
                .isTrue();
    }
}