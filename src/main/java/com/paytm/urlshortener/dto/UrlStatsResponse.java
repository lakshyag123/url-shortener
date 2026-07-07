package com.paytm.urlshortener.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO representing aggregated analytics for a short URL.
 *
 * Fields:
 * - shortCode: the token/alias
 * - totalClicks: aggregated total hits
 * - createdAt: creation timestamp
 * - dailyClicks: map of LocalDate -> clicks for simple time-series summaries
 */
public record UrlStatsResponse(
        String shortCode,
        long totalClicks,
        Instant createdAt,
        Map<LocalDate, Long> dailyClicks
) { }
