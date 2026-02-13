package com.speedline.user.client;

import com.speedline.user.config.FeignConfig;
import com.speedline.user.dto.UserInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign Client pour communiquer avec Auth Service
 * Permet de récupérer les informations utilisateur et créer des comptes
 */
@FeignClient(name = "auth-service", configuration = FeignConfig.class)
public interface AuthServiceClient {

    /**
     * Récupérer les informations d'un utilisateur par son ID
     * 
     * @param userId ID de l'utilisateur
     * @return UserInfoDTO avec les données utilisateur
     */
    @GetMapping("/users/{userId}")
    UserInfoDTO getUserById(@PathVariable("userId") Long userId);
    
    /**
     * Crée un compte admin dans auth-service
     * 
     * @param request Données pour créer le compte
     * @return Informations du compte créé
     */
    @PostMapping("/api/v1/auth/admin/create-account")
    CreateUserResponse createAdminAccount(@RequestBody CreateAdminAccountRequest request);
    
    /**
     * Supprime un utilisateur dans auth-service
     */
    @DeleteMapping("/users/{userId}")
    void deleteUser(@PathVariable("userId") Long userId);
    
    /**
     * Change le statut d'un utilisateur dans auth-service
     */
    @PutMapping("/users/{userId}/status")
    void changeUserStatus(@PathVariable("userId") Long userId, @RequestParam("status") String status);
    
    /**
     * DTO pour créer un compte admin dans auth-service
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CreateAdminAccountRequest {
        private String email;
        private String password;
        private String fullName;
        private String role; // "ADMIN" ou "SUPER_ADMIN"
    }
    
    /**
     * DTO de réponse après création du compte
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CreateUserResponse {
        private Long userId;
        private String email;
        private String role;
    }
}
