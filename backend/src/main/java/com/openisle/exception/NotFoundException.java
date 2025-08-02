package com.openisle.exception;

/**
 * Exception representing a missing resource such as a post or user.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
