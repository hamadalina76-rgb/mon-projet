package com.speedline.delivery.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT utility dedicated to delivery-service.
 * Lightweight parsing of JWT payload to extract userId / role and
 * check basic expiration, without bringing extra crypto dependencies.
 *
 * NOTE: Signature is supposée déjà validée par les services amont
 * (auth-service / api-gateway). Ici on fait une validation de forme
 * et de date d'expiration pour sécuriser le handshake WebSocket.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean validateToken(String token) {
        try {
            Map<String, Object> claims = extractAllClaims(token);
            Object exp = claims.get("exp");
            if (exp instanceof Number expNumber) {
                long expSeconds = expNumber.longValue();
                boolean valid = Instant.now().isBefore(Instant.ofEpochSecond(expSeconds));
                if (!valid) {
                    log.warn("JWT token expired (exp={})", expSeconds);
                }
                return valid;
            }
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    public String extractUserId(String token) {
        try {
            Map<String, Object> claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                return String.valueOf(userIdObj);
            }
            Object sub = claims.get("sub");
            return sub != null ? String.valueOf(sub) : null;
        } catch (Exception e) {
            log.error("Failed to extract userId from JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            Map<String, Object> claims = extractAllClaims(token);
            String role = toRoleString(claims.get("role"));
            if (role != null && !role.isBlank()) {
                return role;
            }

            String roles = toRoleString(claims.get("roles"));
            if (roles != null && !roles.isBlank()) {
                return roles;
            }

            String authorities = toRoleString(claims.get("authorities"));
            if (authorities != null && !authorities.isBlank()) {
                return authorities;
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to extract role from JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    private String toRoleString(Object claimValue) {
        if (claimValue == null) {
            return null;
        }

        if (claimValue instanceof String s) {
            return s;
        }

        if (claimValue instanceof Collection<?> c) {
            return c.stream()
                    .filter(v -> v != null && !String.valueOf(v).isBlank())
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }

        return String.valueOf(claimValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAllClaims(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        String json = new String(decoded, StandardCharsets.UTF_8);
        return objectMapper.readValue(json, Map.class);
    }
}


