package com.speedline.user.controller;

import com.speedline.user.dto.AdminRoleRequest;
import com.speedline.user.dto.AdminRoleResponse;
import com.speedline.user.service.AdminRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des AdminRoles
 */
@RestController
@RequestMapping("/v1/admin-roles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminRoleController {
    
    private final AdminRoleService roleService;
    
    /**
     * Récupère tous les rôles
     * GET /api/admin-roles
     */
    @GetMapping
    public ResponseEntity<List<AdminRoleResponse>> getAllRoles() {
        log.info("GET /api/admin-roles");
        List<AdminRoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Récupère tous les rôles actifs
     * GET /api/admin-roles/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<AdminRoleResponse>> getActiveRoles() {
        log.info("GET /api/admin-roles/active");
        List<AdminRoleResponse> roles = roleService.getActiveRoles();
        return ResponseEntity.ok(roles);
    }
    
    /**
     * Récupère un rôle par son ID
     * GET /api/admin-roles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminRoleResponse> getRoleById(@PathVariable Long id) {
        log.info("GET /api/admin-roles/{}", id);
        AdminRoleResponse role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Récupère un rôle par son code
     * GET /api/admin-roles/by-code/{code}
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<AdminRoleResponse> getRoleByCode(@PathVariable String code) {
        log.info("GET /api/admin-roles/by-code/{}", code);
        AdminRoleResponse role = roleService.getRoleByCode(code);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Crée un nouveau rôle
     * POST /api/admin-roles
     */
    @PostMapping
    public ResponseEntity<AdminRoleResponse> createRole(@Valid @RequestBody AdminRoleRequest request) {
        log.info("POST /api/admin-roles - code: {}", request.getCode());
        AdminRoleResponse role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }
    
    /**
     * Met à jour un rôle existant
     * PUT /api/admin-roles/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminRoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleRequest request
    ) {
        log.info("PUT /api/admin-roles/{}", id);
        AdminRoleResponse role = roleService.updateRole(id, request);
        return ResponseEntity.ok(role);
    }
    
    /**
     * Supprime un rôle
     * DELETE /api/admin-roles/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        log.info("DELETE /api/admin-roles/{}", id);
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Récupère les rôles par niveau de confiance minimum
     * GET /api/admin-roles/by-trust-level?minLevel=50
     */
    @GetMapping("/by-trust-level")
    public ResponseEntity<List<AdminRoleResponse>> getRolesByMinTrustLevel(
            @RequestParam Integer minLevel
    ) {
        log.info("GET /api/admin-roles/by-trust-level - minLevel: {}", minLevel);
        List<AdminRoleResponse> roles = roleService.getRolesByMinTrustLevel(minLevel);
        return ResponseEntity.ok(roles);
    }
}
