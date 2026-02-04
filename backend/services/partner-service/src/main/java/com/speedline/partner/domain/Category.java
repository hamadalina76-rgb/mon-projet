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
 * Entité Category
 * Catégories de partenaires et/ou de produits
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_parent", columnList = "parentId"),
    @Index(name = "idx_category_display_order", columnList = "displayOrder")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom de la catégorie
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Slug URL unique
     */
    @Column(unique = true, length = 100)
    private String slug;

    /**
     * Description de la catégorie
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * URL de l'icône
     */
    @Column(length = 500)
    private String icon;

    /**
     * URL de l'image de la catégorie
     */
    @Column(length = 500)
    private String image;

    /**
     * Catégorie parente (pour les sous-catégories)
     */
    private Long parentId;

    /**
     * Ordre d'affichage (plus petit = affiché en premier)
     */
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Catégorie active (visible dans l'app)
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Catégorie mise en avant sur la page d'accueil
     */
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Type de catégorie: PARTNER (pour les partenaires) ou PRODUCT (pour les produits)
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CategoryType categoryType = CategoryType.PARTNER;

    /**
     * Couleur de fond (hex)
     */
    @Column(length = 7)
    private String backgroundColor;

    /**
     * Couleur du texte (hex)
     */
    @Column(length = 7)
    private String textColor;

    /**
     * Nombre de partenaires dans cette catégorie
     */
    @Builder.Default
    private Integer partnerCount = 0;

    /**
     * Nombre de produits dans cette catégorie
     */
    @Builder.Default
    private Integer productCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==================== ENUMS ====================

    public enum CategoryType {
        PARTNER,
        PRODUCT
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Générer le slug à partir du nom
     */
    @PrePersist
    public void generateSlug() {
        if (this.slug == null && this.name != null) {
            this.slug = this.name.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-");
        }
    }

    /**
     * Vérifier si c'est une catégorie racine
     */
    public boolean isRootCategory() {
        return parentId == null;
    }
}
