package com.speedline.user.mapper;

import com.speedline.user.domain.Admin;
import com.speedline.user.domain.AdminRole;
import com.speedline.user.domain.Permission;
import com.speedline.user.dto.*;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper pour convertir entre les entités Admin et les DTOs
 */
@Component
public class AdminMapper {
    
    public AdminResponse toResponse(Admin admin) {
        if (admin == null) {
            return null;
        }
        
        return AdminResponse.builder()
                .id(admin.getId())
                .userId(admin.getUserId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role(toRoleResponse(admin.getRole()))
                .status(admin.getStatus())
                .avatar(admin.getAvatar())
                .lastLogin(admin.getLastLogin())
                .customPermissions(admin.getCustomPermissions())
                .createdAt(admin.getCreatedAt())
                .updatedAt(admin.getUpdatedAt())
                .notes(admin.getNotes())
                .build();
    }
    
    public Admin toEntity(AdminRequest request, AdminRole role) {
        if (request == null) {
            return null;
        }
        
        Admin admin = new Admin();
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setRole(role);
        admin.setStatus(request.getStatus());
        admin.setAvatar(request.getAvatar());
        admin.setCustomPermissions(request.getCustomPermissions());
        admin.setNotes(request.getNotes());
        
        return admin;
    }
    
    public void updateEntity(Admin admin, AdminRequest request, AdminRole role) {
        if (admin == null || request == null) {
            return;
        }
        
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setRole(role);
        if (request.getStatus() != null) {
            admin.setStatus(request.getStatus());
        }
        admin.setAvatar(request.getAvatar());
        admin.setCustomPermissions(request.getCustomPermissions());
        admin.setNotes(request.getNotes());
    }
    
    public AdminRoleResponse toRoleResponse(AdminRole role) {
        if (role == null) {
            return null;
        }
        
        Set<PermissionResponse> permissions = null;
        if (role.getPermissions() != null) {
            permissions = role.getPermissions().stream()
                    .map(this::toPermissionResponse)
                    .collect(Collectors.toSet());
        }
        
        return AdminRoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .label(role.getLabel())
                .description(role.getDescription())
                .color(role.getColor())
                .trustLevel(role.getTrustLevel())
                .maxRefundAmount(role.getMaxRefundAmount())
                .requiresApproval(role.getRequiresApproval())
                .permissions(permissions)
                .createdAt(role.getCreatedAt())
                .active(role.getActive())
                .build();
    }
    
    public AdminRole toRoleEntity(AdminRoleRequest request) {
        if (request == null) {
            return null;
        }
        
        AdminRole role = new AdminRole();
        role.setCode(request.getCode());
        role.setLabel(request.getLabel());
        role.setDescription(request.getDescription());
        role.setColor(request.getColor());
        role.setTrustLevel(request.getTrustLevel());
        role.setMaxRefundAmount(request.getMaxRefundAmount());
        role.setRequiresApproval(request.getRequiresApproval());
        role.setActive(request.getActive() != null ? request.getActive() : true);
        
        return role;
    }
    
    public void updateRoleEntity(AdminRole role, AdminRoleRequest request) {
        if (role == null || request == null) {
            return;
        }
        
        role.setCode(request.getCode());
        role.setLabel(request.getLabel());
        role.setDescription(request.getDescription());
        role.setColor(request.getColor());
        role.setTrustLevel(request.getTrustLevel());
        role.setMaxRefundAmount(request.getMaxRefundAmount());
        role.setRequiresApproval(request.getRequiresApproval());
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }
    }
    
    public PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) {
            return null;
        }
        
        return PermissionResponse.builder()
                .id(permission.getId())
                .module(permission.getModule())
                .moduleLabel(permission.getModuleLabel())
                .icon(permission.getIcon())
                .description(permission.getDescription())
                .descriptionEn(permission.getDescriptionEn())
                .descriptionAr(permission.getDescriptionAr())
                .enabled(permission.getEnabled())
                .build();
    }
    
    public Permission toPermissionEntity(PermissionRequest request, AdminRole role) {
        if (request == null) {
            return null;
        }
        
        Permission permission = new Permission();
        permission.setModule(request.getModule());
        permission.setModuleLabel(request.getModuleLabel());
        permission.setIcon(request.getIcon());
        permission.setDescription(request.getDescription());
        permission.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        permission.setRole(role);
        
        return permission;
    }
}
