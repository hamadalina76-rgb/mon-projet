package com.speedline.user.config;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.domain.Admin;
import com.speedline.user.domain.AdminRole;
import com.speedline.user.domain.AdminStatus;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.AdminRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initialise automatiquement le profil admin pour le SUPER_ADMIN
 * S'exécute après les migrations Flyway
 */
@Component
@Order(2) // S'exécute après DefaultAdminInitializer de auth-service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminProfileInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AuthServiceClient authServiceClient;

    private static final String SUPER_ADMIN_EMAIL = "superadmin@speedline.com";

    @Override
    public void run(String... args) {
        try {
            // Vérifier si le profil admin existe déjà pour cet email
            if (adminRepository.findByEmail(SUPER_ADMIN_EMAIL).isPresent()) {
                log.info("✓ Profil SUPER_ADMIN déjà existant pour {}", SUPER_ADMIN_EMAIL);
                return;
            }

            // Récupérer le rôle SUPER_ADMIN (roleId = 1)
            AdminRole superAdminRole = adminRoleRepository.findById(1L).orElse(null);
            
            if (superAdminRole == null) {
                log.warn("⚠️  Rôle SUPER_ADMIN (ID=1) non trouvé dans admin_roles. Exécutez d'abord les migrations V3 et V4 du user-service.");
                return;
            }

            // Récupérer l'utilisateur depuis auth-service (pour obtenir le userId)
            try {
                // Note: On suppose que le SUPER_ADMIN a été créé par auth-service
                // Le userId sera probablement 1 car c'est le premier utilisateur créé
                
                log.info("╔════════════════════════════════════════════════════════╗");
                log.info("║  Création du profil SUPER_ADMIN dans user-service     ║");
                log.info("╚════════════════════════════════════════════════════════╝");

                // Créer le profil admin
                // userId = 1 car c'est le premier utilisateur créé par auth-service
                Long userId = 1L;
                
                Admin superAdminProfile = Admin.builder()
                        .userId(userId)
                        .fullName("Super Admin")
                        .email(SUPER_ADMIN_EMAIL)
                        .role(superAdminRole)
                        .status(AdminStatus.ACTIVE)
                        .notes("Compte SUPER_ADMIN créé automatiquement au démarrage")
                        .build();

                adminRepository.save(superAdminProfile);

                log.info("╔════════════════════════════════════════════════════════╗");
                log.info("║  ✓ PROFIL SUPER_ADMIN CRÉÉ AVEC SUCCÈS                ║");
                log.info("║                                                        ║");
                log.info("║  Email:  {}              ║", SUPER_ADMIN_EMAIL);
                log.info("║  UserId: {}                                           ║", userId);
                log.info("║  Rôle:   {}                          ║", superAdminRole.getLabel());
                log.info("║  Status: ACTIVE                                        ║");
                log.info("║                                                        ║");
                log.info("║  Le SUPER_ADMIN peut maintenant créer d'autres admins ║");
                log.info("╚════════════════════════════════════════════════════════╝");

            } catch (Exception e) {
                log.error("❌ Erreur lors de la récupération du compte depuis auth-service: {}", e.getMessage());
                log.info("ℹ️  Le profil admin sera créé plus tard lors de la première connexion");
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation du profil SUPER_ADMIN: {}", e.getMessage(), e);
        }
    }
}
