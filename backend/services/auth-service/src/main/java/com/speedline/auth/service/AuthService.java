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

    Object login(LoginRequest request);

    AuthResponse adminLogin(LoginRequest request);

    Object verifyOtp(VerifyOtpRequest request);
    
    AuthResponse socialLogin(SocialLoginRequest request);

    void logout(String token);

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

    /**
     * Envoie un email de bienvenue avec les credentiels au nouvel admin
     * @param email Email du nouvel admin
     * @param fullName Nom complet de l'admin
     * @param temporaryPassword Mot de passe provisoire
     */
    void sendAdminWelcomeEmail(String email, String fullName, String temporaryPassword);

    /**
     * Envoie l'email de réinitialisation de mot de passe à l'utilisateur (admin déclenche pour un client).
     * @param userId ID de l'utilisateur (client) qui recevra l'email
     */
    void sendResetPasswordEmailByUserId(Long userId);

    /**
     * Change le mot de passe de l'utilisateur connecté
     * @param email Email de l'utilisateur connecté
     * @param currentPassword Mot de passe actuel
     * @param newPassword Nouveau mot de passe
     */
    void changePassword(String email, String currentPassword, String newPassword);

    /**
     * Récupère le profil de l'utilisateur connecté
     * @param email Email de l'utilisateur
     * @return Profil (firstName, lastName, phoneNumber, etc.)
     */
    AuthResponse.UserInfo getProfile(String email);

    /**
     * Met à jour le profil utilisateur (firstName, lastName, phoneNumber)
     * @param email Email de l'utilisateur connecté
     * @param firstName Prénom
     * @param lastName Nom
     * @param phoneNumber Téléphone (optionnel)
     */
    void updateProfile(String email, String firstName, String lastName, String phoneNumber);

    OtpResponse resendOtp(String email);
}
