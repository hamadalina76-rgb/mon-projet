package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Rôle d'administrateur avec permissions associées
 */
@Entity
@Table(name = "admin_roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "permissions")
@EqualsAndHashCode(exclude = "permissions")
public class AdminRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Code unique du rôle (ex: SUPER_ADMIN, FINANCE, SUPPORT)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Nom d'affichage du rôle
     */
    @Column(nullable = false, length = 100)
    private String label;

    /**
     * Description du rôle
     */
    @Column(length = 255)
    private String description;

    /**
     * Couleur pour l'affichage (hex code)
     */
    @Column(length = 7)
    @Builder.Default
    private String color = "#94A3B8";

    /**
     * Niveau de confiance (0-100)
     * Utilisé pour les limites de validation automatique
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer trustLevel = 50;

    /**
     * Montant maximum de remboursement autorisé
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxRefundAmount = BigDecimal.ZERO;

    /**
     * Nécessite une approbation supplémentaire pour certaines actions
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    /**
     * Permissions associées à ce rôle
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Date de création
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Rôle actif ou non
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Ajoute une permission au rôle
     */
    public void addPermission(Permission permission) {
        permissions.add(permission);
        permission.setRole(this);
    }

    /**
     * Retire une permission du rôle
     */
    public void removePermission(Permission permission) {
        permissions.remove(permission);
        permission.setRole(null);
    }
}
