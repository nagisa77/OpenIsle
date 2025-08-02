package com.openisle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.openisle.service.AvatarGenerator;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GithubAuthService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AvatarGenerator avatarGenerator;

    @Value("${github.client-id:}")
    private String clientId;

    @Value("${github.client-secret:}")
    private String clientSecret;

    public Optional<User> authenticate(String code, com.openisle.model.RegisterMode mode, String redirectUri) {
        try {
            String tokenUrl = "https://github.com/login/oauth/access_token";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("code", code);
            if (redirectUri != null) {
                body.put("redirect_uri", redirectUri);
            }

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> tokenRes = restTemplate.postForEntity(tokenUrl, request, JsonNode.class);
            if (!tokenRes.getStatusCode().is2xxSuccessful() || tokenRes.getBody() == null || !tokenRes.getBody().has("access_token")) {
                return Optional.empty();
            }
            String accessToken = tokenRes.getBody().get("access_token").asText();
            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setBearerAuth(accessToken);
            authHeaders.set(HttpHeaders.USER_AGENT, "OpenIsle");
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders);
            ResponseEntity<JsonNode> userRes = restTemplate.exchange(
                    "https://api.github.com/user", HttpMethod.GET, entity, JsonNode.class);
            if (!userRes.getStatusCode().is2xxSuccessful() || userRes.getBody() == null) {
                return Optional.empty();
            }
            JsonNode userNode = userRes.getBody();
            String username = userNode.hasNonNull("login") ? userNode.get("login").asText() : null;
            String avatarUrl = userNode.hasNonNull("avatar_url") ? userNode.get("avatar_url").asText() : null;
            String email = null;
            if (userNode.hasNonNull("email")) {
                email = userNode.get("email").asText();
            }
            if (email == null || email.isEmpty()) {
                try {
                    ResponseEntity<JsonNode> emailsRes = restTemplate.exchange(
                            "https://api.github.com/user/emails", HttpMethod.GET, entity, JsonNode.class);
                    if (emailsRes.getStatusCode().is2xxSuccessful() && emailsRes.getBody() != null && emailsRes.getBody().isArray()) {
                        for (JsonNode n : emailsRes.getBody()) {
                            if (n.has("primary") && n.get("primary").asBoolean()) {
                                email = n.get("email").asText();
                                break;
                            }
                        }
                    }
                } catch (HttpClientErrorException ignored) {
                    // ignore when the email API is not accessible
                }
            }
            if (email == null) {
                email = username + "@users.noreply.github.com";
            }
            return Optional.of(processUser(email, username, avatarUrl, mode));
        } catch (Exception e) {
            return Optional.empty();
        }
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
            user.setAvatar(avatarGenerator.generate(finalUsername));
        }
        return userRepository.save(user);
    }
}
