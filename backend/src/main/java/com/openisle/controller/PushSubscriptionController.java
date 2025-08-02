package com.openisle.controller;

import com.openisle.service.PushSubscriptionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {
    private final PushSubscriptionService pushSubscriptionService;
    @Value("${app.webpush.public-key}")
    private String publicKey;

    @GetMapping("/public-key")
    public PublicKeyResponse getPublicKey() {
        PublicKeyResponse r = new PublicKeyResponse();
        r.setKey(publicKey);
        return r;
    }

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody SubscriptionRequest req, Authentication auth) {
        pushSubscriptionService.saveSubscription(auth.getName(), req.getEndpoint(), req.getP256dh(), req.getAuth());
    }

    @Data
    private static class PublicKeyResponse {
        private String key;
    }

    @Data
    private static class SubscriptionRequest {
        private String endpoint;
        private String p256dh;
        private String auth;
    }
}
