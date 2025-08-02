package com.openisle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscordAuthService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${discord.client-id:}")
    private String clientId;

    @Value("${discord.client-secret:}")
    private String clientSecret;

    public Optional<User> authenticate(String code, com.openisle.model.RegisterMode mode, String redirectUri) {
        try {
            String tokenUrl = "https://discord.com/api/oauth2/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            if (redirectUri != null) {
                body.add("redirect_uri", redirectUri);
            }
            body.add("scope", "identify email");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> tokenRes = restTemplate.postForEntity(tokenUrl, request, JsonNode.class);
            if (!tokenRes.getStatusCode().is2xxSuccessful() || tokenRes.getBody() == null || !tokenRes.getBody().has("access_token")) {
                return Optional.empty();
            }
            String accessToken = tokenRes.getBody().get("access_token").asText();
            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders);
            ResponseEntity<JsonNode> userRes = restTemplate.exchange(
                    "https://discord.com/api/users/@me", HttpMethod.GET, entity, JsonNode.class);
            if (!userRes.getStatusCode().is2xxSuccessful() || userRes.getBody() == null) {
                return Optional.empty();
            }
            JsonNode userNode = userRes.getBody();
            String email = userNode.hasNonNull("email") ? userNode.get("email").asText() : null;
            String username = userNode.hasNonNull("username") ? userNode.get("username").asText() : null;
            String id = userNode.hasNonNull("id") ? userNode.get("id").asText() : null;
            String avatar = null;
            if (userNode.hasNonNull("avatar") && id != null) {
                avatar = "https://cdn.discordapp.com/avatars/" + id + "/" + userNode.get("avatar").asText() + ".png";
            }
            if (email == null) {
                email = (username != null ? username : id) + "@users.noreply.discord.com";
            }
            return Optional.of(processUser(email, username, avatar, mode));
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
            user.setAvatar("https://cdn.discordapp.com/embed/avatars/0.png");
        }
        return userRepository.save(user);
    }
}
