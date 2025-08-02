package com.openisle.exception;

/**
 * Exception thrown when a user exceeds allowed action rate.
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
