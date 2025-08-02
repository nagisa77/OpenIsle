package com.openisle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openisle.model.RegisterMode;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TwitterAuthService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(TwitterAuthService.class);

    @Value("${twitter.client-id:}")
    private String clientId;

    @Value("${twitter.client-secret:}")
    private String clientSecret;

    public Optional<User> authenticate(
            String code,
            String codeVerifier,
            RegisterMode mode,
            String redirectUri) {

        logger.debug("Starting authentication with code {} and verifier {}", code, codeVerifier);

        // 1. 交换 token
        String tokenUrl = "https://api.twitter.com/2/oauth2/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (!clientId.isEmpty() && !clientSecret.isEmpty()) {
            String credentials = clientId + ":" + clientSecret;
            String authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        // Twitter PKCE 要求的五个参数
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("code_verifier", codeVerifier);
        body.add("redirect_uri", redirectUri);      // 一律必填
        // 如果你的 app 属于机密客户端，必须带 client_secret
        body.add("client_secret", clientSecret);

        ResponseEntity<JsonNode> tokenRes;
        try {
            logger.debug("Requesting token from {}", tokenUrl);
            tokenRes = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(body, headers), JsonNode.class);
            logger.debug("Token response: {}", tokenRes.getBody());
        } catch (HttpClientErrorException e) {
            logger.warn("Token request failed with status {} and body {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        }

        JsonNode tokenJson = tokenRes.getBody();
        if (tokenJson == null || !tokenJson.hasNonNull("access_token")) {
            return Optional.empty();
        }
        String accessToken = tokenJson.get("access_token").asText();

        // 2. 拉取用户信息
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<JsonNode> userRes;
        try {
            logger.debug("Fetching user info with access token");
            userRes = restTemplate.exchange(
                    "https://api.twitter.com/2/users/me?user.fields=profile_image_url",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders),
                    JsonNode.class);
            logger.debug("User info response: {}", userRes.getBody());
        } catch (HttpClientErrorException e) {
            logger.debug("User info request failed", e);
            return Optional.empty();
        }

        JsonNode data = userRes.getBody() == null ? null : userRes.getBody().path("data");
        String username = data != null ? data.path("username").asText(null) : null;
        String avatar = data != null ? data.path("profile_image_url").asText(null) : null;
        if (username == null) {
            return Optional.empty();
        }

        // Twitter v2 默认拿不到 email；如果你申请到 email.scope，可改用 /2/users/:id?user.fields=email
        String email = username + "@twitter.com";
        logger.debug("Processing user {} with email {}", username, email);
        return Optional.of(processUser(email, username, avatar, mode));
    }

    private User processUser(String email, String username, String avatar, com.openisle.model.RegisterMode mode) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isVerified()) {
                user.setVerified(true);
                user.setVerificationCode(null);
                userRepository.save(user);
            }
            logger.debug("Existing user {} authenticated", user.getUsername());
            return user;
        }
        String baseUsername = username != null ? username : email.split("@")[0];
        String finalUsername = baseUsername;
        int suffix = 1;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = baseUsername + suffix++;
        }
        User user = new User();
        user.setUsername(finalUsername);
        user.setEmail(email);
        user.setPassword("");
        user.setRole(Role.USER);
        user.setVerified(true);
        user.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
        if (avatar != null) {
            user.setAvatar(avatar);
        } else {
            user.setAvatar("https://twitter.com/" + finalUsername + "/profile_image");
        }
        logger.debug("Creating new user {}", finalUsername);
        return userRepository.save(user);
    }
}
