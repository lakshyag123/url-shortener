package com.paytm.urlshortener.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * JPA entity representing a URL mapping (short_code -> original_url).
 */
@Entity
@Table(name = "url_mapping",
       uniqueConstraints = @UniqueConstraint(name = "uk_url_mapping_short_code", columnNames = {"short_code"}))
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key. Mapped to PostgreSQL IDENTITY column (BIGINT).
     * GenerationType.IDENTITY aligns with the Flyway migration that uses GENERATED ALWAYS AS IDENTITY.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Full original/target URL. Use TEXT in DB; validate at application layer to limit length.
     */
    @Column(name = "original_url", nullable = false, columnDefinition = "text")
    @NotBlank(message = "originalUrl must not be blank")
    @Size(max = 2048, message = "originalUrl must be at most 2048 characters")
    private String originalUrl;

    /**
     * Short token or custom alias. Unique and validated by regex.
     */
    @Column(name = "short_code", nullable = false, length = 64, unique = true)
    @NotBlank(message = "shortCode must not be blank")
    @Size(min = 4, max = 64, message = "shortCode length must be between 4 and 64")
    @Pattern(regexp = "^[A-Za-z0-9_-]{4,64}$", message = "shortCode contains invalid characters")
    private String shortCode;

    /**
     * Creation timestamp. Set in @PrePersist to ensure a value when persisted.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Aggregated click counter. Kept as authoritative backup; high-throughput increments should
     * be handled in cache (Redis) and flushed to this column asynchronously.
     */
    @Column(name = "click_count", nullable = false)
    @Min(value = 0, message = "clickCount must be >= 0")
    private Long clickCount;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (clickCount == null) clickCount = 0L;
    }
}
