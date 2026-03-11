package com.speedline.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Utility for token validation and claims extraction
 * Uses the same secret key as auth-service for token validation
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates JWT token signature and expiration
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean isValid = !claims.getExpiration().before(new Date());
            
            if (isValid) {
                log.debug("JWT token validated successfully for user: {}", claims.getSubject());
            } else {
                log.warn("JWT token expired for user: {}", claims.getSubject());
            }
            
            return isValid;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extracts user ID from JWT token
     * @param token JWT token string
     * @return user ID as string
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        // Try to get userId from claims first (numeric ID)
        Object userIdObj = claims.get("userId");
        if (userIdObj != null) {
            return String.valueOf(userIdObj);
        }
        // Fallback to subject if userId not present
        return claims.getSubject();
    }

    /**
     * Extracts user role from JWT token
     * @param token JWT token string
     * @return role string (e.g., "ADMIN", "SUPER_ADMIN")
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extracts user email from JWT token
     * @param token JWT token string
     * @return user email
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /**
     * Extracts all claims from JWT token
     * @param token JWT token string
     * @return Claims object
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

