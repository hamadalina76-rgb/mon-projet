package com.speedline.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.domain.UserStatus;
import com.speedline.auth.dto.request.*;
import com.speedline.auth.dto.response.AuthResponse;
import com.speedline.auth.dto.response.OtpResponse;
import com.speedline.auth.exception.AccountStatusException;
import com.speedline.auth.repository.UserRepository;
import com.speedline.auth.security.JwtTokenProvider;
import com.speedline.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new com.speedline.auth.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should return 201 Created on successful registration")
        void register_success_returns201() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("Password123");
            request.setFirstName("Jane");
            request.setLastName("Doe");
            request.setPhoneNumber("+21623456789");
            request.setRole(Role.CUSTOMER);

            doNothing().when(authService).register(any(RegisterRequest.class));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("User registered successfully. Please verify your email."));
        }

        @Test
        @DisplayName("Should return 500 when email already exists")
        void register_duplicateEmail_returns500() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("Password123");
            request.setFirstName("Jane");
            request.setLastName("Doe");
            request.setPhoneNumber("+21623456789");
            request.setRole(Role.CUSTOMER);

            doThrow(new RuntimeException("Email already exists"))
                    .when(authService).register(any(RegisterRequest.class));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 with AuthResponse for active user")
        void login_activeUser_returns200() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            AuthResponse response = AuthResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(86400000L)
                    .user(AuthResponse.UserInfo.builder()
                            .id(1L).email("john@example.com")
                            .firstName("John").lastName("Doe")
                            .role(Role.CUSTOMER).build())
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("access-token"))
                    .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
                    .andExpect(jsonPath("$.token_type").value("Bearer"));
        }

        @Test
        @DisplayName("Should return 200 with OtpResponse for pending user")
        void login_pendingUser_returnsOtpResponse() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("pending@example.com");
            request.setPassword("password123");

            OtpResponse otpResponse = OtpResponse.builder()
                    .message("Please verify your email with the OTP code sent to complete your first login.")
                    .email("pending@example.com")
                    .otpSent(true)
                    .expirationMinutes(15)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(otpResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("pending@example.com"))
                    .andExpect(jsonPath("$.otpSent").value(true));
        }
    }

    // ==================== VERIFY OTP ====================

    @Nested
    @DisplayName("POST /api/v1/auth/verify-otp")
    class VerifyOtpEndpointTests {

        @Test
        @DisplayName("Should return 200 on successful OTP verification")
        void verifyOtp_success_returns200() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("john@example.com").otpCode("123456").type("login").build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("access-token").refreshToken("refresh-token")
                    .tokenType("Bearer").expiresIn(86400000L)
                    .user(AuthResponse.UserInfo.builder()
                            .id(1L).email("john@example.com").role(Role.CUSTOMER).build())
                    .build();

            when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("access-token"));
        }

        @Test
        @DisplayName("Should return 500 for invalid OTP")
        void verifyOtp_invalid_returns500() throws Exception {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("john@example.com").otpCode("000000").type("login").build();

            when(authService.verifyOtp(any(VerifyOtpRequest.class)))
                    .thenThrow(new RuntimeException("Invalid or expired OTP code"));

            mockMvc.perform(post("/api/v1/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== REFRESH TOKEN ====================

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenEndpointTests {

        @Test
        @DisplayName("Should return 200 with new tokens")
        void refreshToken_success_returns200() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            AuthResponse response = AuthResponse.builder()
                    .accessToken("new-access-token").refreshToken("valid-refresh-token")
                    .tokenType("Bearer").expiresIn(86400000L)
                    .user(AuthResponse.UserInfo.builder()
                            .id(1L).email("john@example.com").role(Role.CUSTOMER).build())
                    .build();

            when(authService.refreshToken("valid-refresh-token")).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("new-access-token"));
        }

        @Test
        @DisplayName("Should return 500 for invalid refresh token")
        void refreshToken_invalid_returns500() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("bad-refresh-token");

            when(authService.refreshToken("bad-refresh-token"))
                    .thenThrow(new RuntimeException("Invalid refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== LOGOUT ====================

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutEndpointTests {

        @Test
        @DisplayName("Should return 200 with valid token")
        void logout_validToken_returns200() throws Exception {
            doNothing().when(authService).logout(anyString());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("Should return 200 even without Authorization header")
        void logout_noHeader_returns200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("Should return 200 even with empty Bearer token")
        void logout_emptyBearer_returns200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }

        @Test
        @DisplayName("Should return 200 even when logout throws (idempotent)")
        void logout_serviceThrows_returns200() throws Exception {
            doThrow(new RuntimeException("Redis down")).when(authService).logout(anyString());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    // ==================== VERIFY EMAIL ====================

    @Nested
    @DisplayName("POST /api/v1/auth/verify-email")
    class VerifyEmailEndpointTests {

        @Test
        @DisplayName("Should return 200 on successful email verification")
        void verifyEmail_success_returns200() throws Exception {
            doNothing().when(authService).verifyEmail("valid-token");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email verified successfully"));
        }

        @Test
        @DisplayName("Should return 500 for invalid verification token")
        void verifyEmail_invalidToken_returns500() throws Exception {
            doThrow(new RuntimeException("Invalid verification token"))
                    .when(authService).verifyEmail("bad-token");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .param("token", "bad-token"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== FORGOT PASSWORD ====================

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordEndpointTests {

        @Test
        @DisplayName("Should return 200 with OtpResponse")
        void forgotPassword_success_returns200() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("john@example.com");

            OtpResponse response = OtpResponse.builder()
                    .message("OTP sent to your email")
                    .email("john@example.com")
                    .expirationMinutes(3)
                    .build();

            when(authService.forgotPassword("john@example.com")).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.message").value("OTP sent to your email"));
        }
    }

    // ==================== RESET PASSWORD ====================

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordEndpointTests {

        @Test
        @DisplayName("Should return 200 on successful password reset")
        void resetPassword_success_returns200() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setEmail("john@example.com");
            request.setOtpCode("123456");
            request.setNewPassword("NewPassword1");

            doNothing().when(authService).resetPassword("john@example.com", "123456", "NewPassword1");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully"));
        }
    }

    // ==================== CHECK EMAIL ====================

    @Nested
    @DisplayName("GET /api/v1/auth/check-email")
    class CheckEmailEndpointTests {

        @Test
        @DisplayName("Should return exists=true for existing email")
        void checkEmail_exists_returnsTrue() throws Exception {
            when(authService.checkEmailExists("john@example.com")).thenReturn(true);

            mockMvc.perform(get("/api/v1/auth/check-email")
                            .param("email", "john@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists").value(true));
        }

        @Test
        @DisplayName("Should return exists=false for non-existing email")
        void checkEmail_notExists_returnsFalse() throws Exception {
            when(authService.checkEmailExists("nope@example.com")).thenReturn(false);

            mockMvc.perform(get("/api/v1/auth/check-email")
                            .param("email", "nope@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exists").value(false));
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @Nested
    @DisplayName("POST /api/v1/auth/change-password")
    class ChangePasswordEndpointTests {

        @Test
        @DisplayName("Should return 200 with X-User-Email header")
        void changePassword_withEmailHeader_returns200() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("oldPassword1");
            request.setNewPassword("newPassword1");

            doNothing().when(authService).changePassword("john@example.com", "oldPassword1", "newPassword1");

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer some-token")
                            .header("X-User-Email", "john@example.com")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("Should extract email from JWT when X-User-Email is missing")
        void changePassword_withoutEmailHeader_extractsFromJwt() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("oldPassword1");
            request.setNewPassword("newPassword1");

            when(jwtTokenProvider.validateToken("valid-jwt")).thenReturn(true);
            when(jwtTokenProvider.getEmailFromToken("valid-jwt")).thenReturn("john@example.com");
            doNothing().when(authService).changePassword("john@example.com", "oldPassword1", "newPassword1");

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer valid-jwt")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("Should return 401 when token is invalid and no email header")
        void changePassword_invalidToken_returns401() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setCurrentPassword("oldPassword1");
            request.setNewPassword("newPassword1");

            when(jwtTokenProvider.validateToken("expired-jwt")).thenReturn(false);

            mockMvc.perform(post("/api/v1/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer expired-jwt")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }
    }

    // ==================== GET PROFILE ====================

    @Nested
    @DisplayName("GET /api/v1/auth/profile")
    class GetProfileEndpointTests {

        @Test
        @DisplayName("Should return profile with valid JWT")
        void getProfile_validJwt_returns200() throws Exception {
            when(jwtTokenProvider.validateToken("valid-jwt")).thenReturn(true);
            when(jwtTokenProvider.getEmailFromToken("valid-jwt")).thenReturn("john@example.com");

            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .id(1L).email("john@example.com").firstName("John").lastName("Doe")
                    .role(Role.CUSTOMER).build();
            when(authService.getProfile("john@example.com")).thenReturn(userInfo);

            mockMvc.perform(get("/api/v1/auth/profile")
                            .header("Authorization", "Bearer valid-jwt"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        @DisplayName("Should return 401 with invalid JWT and no email header")
        void getProfile_invalidJwt_returns401() throws Exception {
            when(jwtTokenProvider.validateToken("bad-jwt")).thenReturn(false);

            mockMvc.perform(get("/api/v1/auth/profile")
                            .header("Authorization", "Bearer bad-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should use X-User-Email header when present")
        void getProfile_withEmailHeader_usesIt() throws Exception {
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .id(1L).email("john@example.com").firstName("John").lastName("Doe")
                    .role(Role.CUSTOMER).build();
            when(authService.getProfile("john@example.com")).thenReturn(userInfo);

            mockMvc.perform(get("/api/v1/auth/profile")
                            .header("Authorization", "Bearer whatever")
                            .header("X-User-Email", "john@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }
    }

    // ==================== CURRENT USER ====================

    @Nested
    @DisplayName("GET /api/v1/auth/current_user")
    class CurrentUserEndpointTests {

        @Test
        @DisplayName("Should return current user info from JWT")
        void getCurrentUser_validJwt_returns200() throws Exception {
            User user = User.builder()
                    .id(1L).email("john@example.com").firstName("John").lastName("Doe")
                    .phoneNumber("+21623456789").role(Role.CUSTOMER).status(UserStatus.ACTIVE)
                    .build();

            when(jwtTokenProvider.getUserIdFromToken("valid-jwt")).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            mockMvc.perform(get("/api/v1/auth/current_user")
                            .header("Authorization", "Bearer valid-jwt"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("Should return 500 when user not found by id from token")
        void getCurrentUser_userNotFound_returns500() throws Exception {
            when(jwtTokenProvider.getUserIdFromToken("valid-jwt")).thenReturn(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/auth/current_user")
                            .header("Authorization", "Bearer valid-jwt"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== HEALTH ====================

    @Nested
    @DisplayName("GET /api/v1/auth/health")
    class HealthEndpointTests {

        @Test
        @DisplayName("Should return UP status")
        void health_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/auth/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("auth-service"));
        }
    }

    // ==================== ADMIN LOGIN ====================

    @Nested
    @DisplayName("POST /api/v1/auth/admin/login")
    class AdminLoginEndpointTests {

        @Test
        @DisplayName("Should return 200 with AuthResponse for admin")
        void adminLogin_success_returns200() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@example.com");
            request.setPassword("adminPass");

            AuthResponse response = AuthResponse.builder()
                    .accessToken("admin-access").refreshToken("admin-refresh")
                    .tokenType("Bearer").expiresIn(86400000L)
                    .user(AuthResponse.UserInfo.builder()
                            .id(100L).email("admin@example.com").role(Role.ADMIN).build())
                    .build();

            when(authService.adminLogin(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/admin/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("admin-access"))
                    .andExpect(jsonPath("$.user.role").value("ADMIN"));
        }
    }

    // ==================== RESEND OTP ====================

    @Nested
    @DisplayName("POST /api/v1/auth/resend-otp")
    class ResendOtpEndpointTests {

        @Test
        @DisplayName("Should return 200 with OtpResponse")
        void resendOtp_success_returns200() throws Exception {
            OtpResponse response = OtpResponse.builder()
                    .message("New OTP sent to your email.")
                    .email("john@example.com")
                    .otpSent(true)
                    .expirationMinutes(3)
                    .build();

            when(authService.resendOtp("john@example.com")).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/resend-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"john@example.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.otpSent").value(true))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }
    }

    // ==================== UPDATE PROFILE ====================

    @Nested
    @DisplayName("PUT /api/v1/auth/profile")
    class UpdateProfileEndpointTests {

        @Test
        @DisplayName("Should return 200 with updated profile fields")
        void updateProfile_success_returns200() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Jane");
            request.setLastName("Updated");
            request.setPhoneNumber("+21699887766");

            doNothing().when(authService).updateProfile("john@example.com", "Jane", "Updated", "+21699887766");

            AuthResponse.UserInfo profile = AuthResponse.UserInfo.builder()
                    .id(1L).email("john@example.com").firstName("Jane").lastName("Updated")
                    .phoneNumber("+21699887766").role(Role.CUSTOMER).build();
            when(authService.getProfile("john@example.com")).thenReturn(profile);

            mockMvc.perform(put("/api/v1/auth/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer some-token")
                            .header("X-User-Email", "john@example.com")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Updated"));
        }

        @Test
        @DisplayName("Should return 401 when JWT invalid and no email header")
        void updateProfile_invalidJwt_returns401() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Jane");
            request.setLastName("Updated");

            when(jwtTokenProvider.validateToken("bad-jwt")).thenReturn(false);

            mockMvc.perform(put("/api/v1/auth/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer bad-jwt")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
