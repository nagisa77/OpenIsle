package com.openisle.service;

import com.openisle.model.PasswordStrength;
import com.openisle.exception.FieldException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordValidator {
    private PasswordStrength strength;

    public PasswordValidator(@Value("${app.password.strength:LOW}") PasswordStrength strength) {
        this.strength = strength;
    }

    public PasswordStrength getStrength() {
        return strength;
    }

    public void setStrength(PasswordStrength strength) {
        this.strength = strength;
    }

    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new FieldException("password", "Password cannot be empty");
        }
        switch (strength) {
            case MEDIUM:
                checkMedium(password);
                break;
            case HIGH:
                checkHigh(password);
                break;
            default:
                checkLow(password);
                break;
        }
    }

    private void checkLow(String password) {
        if (password.length() < 6) {
            throw new FieldException("password", "Password must be at least 6 characters long");
        }
    }

    private void checkMedium(String password) {
        if (password.length() < 8) {
            throw new FieldException("password", "Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new FieldException("password", "Password must contain letters and numbers");
        }
    }

    private void checkHigh(String password) {
        if (password.length() < 12) {
            throw new FieldException("password", "Password must be at least 12 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new FieldException("password", "Password must contain uppercase letters");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new FieldException("password", "Password must contain lowercase letters");
        }
        if (!password.matches(".*\\d.*")) {
            throw new FieldException("password", "Password must contain numbers");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new FieldException("password", "Password must contain special characters");
        }
    }
}
