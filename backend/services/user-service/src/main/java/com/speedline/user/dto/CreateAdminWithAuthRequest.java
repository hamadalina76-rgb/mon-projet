package com.speedline.user.dto;

import com.speedline.user.domain.AdminStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO pour créer un admin avec son compte d'authentification
 * Utilisé pour créer simultanément:
 * - Un compte utilisateur dans auth-service
 * - Un profil admin dans user-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminWithAuthRequest {
    
    @NotBlank(message = "Le nom complet est requis")
    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String fullName;
    
    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;
    
    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
    
    @NotNull(message = "Le rôle est requis")
    private Long roleId;
    
    private AdminStatus status = AdminStatus.ACTIVE;
    
    private String avatar;
    
    private String notes;
    
    @Builder.Default
    private Set<String> customPermissions = new HashSet<>();
}
