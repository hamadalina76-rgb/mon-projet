package com.speedline.auth.service;

import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.domain.UserStatus;
import com.speedline.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtTokenProvider.
 * These tests exercise the real JWT logic (no mocks needed for the provider itself).
 */
class JwtServiceTest {

    private JwtTokenProvider jwtTokenProvider;

    // Must be at least 256 bits (32 bytes) for HMAC-SHA
    private static final String TEST_SECRET = "my-super-secret-key-for-testing-jwt-tokens-must-be-long-enough-256-bits";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour
    private static final long REFRESH_EXPIRATION_MS = 604800000L; // 7 days

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);

        testUser = User.builder()
                .id(42L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ==================== GENERATE TOKEN ====================

    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate a non-empty JWT token")
        void generateToken_returnsNonEmptyString() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Should embed email as subject in token")
        void generateToken_containsEmailAsSubject() {
            String token = jwtTokenProvider.generateToken(testUser);
            String email = jwtTokenProvider.getEmailFromToken(token);
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should embed userId claim in token")
        void generateToken_containsUserId() {
            String token = jwtTokenProvider.generateToken(testUser);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            assertThat(userId).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should embed role claim in token")
        void generateToken_containsRole() {
            String token = jwtTokenProvider.generateToken(testUser);
            String role = jwtTokenProvider.getRoleFromToken(token);
            assertThat(role).isEqualTo("CUSTOMER");
        }

        @Test
        @DisplayName("Should set correct expiration time")
        void generateToken_hasCorrectExpiration() {
            long before = System.currentTimeMillis();
            String token = jwtTokenProvider.generateToken(testUser);
            long after = System.currentTimeMillis();

            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            // Expiration should be roughly (now + EXPIRATION_MS), allow 2s tolerance
            long expectedMin = before + EXPIRATION_MS - 2000;
            long expectedMax = after + EXPIRATION_MS + 2000;
            assertThat(expiration.getTime()).isBetween(expectedMin, expectedMax);
        }

        @Test
        @DisplayName("Should handle user with null firstName gracefully")
        void generateToken_nullFirstName() {
            testUser.setFirstName(null);
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(token).isNotBlank();
            assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should handle user with null lastName gracefully")
        void generateToken_nullLastName() {
            testUser.setLastName(null);
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(token).isNotBlank();
        }
    }

    // ==================== GENERATE REFRESH TOKEN ====================

    @Nested
    @DisplayName("generatRefreshToken()")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Should generate a non-empty refresh token")
        void generatRefreshToken_returnsNonEmptyString() {
            String token = jwtTokenProvider.generatRefreshToken("test@example.com");
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Should embed email as subject in refresh token")
        void generatRefreshToken_containsEmail() {
            String token = jwtTokenProvider.generatRefreshToken("test@example.com");
            String email = jwtTokenProvider.getEmailFromToken(token);
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Refresh token should have longer expiration than access token")
        void generatRefreshToken_hasLongerExpiration() {
            String accessToken = jwtTokenProvider.generateToken(testUser);
            String refreshToken = jwtTokenProvider.generatRefreshToken("test@example.com");

            Date accessExp = jwtTokenProvider.getExpirationDateFromToken(accessToken);
            Date refreshExp = jwtTokenProvider.getExpirationDateFromToken(refreshToken);

            assertThat(refreshExp).isAfter(accessExp);
        }
    }

    // ==================== GENERATE TOKEN FROM EMAIL ====================

    @Nested
    @DisplayName("generateTokenFromEmail()")
    class GenerateTokenFromEmailTests {

        @Test
        @DisplayName("Should generate token with email as subject")
        void generateTokenFromEmail_success() {
            String token = jwtTokenProvider.generateTokenFromEmail("verify@example.com");
            assertThat(token).isNotBlank();

            String email = jwtTokenProvider.getEmailFromToken(token);
            assertThat(email).isEqualTo("verify@example.com");
        }
    }

    // ==================== VALIDATE TOKEN ====================

    @Nested
    @DisplayName("validateToken()")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for a valid token")
        void validateToken_valid_returnsTrue() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Should return false for a malformed token")
        void validateToken_malformed_returnsFalse() {
            assertThat(jwtTokenProvider.validateToken("not.a.valid.jwt")).isFalse();
        }

        @Test
        @DisplayName("Should return false for an empty string")
        void validateToken_empty_returnsFalse() {
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("Should return false for an expired token")
        void validateToken_expired_returnsFalse() {
            // Create a token provider with 0ms expiration
            JwtTokenProvider shortLived = new JwtTokenProvider();
            ReflectionTestUtils.setField(shortLived, "jwtSecret", TEST_SECRET);
            ReflectionTestUtils.setField(shortLived, "jwtExpirationMs", 0L);
            ReflectionTestUtils.setField(shortLived, "refreshTokenExpirationMs", 0L);

            String token = shortLived.generateToken(testUser);
            // Token is already expired (0ms lifetime)
            assertThat(jwtTokenProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("Should return false for token signed with different secret")
        void validateToken_wrongSecret_returnsFalse() {
            // Generate token with a different secret
            SecretKey otherKey = Keys.hmacShaKeyFor(
                    "another-secret-key-for-testing-must-also-be-256-bits-long-enough".getBytes(StandardCharsets.UTF_8));

            String forgedToken = Jwts.builder()
                    .subject("test@example.com")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(otherKey)
                    .compact();

            assertThat(jwtTokenProvider.validateToken(forgedToken)).isFalse();
        }
    }

    // ==================== EXTRACT CLAIMS ====================

    @Nested
    @DisplayName("Claim extraction methods")
    class ClaimExtractionTests {

        @Test
        @DisplayName("getEmailFromToken should return subject")
        void getEmailFromToken_returnsSubject() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("getUserIdFromToken should return userId claim")
        void getUserIdFromToken_returnsUserId() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
        }

        @Test
        @DisplayName("getRoleFromToken should return role claim")
        void getRoleFromToken_returnsRole() {
            testUser.setRole(Role.ADMIN);
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("getExpirationDateFromToken should return future date for valid token")
        void getExpirationDateFromToken_returnsFutureDate() {
            String token = jwtTokenProvider.generateToken(testUser);
            Date expiration = jwtTokenProvider.getExpirationDateFromToken(token);
            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("getUserIdFromToken returns null for refresh token (no userId claim)")
        void getUserIdFromToken_refreshToken_returnsNull() {
            String refreshToken = jwtTokenProvider.generatRefreshToken("test@example.com");
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            assertThat(userId).isNull();
        }
    }

    // ==================== IS TOKEN EXPIRED ====================

    @Nested
    @DisplayName("isTokenExpired()")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("Should return false for a non-expired token")
        void isTokenExpired_validToken_returnsFalse() {
            String token = jwtTokenProvider.generateToken(testUser);
            assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("Should return true for an expired token")
        void isTokenExpired_expiredToken_returnsTrue() {
            JwtTokenProvider shortLived = new JwtTokenProvider();
            ReflectionTestUtils.setField(shortLived, "jwtSecret", TEST_SECRET);
            ReflectionTestUtils.setField(shortLived, "jwtExpirationMs", 0L);
            ReflectionTestUtils.setField(shortLived, "refreshTokenExpirationMs", 0L);

            String token = shortLived.generateToken(testUser);
            // Use the same provider to check (has same secret)
            assertThat(jwtTokenProvider.isTokenExpired(token)).isTrue();
        }

        @Test
        @DisplayName("Should return true for garbage input")
        void isTokenExpired_garbageInput_returnsTrue() {
            assertThat(jwtTokenProvider.isTokenExpired("garbage")).isTrue();
        }
    }
}
