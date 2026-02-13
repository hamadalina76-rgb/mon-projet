package com.speedline.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la création d'un log d'activité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogRequest {
    
    @NotNull(message = "Admin ID is required")
    private Long adminId;
    
    @NotBlank(message = "Admin name is required")
    private String adminName;
    
    @NotBlank(message = "Action is required")
    private String action;
    
    @NotBlank(message = "Resource is required")
    private String resource;
    
    private String resourceId;
    
    private String details;
}
