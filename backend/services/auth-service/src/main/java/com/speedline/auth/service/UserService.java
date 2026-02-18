package com.speedline.auth.service;

import com.speedline.auth.dto.request.UpdateUserRequest;
import com.speedline.auth.dto.response.UserInfoResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service pour gérer les opérations utilisateur (database persistence only)
 */
public interface UserService {
    
    /**
     * Met à jour le profil utilisateur dans la base de données
     */
    UserInfoResponse updateUserProfile(Long userId, UpdateUserRequest request);
    
    /**
     * Récupère les informations utilisateur
     */
    UserInfoResponse getUserInfo(Long userId);
}
