package com.speedline.user.service;

import com.speedline.user.domain.AdminRole;
import com.speedline.user.domain.Permission;
import com.speedline.user.dto.AdminRoleRequest;
import com.speedline.user.dto.AdminRoleResponse;
import com.speedline.user.dto.PermissionRequest;
import com.speedline.user.mapper.AdminMapper;
import com.speedline.user.repository.AdminRoleRepository;
import com.speedline.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des AdminRoles
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminRoleService {
    
    private final AdminRoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AdminMapper adminMapper;
    
    /**
     * Récupère tous les rôles
     */
    @Transactional(readOnly = true)
    public List<AdminRoleResponse> getAllRoles() {
        log.debug("Récupération de tous les rôles");
        return roleRepository.findAll().stream()
                .map(adminMapper::toRoleResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère tous les rôles actifs
     */
    @Transactional(readOnly = true)
    public List<AdminRoleResponse> getActiveRoles() {
        log.debug("Récupération des rôles actifs");
        return roleRepository.findByActiveTrue().stream()
                .map(adminMapper::toRoleResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère un rôle par son ID
     */
    @Transactional(readOnly = true)
    public AdminRoleResponse getRoleById(Long id) {
        log.debug("Récupération du rôle avec l'ID: {}", id);
        AdminRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + id));
        return adminMapper.toRoleResponse(role);
    }
    
    /**
     * Récupère un rôle par son code
     */
    @Transactional(readOnly = true)
    public AdminRoleResponse getRoleByCode(String code) {
        log.debug("Récupération du rôle avec le code: {}", code);
        AdminRole role = roleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec le code: " + code));
        return adminMapper.toRoleResponse(role);
    }
    
    /**
     * Crée un nouveau rôle
     */
    public AdminRoleResponse createRole(AdminRoleRequest request) {
        log.info("Création d'un nouveau rôle: {}", request.getCode());
        
        // Vérifier si le code existe déjà
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Un rôle existe déjà avec ce code: " + request.getCode());
        }
        
        // Créer le rôle
        AdminRole role = adminMapper.toRoleEntity(request);
        
        // Sauvegarder le rôle d'abord pour obtenir son ID
        AdminRole savedRole = roleRepository.save(role);
        
        // Ajouter les permissions si présentes
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            for (PermissionRequest permReq : request.getPermissions()) {
                Permission permission = adminMapper.toPermissionEntity(permReq, savedRole);
                savedRole.addPermission(permission);
            }
            savedRole = roleRepository.save(savedRole);
        }
        
        log.info("Rôle créé avec succès - ID: {}", savedRole.getId());
        return adminMapper.toRoleResponse(savedRole);
    }
    
    /**
     * Met à jour un rôle existant
     */
    public AdminRoleResponse updateRole(Long id, AdminRoleRequest request) {
        log.info("Mise à jour du rôle ID: {}", id);
        
        AdminRole role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + id));
        
        // Vérifier si le code est déjà utilisé par un autre rôle
        if (!role.getCode().equals(request.getCode()) && 
            roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Un rôle existe déjà avec ce code: " + request.getCode());
        }
        
        // Mettre à jour le rôle
        adminMapper.updateRoleEntity(role, request);
        
        // Gérer les permissions
        if (request.getPermissions() != null) {
            // Supprimer les anciennes permissions
            role.getPermissions().clear();
            
            // Ajouter les nouvelles permissions
            for (PermissionRequest permReq : request.getPermissions()) {
                Permission permission = adminMapper.toPermissionEntity(permReq, role);
                role.addPermission(permission);
            }
        }
        
        AdminRole updatedRole = roleRepository.save(role);
        log.info("Rôle mis à jour avec succès - ID: {}", updatedRole.getId());
        
        return adminMapper.toRoleResponse(updatedRole);
    }
    
    /**
     * Supprime un rôle
     */
    public void deleteRole(Long id) {
        log.info("Suppression du rôle ID: {}", id);
        
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("Rôle non trouvé avec l'ID: " + id);
        }
        
        roleRepository.deleteById(id);
        log.info("Rôle supprimé avec succès - ID: {}", id);
    }
    
    /**
     * Récupère les rôles par niveau de confiance minimum
     */
    @Transactional(readOnly = true)
    public List<AdminRoleResponse> getRolesByMinTrustLevel(Integer minTrustLevel) {
        log.debug("Récupération des rôles avec trustLevel >= {}", minTrustLevel);
        return roleRepository.findByMinTrustLevel(minTrustLevel).stream()
                .map(adminMapper::toRoleResponse)
                .collect(Collectors.toList());
    }
}
