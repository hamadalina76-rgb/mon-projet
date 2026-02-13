package com.speedline.auth.service;

import com.speedline.auth.dto.request.CreateAdminAccountRequest;
import com.speedline.auth.dto.request.LoginRequest;
import com.speedline.auth.dto.request.RegisterRequest;
import com.speedline.auth.dto.request.SocialLoginRequest;
import com.speedline.auth.dto.request.VerifyOtpRequest;
import com.speedline.auth.dto.response.AuthResponse;
import com.speedline.auth.dto.response.OtpResponse;

public interface AuthService {

    void register(RegisterRequest request);

    OtpResponse login(LoginRequest request);

    AuthResponse adminLogin(LoginRequest request);

    Object verifyOtp(VerifyOtpRequest request);
    
    AuthResponse socialLogin(SocialLoginRequest request);

    void logout(String email);

    AuthResponse refreshToken(String refreshToken);

    OtpResponse forgotPassword(String email);

    void resetPassword(String email, String otpCode, String newPassword);

    void verifyEmail(String token);
    
    boolean checkEmailExists(String email);

    /**
     * Crée un compte admin (appelé par user-service)
     * @param request Détails du compte à créer
     * @return ID de l'utilisateur créé
     */
    Long createAdminAccount(CreateAdminAccountRequest request);

    OtpResponse resendOtp(String email);
}
