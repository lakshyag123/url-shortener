package com.paytm.urlshortener.dto;

import java.time.Instant;

/**
 * Standard error response returned by the API for exceptions.
 */
public record ErrorResponse(String error, String message, Instant timestamp) {}
