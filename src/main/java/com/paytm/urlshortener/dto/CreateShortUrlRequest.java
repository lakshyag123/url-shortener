package com.paytm.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * Request DTO for creating a short URL.
 * Validation annotations ensure the originalUrl is present, within length limits,
 * and conforms to a URL format (uses Hibernate Validator @URL).
 */
public record CreateShortUrlRequest(
    @NotBlank(message = "originalUrl must not be blank")
    @Size(max = 2048, message = "originalUrl must be at most 2048 characters")
    @URL(message = "originalUrl must be a valid URL")
    String originalUrl,

    /** Optional custom alias; validated at service layer for uniqueness and format. */
    String customAlias
) {
}
