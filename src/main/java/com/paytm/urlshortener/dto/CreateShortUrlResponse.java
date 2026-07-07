package com.paytm.urlshortener.dto;

import java.time.Instant;

/**
 * Response DTO returned after creating a short URL.
 */
public record CreateShortUrlResponse(
    String shortCode,
    String shortUrl,
    Instant createdAt
) {
}
