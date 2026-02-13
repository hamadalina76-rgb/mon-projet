package com.speedline.user.dto;

import com.speedline.user.domain.AdminStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO pour la réponse Admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResponse {
    
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private AdminRoleResponse role;
    private AdminStatus status;
    private String avatar;
    private LocalDateTime lastLogin;
    private Set<String> customPermissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;
}
