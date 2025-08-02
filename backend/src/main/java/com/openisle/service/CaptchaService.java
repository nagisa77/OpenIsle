package com.openisle.service;

/**
 * Abstract service for verifying CAPTCHA tokens.
 */
public abstract class CaptchaService {
    /**
     * Verify the CAPTCHA token sent from client.
     *
     * @param token CAPTCHA token
     * @return true if token is valid
     */
    public abstract boolean verify(String token);
}
