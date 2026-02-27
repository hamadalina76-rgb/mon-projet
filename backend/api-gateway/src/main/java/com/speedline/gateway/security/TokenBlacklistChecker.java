package com.speedline.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistChecker {

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    private static final String BLOCKED_USER_PREFIX = "blocked:user:";

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public Mono<Boolean> isBlacklisted(String token) {
        return Mono.defer(() -> {
            try {
                String key = TOKEN_BLACKLIST_PREFIX + hashToken(token);
                return reactiveStringRedisTemplate.hasKey(key)
                        .onErrorResume(e -> {
                            log.error("Erreur Redis lors de la vérification de la blacklist, on considère le token comme non blacklisté", e);
                            return Mono.just(false);
                        })
                        .map(Boolean.TRUE::equals);
            } catch (Exception e) {
                log.error("Erreur lors du calcul de la clé de blacklist token", e);
                return Mono.just(false);
            }
        });
    }

    public Mono<Boolean> isUserBlocked(String userId) {
        return Mono.defer(() -> {
            if (userId == null) {
                return Mono.just(false);
            }
            String key = BLOCKED_USER_PREFIX + userId;
            return reactiveStringRedisTemplate.hasKey(key)
                    .onErrorResume(e -> {
                        log.error("Erreur Redis lors de la vérification de l'utilisateur bloqué, on considère l'utilisateur comme non bloqué", e);
                        return Mono.just(false);
                    })
                    .map(Boolean.TRUE::equals);
        });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

