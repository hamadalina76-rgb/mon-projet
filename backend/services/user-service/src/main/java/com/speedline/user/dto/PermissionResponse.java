package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse Permission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    
    private Long id;
    private String module;
    private String moduleLabel;
    private String icon;
    private String description;
    private String descriptionEn;
    private String descriptionAr;
    private Boolean enabled;
}
