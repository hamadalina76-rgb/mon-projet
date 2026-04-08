package com.speedline.order.domain;

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
 * Entité Order - Commande client
 * Contient des données dénormalisées pour éviter les appels inter-services
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "orderNumber", unique = true),
    @Index(name = "idx_order_customer", columnList = "customerId"),
    @Index(name = "idx_order_partner", columnList = "partnerId"),
    @Index(name = "idx_order_courier", columnList = "courierId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Numéro de commande unique (ex: "ORD-2026-00001")
     */
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    // ==================== RÉFÉRENCES (PAS DE FK) ====================

    /**
     * ID du client (user-service)
     */
    @Column(nullable = false)
    private Long customerId;

    /**
     * ID du partenaire (partner-service)
     */
    @Column(nullable = false)
    private Long partnerId;

    /**
     * ID du livreur assigné (user-service, peut être null)
     */
    private Long courierId;

    // ==================== DONNÉES DÉNORMALISÉES - CLIENT ====================

    /**
     * Nom complet du client (copie)
     */
    @Column(length = 255)
    private String customerName;

    /**
     * Email du client (copie)
     */
    @Column(length = 255)
    private String customerEmail;

    /**
     * Téléphone du client (copie)
     */
    @Column(length = 20)
    private String customerPhone;

    // ==================== DONNÉES DÉNORMALISÉES - PARTENAIRE ====================

    /**
     * Nom du partenaire (copie)
     */
    @Column(length = 255)
    private String partnerName;

    /**
     * Adresse du partenaire (copie)
     */
    @Column(length = 500)
    private String partnerAddress;

    /**
     * Téléphone du partenaire (copie)
     */
    @Column(length = 20)
    private String partnerPhone;

    // ==================== DONNÉES DÉNORMALISÉES - LIVREUR ====================

    /**
     * Nom du livreur (copie)
     */
    @Column(length = 255)
    private String courierName;

    /**
     * Téléphone du livreur (copie)
     */
    @Column(length = 20)
    private String courierPhone;

    // ==================== STATUT ====================

    /**
     * Statut actuel de la commande
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Type de commande (DELIVERY, PICKUP)
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderType type = OrderType.DELIVERY;

    // ==================== MONTANTS ====================

    /**
     * Sous-total (somme des articles)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Frais de livraison
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    /**
     * Frais de service
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    /**
     * TVA
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    /**
     * Réduction appliquée
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * Code promo utilisé
     */
    @Column(length = 50)
    private String promoCode;

    /**
     * Pourboire pour le livreur
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tip = BigDecimal.ZERO;

    /**
     * Total final de la commande
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // ==================== ADRESSE DE LIVRAISON ====================

    /**
     * Adresse de livraison complète (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String deliveryAddressJson;

    /**
     * Latitude de livraison
     */
    @Column(precision = 10, scale = 8)
    private BigDecimal deliveryLatitude;

    /**
     * Longitude de livraison
     */
    @Column(precision = 11, scale = 8)
    private BigDecimal deliveryLongitude;

    /**
     * Instructions de livraison
     */
    @Column(length = 500)
    private String deliveryInstructions;

    // ==================== PAIEMENT ====================

    /**
     * Méthode de paiement
     */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /**
     * Statut du paiement
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * ID de la transaction de paiement (payment-service)
     */
    private Long paymentId;

    // ==================== TEMPS ====================

    /**
     * Date/heure de la commande
     */
    @Column(nullable = false)
    private LocalDateTime orderTime;

    /**
     * Heure de livraison estimée
     */
    private LocalDateTime estimatedDeliveryTime;

    /**
     * Max des temps de préparation par article (minutes), figé à la création — correspond au plus long plat / goulot.
     */
    @Column
    private Integer suggestedPreparationMinutes;

    /**
     * Heure de livraison réelle
     */
    private LocalDateTime actualDeliveryTime;

    /**
     * Commande planifiée pour plus tard
     */
    @Builder.Default
    private Boolean isScheduled = false;

    /**
     * Heure de livraison souhaitée (si planifiée)
     */
    private LocalDateTime scheduledDeliveryTime;

    // ==================== NOTES ====================

    /**
     * Notes du client
     */
    @Column(length = 500)
    private String customerNotes;

    /**
     * Notes internes (admin/support)
     */
    @Column(length = 500)
    private String internalNotes;

    /**
     * Raison de l'annulation (si annulée)
     */
    @Column(length = 500)
    private String cancellationReason;

    /**
     * Qui a annulé (CUSTOMER, PARTNER, COURIER, ADMIN)
     */
    @Column(length = 50)
    private String cancelledBy;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==================== RELATIONS ====================

    /**
     * Articles de la commande
     */
    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Historique des changements de statut
     */
    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    // ==================== ENUMS ====================

    public enum OrderType {
        DELIVERY,
        PICKUP
    }

    public enum PaymentMethod {
        CASH,
        CARD,
        WALLET,
        CARD_ON_DELIVERY
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Générer le numéro de commande
     */
    @PrePersist
    public void generateOrderNumber() {
        if (this.orderNumber == null) {
            String year = String.valueOf(java.time.Year.now().getValue());
            String random = String.format("%05d", (int) (Math.random() * 100000));
            this.orderNumber = "ORD-" + year + "-" + random;
        }
        if (this.orderTime == null) {
            this.orderTime = LocalDateTime.now();
        }
    }

    /**
     * Calculer le total de la commande
     */
    public void calculateTotal() {
        this.total = subtotal
                .add(deliveryFee)
                .add(serviceFee)
                .add(tax)
                .add(tip)
                .subtract(discount);
    }

    /**
     * Vérifier si la commande peut être annulée
     */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.CONFIRMED ||
               status == OrderStatus.PREPARING;
    }

    /**
     * Vérifier si la commande est terminée
     */
    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED || 
               status == OrderStatus.CANCELLED;
    }

    /**
     * Calculer le temps de livraison (en minutes)
     */
    public Integer getDeliveryTimeMinutes() {
        if (actualDeliveryTime != null && orderTime != null) {
            return (int) java.time.Duration.between(orderTime, actualDeliveryTime).toMinutes();
        }
        return null;
    }
}
