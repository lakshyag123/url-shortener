package com.paytm.urlshortener.exception;

/**
 * Thrown when attempting to create a short code/custom alias that already exists.
 * Results in HTTP 409 Conflict when handled by GlobalExceptionHandler.
 */
public class DuplicateAliasException extends RuntimeException {

    public DuplicateAliasException(String message) {
        super(message);
    }

    public DuplicateAliasException(String message, Throwable cause) {
        super(message, cause);
    }
}
