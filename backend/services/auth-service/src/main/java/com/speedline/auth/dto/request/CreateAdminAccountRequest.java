package com.speedline.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer un compte admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminAccountRequest {
    
    @NotBlank(message = "Email est requis")
    @Email(message = "Email invalide")
    private String email;
    
    @NotBlank(message = "Mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;
    
    @NotBlank(message = "Nom complet est requis")
    private String fullName;
    
    @NotBlank(message = "Rôle est requis")
    private String role; // "ADMIN" ou "SUPER_ADMIN"
}
