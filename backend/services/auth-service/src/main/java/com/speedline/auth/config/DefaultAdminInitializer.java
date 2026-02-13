package com.speedline.auth.config;

import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.domain.UserStatus;
import com.speedline.auth.domain.AuthProvider;
import com.speedline.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialise automatiquement un compte SUPER_ADMIN par défaut si aucun n'existe.
 * S'exécute au démarrage de l'application.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Identifiants par défaut du SUPER_ADMIN
    private static final String DEFAULT_SUPER_ADMIN_EMAIL = "superadmin@speedline.com";
    private static final String DEFAULT_SUPER_ADMIN_PASSWORD = "SuperAdmin@2024";
    private static final String DEFAULT_SUPER_ADMIN_FIRST_NAME = "Super";
    private static final String DEFAULT_SUPER_ADMIN_LAST_NAME = "Admin";
    private static final String DEFAULT_SUPER_ADMIN_PHONE = "+21600000000";

    @Override
    public void run(String... args) {
        try {
            // Vérifier si un SUPER_ADMIN existe déjà
            long superAdminCount = userRepository.countByRole(Role.SUPER_ADMIN);

            if (superAdminCount == 0) {
                log.warn("╔════════════════════════════════════════════════════════╗");
                log.warn("║  AUCUN SUPER ADMINISTRATEUR TROUVÉ                     ║");
                log.warn("║  Création automatique du compte SUPER_ADMIN...        ║");
                log.warn("╚════════════════════════════════════════════════════════╝");

                // Créer l'utilisateur SUPER_ADMIN par défaut
                User superAdmin = User.builder()
                        .email(DEFAULT_SUPER_ADMIN_EMAIL)
                        .password(passwordEncoder.encode(DEFAULT_SUPER_ADMIN_PASSWORD))
                        .firstName(DEFAULT_SUPER_ADMIN_FIRST_NAME)
                        .lastName(DEFAULT_SUPER_ADMIN_LAST_NAME)
                        .phoneNumber(DEFAULT_SUPER_ADMIN_PHONE)
                        .role(Role.SUPER_ADMIN)
                        .status(UserStatus.ACTIVE)
                        .isEmailVerified(true)
                        .isPhoneVerified(false)
                        .authProvider(AuthProvider.LOCAL)
                        .build();

                userRepository.save(superAdmin);

                log.info("╔════════════════════════════════════════════════════════╗");
                log.info("║  ✓ COMPTE SUPER_ADMIN CRÉÉ AVEC SUCCÈS                ║");
                log.info("║                                                        ║");
                log.info("║  Email:    {}              ║", DEFAULT_SUPER_ADMIN_EMAIL);
                log.info("║  Password: {}                  ║", DEFAULT_SUPER_ADMIN_PASSWORD);
                log.info("║  Rôle:     SUPER_ADMIN                                 ║");
                log.info("║                                                        ║");
                log.info("║  🔐 Connexion Admin Panel:                             ║");
                log.info("║     http://localhost:4200/login                        ║");
                log.info("╚════════════════════════════════════════════════════════╝");
                log.warn("⚠️  SÉCURITÉ: Changez ce mot de passe après la première connexion!");
            } else {
                log.info("✓ SUPER_ADMIN déjà existant(s): {} compte(s)", superAdminCount);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation du compte SUPER_ADMIN: {}", e.getMessage(), e);
        }
    }
}
