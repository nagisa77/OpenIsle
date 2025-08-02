package com.openisle.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Simple admin demo endpoint.
 */
@RestController
public class AdminController {
    @GetMapping("/api/admin/hello")
    public Map<String, String> adminHello() {
        return Map.of("message", "Hello, Admin User");
    }
}
