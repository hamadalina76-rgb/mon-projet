package com.speedline.auth.service;

import com.speedline.auth.client.UserServiceClient;
import com.speedline.auth.domain.AuthProvider;
import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.domain.UserStatus;
import com.speedline.auth.dto.request.CreateCourierRequest;
import com.speedline.auth.dto.request.CreateCustomerRequest;
import com.speedline.auth.dto.request.LoginRequest;
import com.speedline.auth.dto.request.RegisterRequest;
import com.speedline.auth.dto.request.SocialLoginRequest;
import com.speedline.auth.dto.request.VerifyOtpRequest;
import com.speedline.auth.dto.response.AuthResponse;
import com.speedline.auth.dto.response.OtpResponse;
import com.speedline.auth.repository.UserRepository;
import com.speedline.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service principal d'authentification
 * - register()
 * - login()
 * - logout()
 * - refreshToken()
 * - forgotPassword()
 * - resetPassword()
 * - verifyEmail()
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OAuth2Service oauth2Service;
    private final UserServiceClient userServiceClient;
    private final OtpService otpService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        // Check if email already exists (ignore case)
        if (userRepo.existsByEmailIgnoreCase(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Generate verification token
        String verificationToken = tokenProvider.generateTokenFromEmail(request.getEmail());

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole() != null ? request.getRole() : Role.CUSTOMER)
                .status(UserStatus.PENDING)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .verificationToken(verificationToken)
                .build();

        user = userRepo.save(user);
        
        log.info("User registered successfully. Verification token: {}", verificationToken);
        log.info("User registered successfully with email: {}", request.getEmail());

        // Create profile after transaction commits
        Long userId = user.getId();
        Role userRole = user.getRole();
        String email = user.getEmail();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String phoneNumber = user.getPhoneNumber();
        
        // Call this after @Transactional method completes
        createUserProfile(userId, userRole, email, firstName, lastName, phoneNumber);
    }

    /**
     * Create user profile in user-service (non-transactional)
     * Separated to avoid issues with @Transactional and Feign calls
     */
    private void createUserProfile(Long userId, Role role, String email, String firstName, String lastName, String phoneNumber) {
        // Create corresponding profile in user-service based on role
        if (role == Role.CUSTOMER) {
            log.info("Attempting to create customer profile for userId: {}", userId);
            try {
                CreateCustomerRequest customerRequest = CreateCustomerRequest.builder()
                        .userId(userId)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .phoneNumber(phoneNumber)
                        .build();
                userServiceClient.createCustomer(customerRequest);
                log.info("Customer profile created successfully for userId: {}", userId);
            } catch (Exception e) {
                log.error("FAILED to create customer profile in user-service for userId: {}. Error: {}", 
                        userId, e.getMessage(), e);
                log.error("Full exception details:", e);
                // Continue - the user account is created, profile can be created later
            }
        } else if (role == Role.COURIER) {
            log.info("Attempting to create courier profile for userId: {}", userId);
            try {
                CreateCourierRequest courierRequest = CreateCourierRequest.builder()
                        .userId(userId)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .phoneNumber(phoneNumber)
                        .build();
                userServiceClient.createCourier(courierRequest);
                log.info("Courier profile created successfully for userId: {}", userId);
            } catch (Exception e) {
                log.error("FAILED to create courier profile in user-service for userId: {}. Error: {}", 
                        userId, e.getMessage(), e);
                log.error("Full exception details:", e);
                // Continue - the user account is created, profile can be created later
            }
        }
    }

    @Override
    @Transactional
    public OtpResponse login(LoginRequest request) {
        log.info("User login attempt with email: {}", request.getEmail());
        
        // Authenticate user (verify email and password)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            log.warn("Authentication failed for email: {}", request.getEmail());
            throw e;
        }

        // Get user from database
        User user = userRepo.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate and send OTP
        otpService.generateAndSendOtp(user.getEmail(), user.getFirstName());

        log.info("OTP sent successfully for login: {}", request.getEmail());

        return OtpResponse.builder()
                .message("OTP sent to your email. Please verify to complete login.")
                .email(user.getEmail())
                .otpSent(true)
                .expirationMinutes(15)
                .build();
    }

    @Override
    @Transactional
    public Object verifyOtp(VerifyOtpRequest request) {
        log.info("Verifying OTP for email: {} with type: {}", request.getEmail(), request.getType());
        
        // Verify OTP code
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtpCode());
        
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP code");
        }

        // Determine flow based on type
        String type = request.getType() != null ? request.getType() : "login";
        
        if ("forgot-password".equals(type)) {
            // Forgot password flow - just verify user exists
            User user = userRepo.findByEmailIgnoreCase(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            log.info("OTP verified successfully for forgot password flow: {}", request.getEmail());
            return Map.of("message", "OTP verified successfully");
        } else {
            // Login flow - generate tokens
            User user = userRepo.findByEmailIgnoreCase(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Generate tokens
            String accessToken = tokenProvider.generateToken(user);
            String refreshToken = tokenProvider.generatRefreshToken(user.getEmail());

            log.info("User logged in successfully after OTP verification: {}", request.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpirationMs)
                    .user(AuthResponse.UserInfo.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .role(user.getRole())
                            .profilePicture(user.getProfilePicture())
                            .build())
                    .build();
        }
    }

    @Override
    @Transactional
    public AuthResponse socialLogin(SocialLoginRequest request) {
        log.info("Social login attempt with provider: {}", request.getProvider());
        
        // Verify token and get user info from OAuth provider
        OAuth2Service.OAuth2UserInfo oauth2UserInfo = oauth2Service.verifyAndGetUserInfo(
                request.getAccessToken(), 
                request.getProvider()
        );
        
        boolean isNewUser = false;
        
        // Check if user already exists by provider and provider user ID
        Optional<User> existingUser = userRepo.findByAuthProviderAndProviderUserId(
                request.getProvider(), 
                oauth2UserInfo.getProviderId()
        );
        
        User user;
        
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update profile picture if changed
            if (oauth2UserInfo.getProfilePicture() != null && 
                !oauth2UserInfo.getProfilePicture().equals(user.getProfilePicture())) {
                user.setProfilePicture(oauth2UserInfo.getProfilePicture());
                userRepo.save(user);
            }
        } else {
            // Check if user exists with same email (different provider)
            Optional<User> existingByEmail = userRepo.findByEmailIgnoreCase(oauth2UserInfo.getEmail());
            
            if (existingByEmail.isPresent()) {
                // User exists with different provider - update to link OAuth account
                user = existingByEmail.get();
                user.setAuthProvider(request.getProvider());
                user.setProviderUserId(oauth2UserInfo.getProviderId());
                user.setProfilePicture(oauth2UserInfo.getProfilePicture());
                user.setIsEmailVerified(oauth2UserInfo.getEmailVerified());
                log.info("Linking existing user to {} account", request.getProvider());
            } else {
                // Create new user
                user = User.builder()
                        .email(oauth2UserInfo.getEmail())
                        .firstName(oauth2UserInfo.getFirstName())
                        .lastName(oauth2UserInfo.getLastName())
                        .profilePicture(oauth2UserInfo.getProfilePicture())
                        .authProvider(request.getProvider())
                        .providerUserId(oauth2UserInfo.getProviderId())
                        .role(request.getRole())
                        .status(UserStatus.ACTIVE)
                        .isEmailVerified(oauth2UserInfo.getEmailVerified())
                        .isPhoneVerified(false)
                        .password(null) // No password for OAuth users
                        .build();
                isNewUser = true;
                log.info("Creating new user from {} login", request.getProvider());
            }
            
            userRepo.save(user);
        }
        
        // Generate JWT tokens
        String accessToken = tokenProvider.generateToken(user);
        String refreshToken = tokenProvider.generatRefreshToken(user.getEmail());
        
        log.info("Social login successful for user: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs)
                .isNewUser(isNewUser)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .profilePicture(user.getProfilePicture())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public void logout(String username) {
        log.info("User logout: {}", username);
        // TODO: Implement token blacklist logic if needed
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        
        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Extract email from token
        String email = tokenProvider.getEmailFromToken(refreshToken);

        // Get user
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate new access token
        String newAccessToken = tokenProvider.generateToken(user);

        log.info("Token refreshed successfully for user: {}", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs) // 24 hours in milliseconds
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .profilePicture(user.getProfilePicture())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public OtpResponse forgotPassword(String email) {
        log.info("Forgot password request for email: {}", email);
        
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate and send OTP via email
        otpService.generateAndSendOtp(email, user.getFirstName());

        log.info("OTP sent successfully to email: {}", email);
        
        return OtpResponse.builder()
                .message("OTP sent to your email")
                .email(email)
                .expirationMinutes(3)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        log.info("Reset password with OTP for email: {}", email);
        
        // Find user
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify OTP
        otpService.verifyOtp(email, otpCode);

        // Check if new password is the same as current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password cannot be the same as the current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpires(null);
        userRepo.save(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email with token");
        
        // Find user by verification token
        User user = userRepo.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        // Update email verification status
        user.setIsEmailVerified(true);
        user.setVerificationToken(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepo.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    @Override
    public boolean checkEmailExists(String email) {
        log.info("Checking if email exists: {}", email);
        return userRepo.existsByEmailIgnoreCase(email);
    }
    
    @Override
    @Transactional
    public OtpResponse resendOtp(String email) {
        log.info("Resend OTP request for email: {}", email);
        
        // Get user from database
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Generate and send new OTP
        otpService.generateAndSendOtp(user.getEmail(), user.getFirstName());
        
        log.info("New OTP sent successfully to: {}", email);
        
        return OtpResponse.builder()
                .message("New OTP sent to your email.")
                .email(user.getEmail())
                .otpSent(true)
                .expirationMinutes(3)
                .build();
    }
}
