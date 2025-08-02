package com.openisle.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAiService {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<String> formatMarkdown(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "请优化以下 Markdown 文本的格式，不改变其内容。"));
        messages.add(Map.of("role", "user", "content", text));
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map respBody = resp.getBody();
            if (respBody != null) {
                Object choicesObj = respBody.get("choices");
                if (choicesObj instanceof List choices && !choices.isEmpty()) {
                    Object first = choices.get(0);
                    if (first instanceof Map firstMap) {
                        Object messageObj = firstMap.get("message");
                        if (messageObj instanceof Map message) {
                            Object content = message.get("content");
                            if (content instanceof String str) {
                                return Optional.of(str.trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
