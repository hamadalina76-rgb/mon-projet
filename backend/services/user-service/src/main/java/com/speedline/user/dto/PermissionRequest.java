package com.speedline.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'une Permission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {
    
    @NotBlank(message = "Le module est requis")
    private String module;
    
    @NotBlank(message = "Le label du module est requis")
    private String moduleLabel;
    
    private String icon;
    private String description;
    private Boolean enabled;
}
