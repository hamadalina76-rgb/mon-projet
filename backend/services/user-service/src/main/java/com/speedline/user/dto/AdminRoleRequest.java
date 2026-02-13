package com.speedline.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO pour la création/modification d'un AdminRole
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleRequest {
    
    @NotBlank(message = "Le code est requis")
    private String code;
    
    @NotBlank(message = "Le label est requis")
    private String label;
    
    private String description;
    private String color;
    
    @NotNull(message = "Le niveau de confiance est requis")
    private Integer trustLevel;
    
    private BigDecimal maxRefundAmount;
    private Boolean requiresApproval;
    private Set<PermissionRequest> permissions;
    private Boolean active;
}
