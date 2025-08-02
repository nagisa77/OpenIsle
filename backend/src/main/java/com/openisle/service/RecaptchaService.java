package com.openisle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * CaptchaService implementation using Google reCAPTCHA.
 */
@Service
public class RecaptchaService extends CaptchaService {

    @Value("${recaptcha.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean verify(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String url = "https://www.google.com/recaptcha/api/siteverify?secret={secret}&response={response}";
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, null, Map.class, secretKey, token);
            Map body = resp.getBody();
            return body != null && Boolean.TRUE.equals(body.get("success"));
        } catch (Exception e) {
            return false;
        }
    }
}
