package com.paytm.urlshortener.service;

import com.paytm.urlshortener.dto.CreateShortUrlRequest;
import com.paytm.urlshortener.dto.CreateShortUrlResponse;
import com.paytm.urlshortener.dto.UrlStatsResponse;

/**
 * Service API for URL shortening domain.
 *
 * Implementations should be thread-safe and handle validation, persistence and
 * asynchronous analytics recording. This interface defines the contract only;
 * no implementation is provided here.
 */
public interface UrlService {

    /**
     * Shortens the provided original URL. If a custom alias is provided in the
     * request and already exists, implementations should throw DuplicateAliasException.
     * @param request create request containing originalUrl and optional customAlias
     * @return response with shortCode, shortUrl and creation timestamp
     */
    CreateShortUrlResponse shortenUrl(CreateShortUrlRequest request);

    /**
     * Resolve the original URL for the given short code. Implementations should
     * throw ResourceNotFoundException if the code does not exist or is inactive/expired.
     * This method returns the original long URL to which clients should be redirected.
     * @param shortCode token/alias
     * @return original long URL
     */
    String redirect(String shortCode);

    /**
     * Fetch analytics/statistics for the given short code.
     * Implementations may aggregate data from event store or counters.
     * @param shortCode token/alias
     * @return aggregated statistics for the mapping
     */
    UrlStatsResponse analytics(String shortCode);
}
