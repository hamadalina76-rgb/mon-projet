package com.speedline.user.service;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.domain.Admin;
import com.speedline.user.domain.AdminRole;
import com.speedline.user.domain.AdminStatus;
import com.speedline.user.dto.AdminRequest;
import com.speedline.user.dto.AdminResponse;
import com.speedline.user.dto.CreateAdminWithAuthRequest;
import com.speedline.user.mapper.AdminMapper;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service pour la gestion des Admins
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {
    
    private final AdminRepository adminRepository;
    private final AdminRoleRepository roleRepository;
    private final AdminMapper adminMapper;
    private final AuthServiceClient authServiceClient;
    
    /**
     * Récupère tous les admins avec pagination
     */
    @Transactional(readOnly = true)
    public Page<AdminResponse> getAllAdmins(Pageable pageable) {
        log.debug("Récupération de tous les admins avec pagination: {}", pageable);
        return adminRepository.findAll(pageable)
                .map(adminMapper::toResponse);
    }
    
    /**
     * Recherche des admins avec filtres
     */
    @Transactional(readOnly = true)
    public Page<AdminResponse> searchAdmins(String search, AdminStatus status, Long roleId, Pageable pageable) {
        log.debug("Recherche d'admins - search: {}, status: {}, roleId: {}", search, status, roleId);
        return adminRepository.findWithFilters(search, status, roleId, pageable)
                .map(adminMapper::toResponse);
    }
    
    /**
     * Récupère un admin par son ID
     */
    @Transactional(readOnly = true)
    public AdminResponse getAdminById(Long id) {
        log.debug("Récupération de l'admin avec l'ID: {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'ID: " + id));
        return adminMapper.toResponse(admin);
    }
    
    /**
     * Récupère un admin par son userId
     */
    @Transactional(readOnly = true)
    public AdminResponse getAdminByUserId(Long userId) {
        log.debug("Récupération de l'admin avec le userId: {}", userId);
        return adminRepository.findByUserId(userId)
                .map(adminMapper::toResponse)
                .orElseGet(() -> {
                    // Some callers historically sent admin.id instead of userId.
                    // Try by primary key before returning a generic placeholder.
                    return adminRepository.findById(userId)
                            .map(adminMapper::toResponse)
                            .orElseGet(() -> AdminResponse.builder()
                                    .userId(userId)
                                    .fullName("Admin")
                                    .build());
                });
    }
    
    /**
     * Crée un nouvel admin
     */
    public AdminResponse createAdmin(AdminRequest request, Long createdBy) {
        log.info("Création d'un nouvel admin: {}", request.getEmail());
        
        // Vérifier si l'email existe déjà
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un admin existe déjà avec cet email: " + request.getEmail());
        }
        
        // Récupérer le rôle
        AdminRole role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + request.getRoleId()));
        
        // Créer l'admin
        Admin admin = adminMapper.toEntity(request, role);
        admin.setStatus(request.getStatus() != null ? request.getStatus() : AdminStatus.PENDING);
        admin.setCreatedBy(createdBy);
        
        // Sauvegarder
        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin créé avec succès - ID: {}", savedAdmin.getId());
        
        return adminMapper.toResponse(savedAdmin);
    }
    
    /**
     * Met à jour un admin existant
     */
    public AdminResponse updateAdmin(Long id, AdminRequest request) {
        log.info("Mise à jour de l'admin ID: {}", id);
        
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'ID: " + id));
        
        // Vérifier si l'email est déjà utilisé par un autre admin
        if (!admin.getEmail().equals(request.getEmail()) && 
            adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un admin existe déjà avec cet email: " + request.getEmail());
        }
        
        // Récupérer le nouveau rôle
        AdminRole role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + request.getRoleId()));
        
        // Mettre à jour l'admin
        adminMapper.updateEntity(admin, request, role);
        
        Admin updatedAdmin = adminRepository.save(admin);
        log.info("Admin mis à jour avec succès - ID: {}", updatedAdmin.getId());
        
        return adminMapper.toResponse(updatedAdmin);
    }
    
    /**
     * Supprime un admin du user-service ET de l'auth-service
     */
    public void deleteAdmin(Long id) {
        log.info("Suppression de l'admin ID: {}", id);
        
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'ID: " + id));
        
        // 1. Supprimer le compte dans auth-service
        if (admin.getUserId() != null) {
            try {
                log.info("Suppression du compte auth pour userId: {}", admin.getUserId());
                authServiceClient.deleteUser(admin.getUserId());
                log.info("Compte auth supprimé avec succès pour userId: {}", admin.getUserId());
            } catch (Exception e) {
                log.error("Erreur lors de la suppression du compte auth userId {}: {}", admin.getUserId(), e.getMessage());
                // On continue quand même pour supprimer le profil admin
            }
        }
        
        // 2. Supprimer le profil admin dans user-service
        adminRepository.deleteById(id);
        log.info("Admin supprimé avec succès des deux services - ID: {}", id);
    }
    
    /**
     * Change le statut d'un admin (synchronisé avec auth-service)
     */
    public AdminResponse changeStatus(Long id, AdminStatus newStatus) {
        log.info("Changement du statut de l'admin ID: {} vers {}", id, newStatus);
        
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'ID: " + id));
        
        admin.setStatus(newStatus);
        Admin updatedAdmin = adminRepository.save(admin);
        
        // Synchroniser le statut dans auth-service (obligatoire - bloque la connexion si échec)
        if (admin.getUserId() != null) {
            String authStatus = mapToAuthStatus(newStatus);
            log.info("Synchronisation du statut auth pour userId {} → {}", admin.getUserId(), authStatus);
            authServiceClient.changeUserStatus(admin.getUserId(), authStatus);
            log.info("✓ Synchronisation réussie du statut auth pour userId {}", admin.getUserId());
        } else {
            log.warn("⚠ Admin {} n'a pas de userId, synchronisation auth-service impossible", id);
        }
        
        return adminMapper.toResponse(updatedAdmin);
    }
    
    /**
     * Mappe le statut admin (user-service) vers le statut auth (auth-service)
     */
    private String mapToAuthStatus(AdminStatus status) {
        return switch (status) {
            case ACTIVE -> "ACTIVE";
            case SUSPENDED -> "SUSPENDED";
            case INACTIVE -> "INACTIVE"; // INACTIVE = désactivé (différent de SUSPENDED)
            case PENDING -> "PENDING";
        };
    }
    
    /**
     * Met à jour la dernière connexion d'un admin
     */
    public void updateLastLogin(Long userId) {
        log.debug("Mise à jour de la dernière connexion pour userId: {}", userId);
        
        adminRepository.findByUserId(userId).ifPresent(admin -> {
            admin.setLastLogin(LocalDateTime.now());
            adminRepository.save(admin);
        });
    }
    
    /**
     * Compte le nombre d'admins par statut
     */
    @Transactional(readOnly = true)
    public long countByStatus(AdminStatus status) {
        return adminRepository.countByStatus(status);
    }

    /**
     * Synchronise le statut actuel de l'admin vers auth-service.
     * Utile pour corriger les comptes désynchronisés (ex: admin INACTIVE dans user-service
     * mais encore ACTIVE dans auth-service).
     */
    public AdminResponse syncStatusToAuth(Long id) {
        log.info("Synchronisation du statut auth pour l'admin ID: {}", id);

        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé avec l'ID: " + id));

        if (admin.getUserId() == null) {
            throw new RuntimeException("Cet admin n'a pas de compte d'authentification lié.");
        }

        String authStatus = mapToAuthStatus(admin.getStatus());
        log.info("Envoi du statut {} vers auth-service pour userId {}", authStatus, admin.getUserId());
        authServiceClient.changeUserStatus(admin.getUserId(), authStatus);
        log.info("✓ Synchronisation réussie du statut auth pour admin {}", id);

        return adminMapper.toResponse(admin);
    }
    
    /**
     * Crée un admin avec son compte d'authentification
     * Cette méthode:
     * 1. Crée le compte dans auth-service
     * 2. Crée le profil admin dans user-service avec le userId retourné
     */
    public AdminResponse createAdminWithAuth(CreateAdminWithAuthRequest request, Long createdBy) {
        log.info("Création d'un admin avec compte auth: {}", request.getEmail());
        
        // Vérifier si l'email existe déjà
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un admin existe déjà avec cet email: " + request.getEmail());
        }
        
        // Récupérer le rôle admin
        AdminRole role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé avec l'ID: " + request.getRoleId()));
        
        // Déterminer le rôle auth (ADMIN ou SUPER_ADMIN)
        String authRole = role.getCode().equals("SUPER_ADMIN") ? "SUPER_ADMIN" : "ADMIN";
        
        try {
            // 1. Créer le compte dans auth-service
            log.info("Création du compte auth pour {}", request.getEmail());
            AuthServiceClient.CreateUserResponse authResponse = authServiceClient.createAdminAccount(
                AuthServiceClient.CreateAdminAccountRequest.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .fullName(request.getFullName())
                    .role(authRole)
                    .build()
            );
            
            log.info("Compte auth créé avec userId: {}", authResponse.getUserId());
            
            // 2. Créer le profil admin dans user-service
            Admin admin = Admin.builder()
                .userId(authResponse.getUserId())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(role)
                .status(request.getStatus() != null ? request.getStatus() : AdminStatus.ACTIVE)
                .avatar(request.getAvatar())
                .customPermissions(request.getCustomPermissions() != null ? request.getCustomPermissions() : new java.util.HashSet<>())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();
            
            Admin savedAdmin = adminRepository.save(admin);
            log.info("Profil admin créé avec succès - ID: {}, userId: {}", savedAdmin.getId(), savedAdmin.getUserId());
            
            // 3. Envoyer l'email de bienvenue avec les credentiels
            try {
                log.info("Envoi de l'email de bienvenue à: {}", request.getEmail());
                authServiceClient.sendAdminWelcomeEmail(
                    AuthServiceClient.SendWelcomeEmailRequest.builder()
                        .email(request.getEmail())
                        .fullName(request.getFullName())
                        .temporaryPassword(request.getPassword())
                        .build()
                );
                log.info("Email de bienvenue envoyé avec succès à: {}", request.getEmail());
            } catch (Exception emailEx) {
                log.warn("Échec de l'envoi de l'email de bienvenue à {}: {}. Admin créé quand même.", 
                         request.getEmail(), emailEx.getMessage());
                // On ne bloque pas la création si l'email échoue
            }
            
            return adminMapper.toResponse(savedAdmin);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'admin avec auth: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création du compte admin: " + e.getMessage());
        }
    }
}
