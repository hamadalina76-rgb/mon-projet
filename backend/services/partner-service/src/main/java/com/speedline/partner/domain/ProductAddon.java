package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité ProductAddon
 * Suppléments/extras pouvant être ajoutés à un produit
 * Exemple: "Fromage supplémentaire", "Sauce spéciale", "Boisson"
 */
@Entity
@Table(name = "product_addons", indexes = {
    @Index(name = "idx_addon_product", columnList = "productId"),
    @Index(name = "idx_addon_display_order", columnList = "displayOrder")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers le produit
     */
    @Column(nullable = false)
    private Long productId;

    /**
     * Nom du supplément
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description du supplément
     */
    @Column(length = 255)
    private String description;

    /**
     * Prix du supplément (ajouté au total)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * URL de l'image du supplément
     */
    @Column(length = 500)
    private String image;

    /**
     * Supplément disponible actuellement
     */
    @Builder.Default
    private Boolean isAvailable = true;

    /**
     * Quantité maximum commandable (null = illimité)
     */
    private Integer maxQuantity;

    /**
     * Catégorie du supplément (pour regroupement)
     * Ex: "Sauces", "Boissons", "Desserts"
     */
    @Column(length = 50)
    private String category;

    /**
     * Ordre d'affichage
     */
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Supplément populaire/recommandé
     */
    @Builder.Default
    private Boolean isPopular = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Calculer le prix total pour une quantité donnée
     */
    public BigDecimal calculateTotal(int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Vérifier si la quantité demandée est valide
     */
    public boolean isValidQuantity(int quantity) {
        if (quantity <= 0) return false;
        if (maxQuantity != null && quantity > maxQuantity) return false;
        return true;
    }
}
