package com.openisle.service;

import com.openisle.exception.FieldException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TagValidator {
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5]+$");

    public void validate(String name) {
        if (name == null || name.isBlank()) {
            throw new FieldException("name", "Tag name cannot be empty");
        }
        if (!ALLOWED.matcher(name).matches()) {
            throw new FieldException("name", "Tag name must be letters or numbers");
        }
    }
}
