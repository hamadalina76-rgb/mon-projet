package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entité Admin - Administrateur de la plateforme SpeedLine
 */
@Entity
@Table(name = "admins", indexes = {
    @Index(name = "idx_admin_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_admin_email", columnList = "email"),
    @Index(name = "idx_admin_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur dans auth-service
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * Nom complet de l'administrateur
     */
    @Column(nullable = false, length = 100)
    private String fullName;

    /**
     * Email professionnel
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Rôle de l'administrateur
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    /**
     * Statut de l'administrateur
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AdminStatus status = AdminStatus.ACTIVE;

    /**
     * Avatar/photo de profil (URL ou initiales)
     */
    @Column(length = 10)
    private String avatar;

    /**
     * Dernière connexion
     */
    private LocalDateTime lastLogin;

    /**
     * Permissions spécifiques (au-delà du rôle)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_permissions", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<String> customPermissions = new HashSet<>();

    /**
     * Date de création
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de dernière modification
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Administrateur qui a créé ce compte
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Notes internes sur cet admin
     */
    @Column(length = 500)
    private String notes;

    /**
     * Vérifie si l'admin a une permission spécifique
     */
    public boolean hasPermission(String permission) {
        if (customPermissions.contains(permission)) {
            return true;
        }
        if (role != null && role.getPermissions() != null) {
            return role.getPermissions().stream()
                .anyMatch(p -> p.getModule().equals(permission));
        }
        return false;
    }

    /**
     * Vérifie si l'admin est actif
     */
    public boolean isActive() {
        return status == AdminStatus.ACTIVE;
    }
}
