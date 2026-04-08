package com.speedline.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité OrderItem - Article d'une commande
 * Contient des données dénormalisées pour l'historique
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_item_order", columnList = "orderId"),
    @Index(name = "idx_order_item_product", columnList = "productId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers la commande parente
     */
    @Column(nullable = false)
    private Long orderId;

    /**
     * ID du produit (partner-service)
     */
    @Column(nullable = false)
    private Long productId;

    // ==================== DONNÉES DÉNORMALISÉES ====================

    /**
     * Nom du produit (copie pour historique)
     */
    @Column(nullable = false, length = 255)
    private String productName;

    /**
     * Description du produit (copie)
     */
    @Column(columnDefinition = "TEXT")
    private String productDescription;

    /**
     * Image du produit (copie)
     */
    @Column(length = 500)
    private String productImage;

    // ==================== QUANTITÉ ET PRIX ====================

    /**
     * Quantité commandée
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Prix unitaire (au moment de la commande)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Sous-total de cet article (unitPrice * quantity + options + addons)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    // ==================== OPTIONS ET SUPPLÉMENTS ====================

    /**
     * Options sélectionnées (JSON)
     * Format: [{"optionId":1,"optionName":"Taille","valueId":2,"valueName":"Medium","priceModifier":2.00},...]
     */
    @Column(columnDefinition = "TEXT")
    private String selectedOptionsJson;

    /**
     * Suppléments sélectionnés (JSON)
     * Format: [{"addonId":1,"addonName":"Extra fromage","quantity":1,"price":1.50},...]
     */
    @Column(columnDefinition = "TEXT")
    private String selectedAddonsJson;

    /**
     * Total des modificateurs de prix (options + addons)
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal modifiersTotal = BigDecimal.ZERO;

    // ==================== INSTRUCTIONS ====================

    /**
     * Instructions spéciales pour cet article
     */
    @Column(length = 500)
    private String specialInstructions;

    /**
     * Minutes de préparation indiquées sur la fiche produit (snapshot au moment de la commande).
     */
    @Column
    private Integer preparationTimeMin;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Calculer le sous-total de cet article
     */
    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {
        BigDecimal basePrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal modifiers = modifiersTotal.multiply(BigDecimal.valueOf(quantity));
        this.subtotal = basePrice.add(modifiers);
    }

    /**
     * Obtenir le prix unitaire total (prix de base + modificateurs)
     */
    public BigDecimal getTotalUnitPrice() {
        return unitPrice.add(modifiersTotal);
    }
}
