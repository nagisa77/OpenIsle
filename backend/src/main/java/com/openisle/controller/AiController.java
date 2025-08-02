package com.openisle.controller;

import com.openisle.service.OpenAiService;
import com.openisle.service.AiUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final OpenAiService openAiService;
    private final AiUsageService aiUsageService;

    @PostMapping("/format")
    public ResponseEntity<Map<String, String>> format(@RequestBody Map<String, String> req,
                                                     Authentication auth) {
        String text = req.get("text");
        if (text == null) {
            return ResponseEntity.badRequest().build();
        }
        int limit = aiUsageService.getFormatLimit();
        int used = aiUsageService.getCount(auth.getName());
        if (limit > 0 && used >= limit) {
            return ResponseEntity.status(429).build();
        }
        aiUsageService.incrementAndGetCount(auth.getName());
        return openAiService.formatMarkdown(text)
                .map(t -> ResponseEntity.ok(Map.of("content", t)))
                .orElse(ResponseEntity.status(500).build());
    }
}
