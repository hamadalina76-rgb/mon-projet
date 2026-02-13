package com.speedline.user.controller;

import com.speedline.user.domain.AdminStatus;
import com.speedline.user.dto.AdminRequest;
import com.speedline.user.dto.AdminResponse;
import com.speedline.user.dto.CreateAdminWithAuthRequest;
import com.speedline.user.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des Admins
 */
@RestController
@RequestMapping("/api/v1/admins")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * Récupère tous les admins avec pagination et filtres
     * GET /api/admins?page=0&size=20&search=john&status=ACTIVE&roleId=1
     */
    @GetMapping
    public ResponseEntity<Page<AdminResponse>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AdminStatus status,
            @RequestParam(required = false) Long roleId
    ) {
        log.info("GET /api/admins - page: {}, size: {}, search: {}, status: {}, roleId: {}", 
                 page, size, search, status, roleId);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<AdminResponse> admins;
        if (search != null || status != null || roleId != null) {
            admins = adminService.searchAdmins(search, status, roleId, pageable);
        } else {
            admins = adminService.getAllAdmins(pageable);
        }
        
        return ResponseEntity.ok(admins);
    }
    
    /**
     * Récupère un admin par son ID
     * GET /api/admins/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminResponse> getAdminById(@PathVariable Long id) {
        log.info("GET /api/admins/{}", id);
        AdminResponse admin = adminService.getAdminById(id);
        return ResponseEntity.ok(admin);
    }
    
    /**
     * Récupère un admin par son userId
     * GET /api/admins/by-user/{userId}
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<AdminResponse> getAdminByUserId(@PathVariable Long userId) {
        log.info("GET /api/admins/by-user/{}", userId);
        AdminResponse admin = adminService.getAdminByUserId(userId);
        return ResponseEntity.ok(admin);
    }
    
    /**
     * Crée un nouvel admin
     * POST /api/admins
     */
    @PostMapping
    public ResponseEntity<AdminResponse> createAdmin(
            @Valid @RequestBody AdminRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long createdBy
    ) {
        log.info("POST /api/admins - email: {}", request.getEmail());
        AdminResponse admin = adminService.createAdmin(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }
    
    /**
     * Crée un admin avec son compte d'authentification
     * POST /api/v1/admins/with-auth
     * Cette route crée à la fois:
     * - Un compte utilisateur dans auth-service
     * - Un profil admin dans user-service
     */
    @PostMapping("/with-auth")
    public ResponseEntity<AdminResponse> createAdminWithAuth(
            @Valid @RequestBody CreateAdminWithAuthRequest request
    ) {
        log.info("POST /api/v1/admins/with-auth - email: {}", request.getEmail());
        // Pour l'instant, on ne gère pas le createdBy depuis le header
        AdminResponse admin = adminService.createAdminWithAuth(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }
    
    /**
     * Met à jour un admin existant
     * PUT /api/admins/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminResponse> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequest request
    ) {
        log.info("PUT /api/admins/{}", id);
        AdminResponse admin = adminService.updateAdmin(id, request);
        return ResponseEntity.ok(admin);
    }
    
    /**
     * Supprime un admin
     * DELETE /api/admins/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        log.info("DELETE /api/admins/{}", id);
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Change le statut d'un admin
     * PATCH /api/admins/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminResponse> changeStatus(
            @PathVariable Long id,
            @RequestParam AdminStatus status
    ) {
        log.info("PATCH /api/admins/{}/status - newStatus: {}", id, status);
        AdminResponse admin = adminService.changeStatus(id, status);
        return ResponseEntity.ok(admin);
    }
    
    /**
     * Met à jour la dernière connexion d'un admin
     * POST /api/admins/last-login/{userId}
     */
    @PostMapping("/last-login/{userId}")
    public ResponseEntity<Void> updateLastLogin(@PathVariable Long userId) {
        log.info("POST /api/admins/last-login/{}", userId);
        adminService.updateLastLogin(userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Compte le nombre d'admins par statut
     * GET /api/admins/count-by-status?status=ACTIVE
     */
    @GetMapping("/count-by-status")
    public ResponseEntity<Long> countByStatus(@RequestParam AdminStatus status) {
        log.info("GET /api/admins/count-by-status - status: {}", status);
        long count = adminService.countByStatus(status);
        return ResponseEntity.ok(count);
    }
}
