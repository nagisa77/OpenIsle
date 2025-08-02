package com.openisle.service;

import com.openisle.model.RegisterMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Holds current registration mode. Configurable at runtime.
 */
@Service
public class RegisterModeService {
    private RegisterMode registerMode;

    public RegisterModeService(@Value("${app.register.mode:WHITELIST}") RegisterMode registerMode) {
        this.registerMode = registerMode;
    }

    public RegisterMode getRegisterMode() {
        return registerMode;
    }

    public void setRegisterMode(RegisterMode mode) {
        this.registerMode = mode;
    }
}
