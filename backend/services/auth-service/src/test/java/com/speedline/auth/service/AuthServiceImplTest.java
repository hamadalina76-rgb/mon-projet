package com.speedline.auth.service;

import com.speedline.auth.client.PartnerServiceClient;
import com.speedline.auth.client.UserServiceClient;
import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.domain.UserStatus;
import com.speedline.auth.dto.request.LoginRequest;
import com.speedline.auth.dto.request.RegisterRequest;
import com.speedline.auth.dto.request.VerifyOtpRequest;
import com.speedline.auth.dto.response.AuthResponse;
import com.speedline.auth.dto.response.OtpResponse;
import com.speedline.auth.exception.AccountStatusException;
import com.speedline.auth.repository.UserRepository;
import com.speedline.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OAuth2Service oauth2Service;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PartnerServiceClient partnerServiceClient;

    @Mock
    private OtpService otpService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeCustomer;
    private User pendingCustomer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(authService, "uploadsBaseUrl", "http://localhost:8080/uploads");

        activeCustomer = User.builder()
                .id(1L)
                .email("john@example.com")
                .password("encoded-password")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+21623456789")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .isPhoneVerified(false)
                .build();

        pendingCustomer = User.builder()
                .id(2L)
                .email("pending@example.com")
                .password("encoded-password")
                .firstName("Pending")
                .lastName("User")
                .phoneNumber("+21699887766")
                .role(Role.CUSTOMER)
                .status(UserStatus.PENDING)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .build();
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new customer successfully")
        void register_success_customer() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("Password123");
            request.setFirstName("Jane");
            request.setLastName("Doe");
            request.setPhoneNumber("+21655443322");
            request.setRole(Role.CUSTOMER);

            when(userRepo.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
            when(tokenProvider.generateTokenFromEmail("new@example.com")).thenReturn("verify-token");
            when(passwordEncoder.encode("Password123")).thenReturn("encoded-pass");
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(10L);
                return u;
            });

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepo).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo("new@example.com");
            assertThat(saved.getPassword()).isEqualTo("encoded-pass");
            assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(saved.getIsEmailVerified()).isFalse();
            assertThat(saved.getVerificationToken()).isEqualTo("verify-token");

            verify(userServiceClient).createCustomer(any());
        }

        @Test
        @DisplayName("Should register a courier and call courier profile creation")
        void register_success_courier() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("courier@example.com");
            request.setPassword("Password123");
            request.setFirstName("Courier");
            request.setLastName("Test");
            request.setPhoneNumber("+21655443322");
            request.setRole(Role.COURIER);

            when(userRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(tokenProvider.generateTokenFromEmail(anyString())).thenReturn("verify-token");
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(11L);
                return u;
            });

            authService.register(request);

            verify(userServiceClient).createCourier(any());
            verify(userServiceClient, never()).createCustomer(any());
        }

        @Test
        @DisplayName("Should register a partner and call partner profile creation")
        void register_success_partner() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("partner@example.com");
            request.setPassword("Password123");
            request.setFirstName("Partner");
            request.setLastName("Test");
            request.setPhoneNumber("+21655443322");
            request.setRole(Role.PARTNER);

            when(userRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(tokenProvider.generateTokenFromEmail(anyString())).thenReturn("verify-token");
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(12L);
                return u;
            });

            authService.register(request);

            verify(partnerServiceClient).createPartner(any());
            verify(userServiceClient, never()).createCustomer(any());
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void register_duplicateEmail_throws() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("john@example.com");
            request.setPassword("Password123");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setPhoneNumber("+21655443322");
            request.setRole(Role.CUSTOMER);

            when(userRepo.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email already exists");

            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("Should default role to CUSTOMER when role is null")
        void register_nullRole_defaultsToCustomer() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("norole@example.com");
            request.setPassword("Password123");
            request.setFirstName("No");
            request.setLastName("Role");
            request.setPhoneNumber("+21655443322");
            request.setRole(null);

            when(userRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(tokenProvider.generateTokenFromEmail(anyString())).thenReturn("token");
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(13L);
                return u;
            });

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepo).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        }

        @Test
        @DisplayName("Should continue registration even if profile creation fails")
        void register_profileCreationFails_continuesSuccessfully() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("fail-profile@example.com");
            request.setPassword("Password123");
            request.setFirstName("Fail");
            request.setLastName("Profile");
            request.setPhoneNumber("+21655443322");
            request.setRole(Role.CUSTOMER);

            when(userRepo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(tokenProvider.generateTokenFromEmail(anyString())).thenReturn("token");
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(14L);
                return u;
            });
            when(userServiceClient.createCustomer(any()))
                    .thenThrow(new RuntimeException("User service unavailable"));

            // Should NOT throw
            authService.register(request);

            verify(userRepo).save(any(User.class));
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Should return AuthResponse for ACTIVE user")
        void login_activeUser_returnsAuthResponse() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(tokenProvider.generateToken(activeCustomer)).thenReturn("access-token");
            when(tokenProvider.generatRefreshToken("john@example.com")).thenReturn("refresh-token");

            Object result = authService.login(request);

            assertThat(result).isInstanceOf(AuthResponse.class);
            AuthResponse authResponse = (AuthResponse) result;
            assertThat(authResponse.getAccessToken()).isEqualTo("access-token");
            assertThat(authResponse.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
            assertThat(authResponse.getUser().getEmail()).isEqualTo("john@example.com");
            assertThat(authResponse.getUser().getRole()).isEqualTo(Role.CUSTOMER);
        }

        @Test
        @DisplayName("Should return OtpResponse for PENDING user")
        void login_pendingUser_returnsOtpResponse() {
            LoginRequest request = new LoginRequest();
            request.setEmail("pending@example.com");
            request.setPassword("password123");

            when(userRepo.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(pendingCustomer));

            Object result = authService.login(request);

            assertThat(result).isInstanceOf(OtpResponse.class);
            OtpResponse otpResponse = (OtpResponse) result;
            assertThat(otpResponse.getEmail()).isEqualTo("pending@example.com");
            assertThat(otpResponse.getOtpSent()).isTrue();
            verify(otpService).generateAndSendOtp("pending@example.com", "Pending");
        }

        @Test
        @DisplayName("Should throw AccountStatusException for SUSPENDED user")
        void login_suspendedUser_throws() {
            User suspended = User.builder()
                    .id(3L).email("sus@example.com").status(UserStatus.SUSPENDED).role(Role.CUSTOMER).build();
            LoginRequest request = new LoginRequest();
            request.setEmail("sus@example.com");
            request.setPassword("password");

            when(userRepo.findByEmailIgnoreCase("sus@example.com")).thenReturn(Optional.of(suspended));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountStatusException.class)
                    .hasMessage("ACCOUNT_SUSPENDED");
        }

        @Test
        @DisplayName("Should throw AccountStatusException for INACTIVE user")
        void login_inactiveUser_throws() {
            User inactive = User.builder()
                    .id(4L).email("inactive@example.com").status(UserStatus.INACTIVE).role(Role.CUSTOMER).build();
            LoginRequest request = new LoginRequest();
            request.setEmail("inactive@example.com");
            request.setPassword("password");

            when(userRepo.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountStatusException.class)
                    .hasMessage("ACCOUNT_INACTIVE");
        }

        @Test
        @DisplayName("Should throw AccountStatusException for DELETED user")
        void login_deletedUser_throws() {
            User deleted = User.builder()
                    .id(5L).email("deleted@example.com").status(UserStatus.DELETED).role(Role.CUSTOMER).build();
            LoginRequest request = new LoginRequest();
            request.setEmail("deleted@example.com");
            request.setPassword("password");

            when(userRepo.findByEmailIgnoreCase("deleted@example.com")).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountStatusException.class)
                    .hasMessage("ACCOUNT_DELETED");
        }

        @Test
        @DisplayName("Should propagate authentication failure (bad credentials)")
        void login_badCredentials_throws() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("wrong-password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw when user not found after authentication")
        void login_userNotFoundAfterAuth_throws() {
            LoginRequest request = new LoginRequest();
            request.setEmail("ghost@example.com");
            request.setPassword("password");

            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    // ==================== VERIFY OTP ====================

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtpTests {

        @Test
        @DisplayName("Should return AuthResponse after valid login OTP for ACTIVE user")
        void verifyOtp_loginFlow_activeUser_returnsAuthResponse() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("john@example.com").otpCode("123456").type("login").build();

            when(otpService.verifyOtp("john@example.com", "123456")).thenReturn(true);
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(tokenProvider.generateToken(activeCustomer)).thenReturn("access-token");
            when(tokenProvider.generatRefreshToken("john@example.com")).thenReturn("refresh-token");

            Object result = authService.verifyOtp(request);

            assertThat(result).isInstanceOf(AuthResponse.class);
            AuthResponse auth = (AuthResponse) result;
            assertThat(auth.getAccessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("Should activate PENDING user after valid OTP")
        void verifyOtp_loginFlow_pendingUser_activatesAccount() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("pending@example.com").otpCode("123456").type("login").build();

            when(otpService.verifyOtp("pending@example.com", "123456")).thenReturn(true);
            when(userRepo.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(pendingCustomer));
            when(userRepo.save(any(User.class))).thenReturn(pendingCustomer);
            when(tokenProvider.generateToken(any(User.class))).thenReturn("access-token");
            when(tokenProvider.generatRefreshToken(anyString())).thenReturn("refresh-token");

            authService.verifyOtp(request);

            assertThat(pendingCustomer.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(pendingCustomer.getIsEmailVerified()).isTrue();
            verify(userRepo).save(pendingCustomer);
        }

        @Test
        @DisplayName("Should throw on invalid OTP code")
        void verifyOtp_invalidCode_throws() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("john@example.com").otpCode("000000").type("login").build();

            when(otpService.verifyOtp("john@example.com", "000000")).thenReturn(false);

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid or expired OTP code");
        }

        @Test
        @DisplayName("Should return map for forgot-password type")
        void verifyOtp_forgotPasswordFlow_returnsMap() {
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("john@example.com").otpCode("123456").type("forgot-password").build();

            when(otpService.verifyOtp("john@example.com", "123456")).thenReturn(true);
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));

            Object result = authService.verifyOtp(request);

            assertThat(result).isInstanceOf(java.util.Map.class);
        }

        @Test
        @DisplayName("Should throw for SUSPENDED user in login OTP flow")
        void verifyOtp_loginFlow_suspendedUser_throws() {
            User suspended = User.builder()
                    .id(3L).email("sus@example.com").status(UserStatus.SUSPENDED).role(Role.CUSTOMER).build();
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("sus@example.com").otpCode("123456").type("login").build();

            when(otpService.verifyOtp("sus@example.com", "123456")).thenReturn(true);
            when(userRepo.findByEmailIgnoreCase("sus@example.com")).thenReturn(Optional.of(suspended));

            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(AccountStatusException.class)
                    .hasMessage("ACCOUNT_SUSPENDED");
        }
    }

    // ==================== REFRESH TOKEN ====================

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should return new access token with valid refresh token")
        void refreshToken_valid_returnsNewAccessToken() {
            when(tokenProvider.validateToken("valid-refresh")).thenReturn(true);
            when(tokenProvider.getEmailFromToken("valid-refresh")).thenReturn("john@example.com");
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(tokenProvider.generateToken(activeCustomer)).thenReturn("new-access-token");

            AuthResponse result = authService.refreshToken("valid-refresh");

            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("valid-refresh");
            assertThat(result.getTokenType()).isEqualTo("Bearer");
            assertThat(result.getUser().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw when refresh token is invalid")
        void refreshToken_invalid_throws() {
            when(tokenProvider.validateToken("bad-refresh")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("bad-refresh"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw when user not found from refresh token")
        void refreshToken_userNotFound_throws() {
            when(tokenProvider.validateToken("orphan-refresh")).thenReturn(true);
            when(tokenProvider.getEmailFromToken("orphan-refresh")).thenReturn("ghost@example.com");
            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("orphan-refresh"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void changePassword_success() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(passwordEncoder.matches("oldPass", "encoded-password")).thenReturn(true);
            when(passwordEncoder.matches("newPass", "encoded-password")).thenReturn(false);
            when(passwordEncoder.encode("newPass")).thenReturn("new-encoded-pass");

            authService.changePassword("john@example.com", "oldPass", "newPass");

            verify(userRepo).save(activeCustomer);
            assertThat(activeCustomer.getPassword()).isEqualTo("new-encoded-pass");
        }

        @Test
        @DisplayName("Should throw when current password is wrong")
        void changePassword_wrongCurrentPassword_throws() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(passwordEncoder.matches("wrongPass", "encoded-password")).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword("john@example.com", "wrongPass", "newPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("mot de passe actuel est incorrect");
        }

        @Test
        @DisplayName("Should throw when new password same as current")
        void changePassword_samePassword_throws() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(passwordEncoder.matches("samePass", "encoded-password")).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword("john@example.com", "samePass", "samePass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("nouveau mot de passe doit");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void changePassword_userNotFound_throws() {
            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword("ghost@example.com", "old", "new"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    // ==================== FORGOT PASSWORD ====================

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should send OTP and return OtpResponse")
        void forgotPassword_success() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));

            OtpResponse result = authService.forgotPassword("john@example.com");

            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getExpirationMinutes()).isEqualTo(3);
            verify(otpService).generateAndSendOtp("john@example.com", "John");
        }

        @Test
        @DisplayName("Should throw when email not found")
        void forgotPassword_unknownEmail_throws() {
            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword("ghost@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    // ==================== RESET PASSWORD ====================

    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password after OTP verification")
        void resetPassword_success() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));
            when(passwordEncoder.encode("NewPassword1")).thenReturn("new-encoded");

            authService.resetPassword("john@example.com", "123456", "NewPassword1");

            verify(otpService).verifyOtp("john@example.com", "123456");
            verify(userRepo).save(activeCustomer);
            assertThat(activeCustomer.getPassword()).isEqualTo("new-encoded");
            assertThat(activeCustomer.getResetPasswordToken()).isNull();
            assertThat(activeCustomer.getResetPasswordExpires()).isNull();
        }

        @Test
        @DisplayName("Should throw when user not found")
        void resetPassword_userNotFound_throws() {
            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("ghost@example.com", "123456", "pass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    // ==================== VERIFY EMAIL ====================

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        @Test
        @DisplayName("Should activate user and clear verification token")
        void verifyEmail_success() {
            User unverified = User.builder()
                    .id(10L).email("unverified@example.com")
                    .verificationToken("valid-token")
                    .status(UserStatus.PENDING)
                    .isEmailVerified(false)
                    .build();
            when(userRepo.findByVerificationToken("valid-token")).thenReturn(Optional.of(unverified));

            authService.verifyEmail("valid-token");

            assertThat(unverified.getIsEmailVerified()).isTrue();
            assertThat(unverified.getVerificationToken()).isNull();
            assertThat(unverified.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepo).save(unverified);
        }

        @Test
        @DisplayName("Should throw for invalid verification token")
        void verifyEmail_invalidToken_throws() {
            when(userRepo.findByVerificationToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid verification token");
        }
    }

    // ==================== LOGOUT ====================

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("Should blacklist valid token on logout")
        void logout_validToken_blacklists() {
            when(tokenProvider.validateToken("valid-jwt")).thenReturn(true);

            authService.logout("Bearer valid-jwt");

            verify(tokenBlacklistService).addToBlacklist("valid-jwt");
        }

        @Test
        @DisplayName("Should not blacklist invalid token")
        void logout_invalidToken_skips() {
            when(tokenProvider.validateToken("invalid-jwt")).thenReturn(false);

            authService.logout("Bearer invalid-jwt");

            verify(tokenBlacklistService, never()).addToBlacklist(anyString());
        }

        @Test
        @DisplayName("Should handle null header gracefully")
        void logout_nullHeader_noop() {
            authService.logout(null);
            verify(tokenBlacklistService, never()).addToBlacklist(anyString());
        }

        @Test
        @DisplayName("Should handle blank header gracefully")
        void logout_blankHeader_noop() {
            authService.logout("  ");
            verify(tokenBlacklistService, never()).addToBlacklist(anyString());
        }
    }

    // ==================== CHECK EMAIL ====================

    @Nested
    @DisplayName("checkEmailExists()")
    class CheckEmailTests {

        @Test
        @DisplayName("Should return true if email exists")
        void checkEmailExists_true() {
            when(userRepo.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);
            assertThat(authService.checkEmailExists("john@example.com")).isTrue();
        }

        @Test
        @DisplayName("Should return false if email does not exist")
        void checkEmailExists_false() {
            when(userRepo.existsByEmailIgnoreCase("nope@example.com")).thenReturn(false);
            assertThat(authService.checkEmailExists("nope@example.com")).isFalse();
        }
    }

    // ==================== GET PROFILE ====================

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("Should return UserInfo for existing user")
        void getProfile_success() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));

            AuthResponse.UserInfo profile = authService.getProfile("john@example.com");

            assertThat(profile.getId()).isEqualTo(1L);
            assertThat(profile.getEmail()).isEqualTo("john@example.com");
            assertThat(profile.getFirstName()).isEqualTo("John");
            assertThat(profile.getLastName()).isEqualTo("Doe");
            assertThat(profile.getRole()).isEqualTo(Role.CUSTOMER);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void getProfile_notFound_throws() {
            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getProfile("ghost@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    // ==================== RESEND OTP ====================

    @Nested
    @DisplayName("resendOtp()")
    class ResendOtpTests {

        @Test
        @DisplayName("Should resend OTP and return OtpResponse")
        void resendOtp_success() {
            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));

            OtpResponse result = authService.resendOtp("john@example.com");

            assertThat(result.getOtpSent()).isTrue();
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            verify(otpService).generateAndSendOtp("john@example.com", "John");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void resendOtp_userNotFound_throws() {
            when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendOtp("ghost@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    // ==================== ADMIN LOGIN ====================

    @Nested
    @DisplayName("adminLogin()")
    class AdminLoginTests {

        @Test
        @DisplayName("Should return AuthResponse for active admin")
        void adminLogin_success() {
            User admin = User.builder()
                    .id(100L).email("admin@example.com").firstName("Admin").lastName("User")
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).build();
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@example.com");
            request.setPassword("adminPass");

            when(userRepo.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
            when(tokenProvider.generateToken(admin)).thenReturn("admin-access");
            when(tokenProvider.generatRefreshToken("admin@example.com")).thenReturn("admin-refresh");
            when(userServiceClient.getAdminByUserId(100L))
                    .thenReturn(new UserServiceClient.AdminProfileResponse("ACTIVE"));

            AuthResponse result = authService.adminLogin(request);

            assertThat(result.getAccessToken()).isEqualTo("admin-access");
            assertThat(result.getUser().getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("Should reject non-admin user")
        void adminLogin_nonAdminRole_throws() {
            LoginRequest request = new LoginRequest();
            request.setEmail("john@example.com");
            request.setPassword("password");

            when(userRepo.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeCustomer));

            assertThatThrownBy(() -> authService.adminLogin(request))
                    .isInstanceOf(AccountStatusException.class);
        }

        @Test
        @DisplayName("Should reject admin with SUSPENDED status in user-service")
        void adminLogin_suspendedInUserService_throws() {
            User admin = User.builder()
                    .id(100L).email("admin@example.com").firstName("Admin").lastName("User")
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).build();
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@example.com");
            request.setPassword("adminPass");

            when(userRepo.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
            when(userServiceClient.getAdminByUserId(100L))
                    .thenReturn(new UserServiceClient.AdminProfileResponse("SUSPENDED"));

            assertThatThrownBy(() -> authService.adminLogin(request))
                    .isInstanceOf(AccountStatusException.class);
        }

        @Test
        @DisplayName("Should proceed when user-service is unreachable (fail-open)")
        void adminLogin_userServiceDown_failsOpen() {
            User admin = User.builder()
                    .id(100L).email("admin@example.com").firstName("Admin").lastName("User")
                    .role(Role.ADMIN).status(UserStatus.ACTIVE).build();
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@example.com");
            request.setPassword("adminPass");

            when(userRepo.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
            when(userServiceClient.getAdminByUserId(100L))
                    .thenThrow(new RuntimeException("Connection refused"));
            when(tokenProvider.generateToken(admin)).thenReturn("admin-access");
            when(tokenProvider.generatRefreshToken("admin@example.com")).thenReturn("admin-refresh");

            AuthResponse result = authService.adminLogin(request);

            assertThat(result.getAccessToken()).isEqualTo("admin-access");
        }
    }
}
