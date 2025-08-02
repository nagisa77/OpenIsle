package com.openisle.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.reason-secret}")
    private String reasonSecret;

    @Value("${app.jwt.reset-secret}")
    private String resetSecret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private Key getSigningKeyForSecret(String signSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(signSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String generateToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKeyForSecret(secret))
                .compact();
    }

    public String generateReasonToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKeyForSecret(reasonSecret))
                .compact();
    }

    public String generateResetToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKeyForSecret(resetSecret))
                .compact();
    }

    public String validateAndGetSubject(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKeyForSecret(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String validateAndGetSubjectForReason(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKeyForSecret(reasonSecret))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String validateAndGetSubjectForReset(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKeyForSecret(resetSecret))
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
