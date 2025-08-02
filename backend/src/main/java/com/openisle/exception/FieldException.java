package com.openisle.exception;

import lombok.Getter;

/**
 * Exception carrying a target field name. Useful for reporting
 * validation errors to clients so they can display feedback near
 * the appropriate input element.
 */
@Getter
public class FieldException extends RuntimeException {
    private final String field;

    public FieldException(String field, String message) {
        super(message);
        this.field = field;
    }
}

