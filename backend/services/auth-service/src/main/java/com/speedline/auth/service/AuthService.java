package com.speedline.auth.service;

import com.speedline.auth.dto.request.LoginRequest;
import com.speedline.auth.dto.request.RegisterRequest;
import com.speedline.auth.dto.request.SocialLoginRequest;
import com.speedline.auth.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
    
    AuthResponse socialLogin(SocialLoginRequest request);

    void logout(String email);

    AuthResponse refreshToken(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    void verifyEmail(String token);
    
    boolean checkEmailExists(String email);
}
