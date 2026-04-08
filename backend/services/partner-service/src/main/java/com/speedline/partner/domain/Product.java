package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Product
 * Représente un produit/article d'un partenaire
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_partner", columnList = "partnerId"),
    @Index(name = "idx_product_category", columnList = "categoryId"),
    @Index(name = "idx_product_status", columnList = "status"),
    @Index(name = "idx_product_price", columnList = "price"),
    @Index(name = "idx_product_moderation_status", columnList = "moderation_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers le partenaire propriétaire
     */
    @Column(nullable = false)
    private Long partnerId;

    /**
     * Référence vers la catégorie du produit
     */
    private Long categoryId;

    // ==================== INFORMATIONS DE BASE ====================

    /**
     * Nom du produit
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Description du produit
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Description courte (pour les listes)
     */
    @Column(length = 255)
    private String shortDescription;

    /**
     * Prix de base
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Prix barré (ancien prix pour les promotions)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Pourcentage de réduction (si en promo)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    // ==================== IMAGES ====================

    /**
     * URL de l'image principale
     */
    @Column(length = 500)
    private String image;

    /**
     * URLs des images supplémentaires (JSON array)
     * Format: ["url1","url2","url3"]
     */
    @Column(columnDefinition = "TEXT")
    private String imagesJson;

    // ==================== DISPONIBILITÉ ====================

    /**
     * Produit disponible à la vente
     */
    @Builder.Default
    private Boolean isAvailable = true;

    /**
     * Quantité en stock (null = illimité)
     */
    private Integer stockQuantity;

    /**
     * Statut du produit
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    /**
     * Moderation status (workflow: PENDING -> APPROVED / REJECTED).
     * Existing products default to APPROVED.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductModerationStatus moderationStatus = ProductModerationStatus.APPROVED;

    /**
     * Reason provided by admin when rejecting the product.
     */
    @Column(columnDefinition = "TEXT")
    private String moderationReason;

    /**
     * Temps de préparation spécifique (en minutes, null = utiliser celui du partenaire).
     * Exposé en API comme {@code preparationTimeMin} dans {@code ProductResponse} ; le order-service
     * en prend le max par commande (plusieurs articles) et le stocke sur la commande.
     */
    private Integer preparationTime;

    // ==================== INFORMATIONS NUTRITIONNELLES ====================

    /**
     * Informations nutritionnelles (JSON)
     * Format: {"calories":500,"protein":20,"carbs":60,"fat":15}
     */
    @Column(columnDefinition = "TEXT")
    private String nutritionalInfoJson;

    /**
     * Allergènes (JSON array)
     * Format: ["gluten","lactose","nuts"]
     */
    @Column(columnDefinition = "TEXT")
    private String allergensJson;

    /**
     * Ingrédients (JSON array)
     */
    @Column(columnDefinition = "TEXT")
    private String ingredientsJson;

    // ==================== CARACTÉRISTIQUES ====================

    /**
     * Produit végétarien
     */
    @Builder.Default
    private Boolean isVegetarian = false;

    /**
     * Produit vegan
     */
    @Builder.Default
    private Boolean isVegan = false;

    /**
     * Produit halal
     */
    @Builder.Default
    private Boolean isHalal = false;

    /**
     * Sans gluten
     */
    @Builder.Default
    private Boolean isGlutenFree = false;

    /**
     * Produit épicé (niveau 0-3)
     */
    @Builder.Default
    private Integer spicyLevel = 0;

    /**
     * Produit populaire/best-seller
     */
    @Builder.Default
    private Boolean isPopular = false;

    /**
     * Nouveau produit
     */
    @Builder.Default
    private Boolean isNew = false;

    /**
     * Produit recommandé/featured
     */
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Label de promotion affiché en badge (ex. "-20%", "Nouveau", "Top vente").
     */
    @Column(length = 100)
    private String promotionLabel;

    /**
     * Date de début de la promotion (null = pas de début).
     */
    @Column
    private java.time.LocalDate promotionStartDate;

    /**
     * Date de fin de la promotion (null = pas de fin).
     */
    @Column
    private java.time.LocalDate promotionEndDate;

    // ==================== STATISTIQUES ====================

    /**
     * Nombre de fois commandé
     */
    @Builder.Default
    private Integer orderCount = 0;

    /**
     * Note moyenne (1-5)
     */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Nombre d'évaluations
     */
    @Builder.Default
    private Integer totalRatings = 0;

    // ==================== ORDRE D'AFFICHAGE ====================

    /**
     * Ordre d'affichage dans la catégorie
     */
    @Builder.Default
    private Integer displayOrder = 0;

    // ==================== TAGS ====================

    /**
     * Tags libres séparés par des virgules (ex: "signature,populaire,halal")
     * Utilisés pour le filtrage et la mise en avant dans l'app client.
     */
    @Column(length = 500)
    private String tags;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==================== RELATIONS ====================

    /**
     * Options du produit (tailles, cuissons, etc.)
     */
    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductOption> options = new ArrayList<>();

    /**
     * Suppléments/Addons disponibles
     */
    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductAddon> addons = new ArrayList<>();

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier si le produit est commandable
     */
    public boolean isOrderable() {
        return isAvailable
               && status == ProductStatus.ACTIVE
               && moderationStatus == ProductModerationStatus.APPROVED
               && (stockQuantity == null || stockQuantity > 0);
    }

    /**
     * Calculer le prix final (avec réduction si applicable)
     */
    public BigDecimal getFinalPrice() {
        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = price.multiply(discountPercentage).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            return price.subtract(discount);
        }
        return price;
    }

    /**
     * Vérifier si le produit est en promotion
     */
    public boolean isOnSale() {
        return originalPrice != null && originalPrice.compareTo(price) > 0;
    }

    /**
     * Incrémenter le compteur de commandes
     */
    public void incrementOrderCount() {
        this.orderCount++;
        // Marquer comme populaire après 50 commandes
        if (this.orderCount >= 50) {
            this.isPopular = true;
        }
    }

    /**
     * Décrémenter le stock
     */
    public boolean decrementStock(int quantity) {
        if (stockQuantity == null) return true; // Stock illimité
        if (stockQuantity >= quantity) {
            stockQuantity -= quantity;
            if (stockQuantity == 0) {
                status = ProductStatus.OUT_OF_STOCK;
                isAvailable = false;
            }
            return true;
        }
        return false;
    }

    /**
     * Mettre à jour la note moyenne
     */
    public void updateRating(BigDecimal newRating) {
        BigDecimal totalScore = this.rating.multiply(BigDecimal.valueOf(this.totalRatings));
        this.totalRatings++;
        this.rating = totalScore.add(newRating).divide(BigDecimal.valueOf(this.totalRatings), 2, java.math.RoundingMode.HALF_UP);
    }
}
