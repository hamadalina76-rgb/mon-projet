package com.speedline.auth.controller;

import com.speedline.auth.domain.User;
import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.dto.request.*;
import com.speedline.auth.dto.response.AuthResponse;
import com.speedline.auth.dto.response.OtpResponse;
import com.speedline.auth.dto.response.UserInfoResponse;
import com.speedline.auth.repository.UserRepository;
import com.speedline.auth.security.JwtTokenProvider;
import com.speedline.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller pour l'authentification
 *
 * Endpoints:
 * POST /api/v1/auth/register - Inscription
 * POST /api/v1/auth/login - Connexion (envoie OTP)
 * POST /api/v1/auth/verify-login-otp - Vérifier OTP de connexion
 * POST /api/v1/auth/refresh - Refresh token
 * POST /api/v1/auth/logout - Déconnexion
 * POST /api/v1/auth/verify-email - Vérifier email
 * POST /api/v1/auth/forgot-password - Mot de passe oublié (envoie OTP)
 * POST /api/v1/auth/verify-forgot-password-otp - Vérifier OTP de mot de passe oublié
 * POST /api/v1/auth/reset-password - Réinitialiser mot de passe avec OTP
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register endpoint called for email: {}", request.getEmail());
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully. Please verify your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<OtpResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called for email: {}", request.getEmail());
        OtpResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify OTP endpoint called for email: {} with type: {}", request.getEmail(), request.getType());
        Object response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin-only login — rejects non-ADMIN/SUPER_ADMIN users.
     * Used by the Angular admin panel.
     */
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        log.info("Admin login endpoint called for email: {}", request.getEmail());
        AuthResponse response = authService.adminLogin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create admin account endpoint
     * Used by user-service to create admin accounts
     * POST /api/v1/auth/admin/create-account
     */
    @PostMapping("/admin/create-account")
    public ResponseEntity<Map<String, Object>> createAdminAccount(@Valid @RequestBody CreateAdminAccountRequest request) {
        log.info("Create admin account endpoint called for email: {}", request.getEmail());

        // Vérifier que le rôle est ADMIN ou SUPER_ADMIN
        if (!request.getRole().equals("ADMIN") && !request.getRole().equals("SUPER_ADMIN")) {
            throw new RuntimeException("Invalid role: " + request.getRole());
        }

        Long userId = authService.createAdminAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "userId", userId,
                "email", request.getEmail(),
                "role", request.getRole()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token endpoint called");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout endpoint called");
        // Extract username from token if needed
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        log.info("Verify email endpoint called");
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<OtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password endpoint called for email: {}", request.getEmail());
        OtpResponse response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password endpoint called for email: {}", request.getEmail());
        authService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/social-login")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Social login endpoint called for provider: {}", request.getProvider());
        AuthResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Google login endpoint called");
        AuthResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/facebook")
    public ResponseEntity<AuthResponse> facebookLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Facebook login endpoint called");
        AuthResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        log.info("Check email endpoint called for: {}", email);
        boolean exists = authService.checkEmailExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<OtpResponse> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Resend OTP endpoint called for email: {}", email);
        OtpResponse response = authService.resendOtp(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user's information
     * Requires JWT token in Authorization header
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        log.info("GET /auth/me - Getting current user info");

        // Extract JWT token from Bearer header
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        log.info("Extracted userId {} from token", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", userId);
                    return new RuntimeException("User not found with id: " + userId);
                });

        log.info("User found: {} {} ({})", user.getFirstName(), user.getLastName(), user.getEmail());

        UserInfoResponse response = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole().name())
                .build();

        log.info("Returning current user info: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }
}
