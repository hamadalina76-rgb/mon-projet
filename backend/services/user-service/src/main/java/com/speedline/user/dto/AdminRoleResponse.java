package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO pour la réponse AdminRole
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleResponse {
    
    private Long id;
    private String code;
    private String label;
    private String description;
    private String color;
    private Integer trustLevel;
    private BigDecimal maxRefundAmount;
    private Boolean requiresApproval;
    private Set<PermissionResponse> permissions;
    private LocalDateTime createdAt;
    private Boolean active;
}
