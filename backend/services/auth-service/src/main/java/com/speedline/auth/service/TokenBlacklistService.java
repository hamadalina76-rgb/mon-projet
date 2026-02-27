package com.speedline.auth.service;

import com.speedline.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

/**
 * Service pour gérer les tokens blacklistés et les comptes bloqués.
 * Utilise Redis pour stocker les tokens invalidés et les utilisateurs bloqués.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    private static final String BLOCKED_USER_PREFIX = "blocked:user:";
    private static final Duration DEFAULT_BLOCK_DURATION = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Ajoute un token JWT à la blacklist avec une TTL égale à son expiration.
     */
    public void addToBlacklist(String token) {
        try {
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            long ttlMillis = expiration.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                log.debug("Token déjà expiré, pas besoin de le mettre en blacklist");
                return;
            }

            String key = TOKEN_BLACKLIST_PREFIX + hashToken(token);
            Duration ttl = Duration.ofMillis(ttlMillis);
            redisTemplate.opsForValue().set(key, "1", ttl);
            log.debug("Token ajouté à la blacklist Redis avec TTL={} ms", ttlMillis);
        } catch (Exception e) {
            // Mode dégradé : en cas d'erreur Redis, on ne bloque pas le logout
            log.error("Erreur lors de l'ajout du token à la blacklist Redis", e);
        }
    }

    /**
     * Vérifie si un token est blacklisté.
     * En cas d'erreur Redis, retourne false (fail-open) pour ne pas bloquer tout le système.
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + hashToken(token);
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de la blacklist Redis, on considère le token comme non blacklisté", e);
            return false;
        }
    }

    /**
     * Marque un utilisateur comme bloqué dans Redis.
     * Utilisé lorsqu'un admin bloque un compte.
     */
    public void blockUser(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            String key = BLOCKED_USER_PREFIX + userId;
            redisTemplate.opsForValue().set(key, "1", DEFAULT_BLOCK_DURATION);
            log.debug("Utilisateur {} marqué comme bloqué dans Redis pour {}", userId, DEFAULT_BLOCK_DURATION);
        } catch (Exception e) {
            // Mode dégradé : en cas d'erreur Redis, on ne bloque pas l'opération
            log.error("Erreur lors du marquage de l'utilisateur bloqué dans Redis", e);
        }
    }

    /**
     * Vérifie si un utilisateur est bloqué.
     * En cas d'erreur Redis, retourne false (fail-open).
     */
    public boolean isUserBlocked(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            String key = BLOCKED_USER_PREFIX + userId;
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'utilisateur bloqué dans Redis, on considère l'utilisateur comme non bloqué", e);
            return false;
        }
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
            // Ne devrait jamais arriver, SHA-256 est toujours présent
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
