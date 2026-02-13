package com.speedline.user.dto;

import com.speedline.user.domain.AdminStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO pour la création/modification d'un Admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRequest {
    
    @NotBlank(message = "Le nom complet est requis")
    private String fullName;
    
    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;
    
    @NotNull(message = "Le rôle est requis")
    private Long roleId;
    
    private AdminStatus status;
    
    private String avatar;
    
    private Set<String> customPermissions;
    
    private String notes;
}
