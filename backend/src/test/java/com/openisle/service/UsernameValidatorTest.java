package com.openisle.service;

import com.openisle.exception.FieldException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsernameValidatorTest {

    @Test
    void rejectsEmptyUsername() {
        UsernameValidator validator = new UsernameValidator();
        assertThrows(FieldException.class, () -> validator.validate(""));
        assertThrows(FieldException.class, () -> validator.validate(null));
    }

    @Test
    void allowsShortUsername() {
        UsernameValidator validator = new UsernameValidator();
        assertDoesNotThrow(() -> validator.validate("a"));
    }
}
