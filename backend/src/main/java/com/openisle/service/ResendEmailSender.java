package com.openisle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ResendEmailSender extends EmailSender {

    @Value("${resend.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Async("notificationExecutor")
    public void sendEmail(String to, String subject, String text) {
        String url = "https://api.resend.com/emails"; // hypothetical endpoint

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, String> body = new HashMap<>();
        body.put("to", to);
        body.put("subject", subject);
        body.put("text", text);
        body.put("from", "openisle <noreply@chenjiating.com>"); // todo(tim): use config

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
