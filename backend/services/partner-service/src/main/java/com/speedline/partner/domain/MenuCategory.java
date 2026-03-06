package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * MenuCategory — catégorie de menu propre à un partenaire.
 *
 * Contrairement à l'entité globale {@link Category} (taxonomie partagée),
 * MenuCategory appartient exclusivement à un partenaire et structure
 * l'affichage des produits dans l'application client.
 */
@Entity
@Table(
    name = "partner_menu_categories",
    indexes = {
        @Index(name = "idx_partner_menu_category_partner",          columnList = "partnerId"),
        @Index(name = "idx_partner_menu_category_partner_position", columnList = "partnerId, position")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK logique vers le partenaire propriétaire (pas de contrainte DB inter-services). */
    @Column(nullable = false)
    private Long partnerId;

    /** Nom affiché dans le menu (ex: "Pizzas", "Boissons"). */
    @Column(nullable = false, length = 100)
    private String name;

    /** Description optionnelle visible par le client. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** URL de l'image d'illustration de la catégorie. */
    @Column(length = 500)
    private String imageUrl;

    /**
     * Ordre d'affichage dans le menu (tri croissant).
     * Auto-calculé lors de la création : max(position) + 1.
     */
    @Builder.Default
    private Integer position = 0;

    /**
     * Catégorie visible dans le menu client.
     * false = masquée temporairement (soft-delete fonctionnel).
     */
    @Builder.Default
    private Boolean isVisible = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
