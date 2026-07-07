package com.paytm.urlshortener.exception;

/**
 * Thrown when a requested resource (e.g., a short code) cannot be found.
 * Results in HTTP 404 Not Found when handled by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
