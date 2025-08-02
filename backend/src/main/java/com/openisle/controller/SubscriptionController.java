package com.openisle.controller;

import com.openisle.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Endpoints for subscribing to posts, comments and users. */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/posts/{postId}")
    public void subscribePost(@PathVariable Long postId, Authentication auth) {
        subscriptionService.subscribePost(auth.getName(), postId);
    }

    @DeleteMapping("/posts/{postId}")
    public void unsubscribePost(@PathVariable Long postId, Authentication auth) {
        subscriptionService.unsubscribePost(auth.getName(), postId);
    }

    @PostMapping("/comments/{commentId}")
    public void subscribeComment(@PathVariable Long commentId, Authentication auth) {
        subscriptionService.subscribeComment(auth.getName(), commentId);
    }

    @DeleteMapping("/comments/{commentId}")
    public void unsubscribeComment(@PathVariable Long commentId, Authentication auth) {
        subscriptionService.unsubscribeComment(auth.getName(), commentId);
    }

    @PostMapping("/users/{username}")
    public void subscribeUser(@PathVariable String username, Authentication auth) {
        subscriptionService.subscribeUser(auth.getName(), username);
    }

    @DeleteMapping("/users/{username}")
    public void unsubscribeUser(@PathVariable String username, Authentication auth) {
        subscriptionService.unsubscribeUser(auth.getName(), username);
    }
}
