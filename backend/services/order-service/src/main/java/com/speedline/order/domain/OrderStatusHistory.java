package com.speedline.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité OrderStatusHistory - Historique des changements de statut
 * Permet le tracking complet du cycle de vie d'une commande
 */
@Entity
@Table(name = "order_status_history", indexes = {
    @Index(name = "idx_status_history_order", columnList = "orderId"),
    @Index(name = "idx_status_history_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers la commande
     */
    @Column(nullable = false)
    private Long orderId;

    /**
     * Nouveau statut
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /**
     * Statut précédent (pour référence)
     */
    @Enumerated(EnumType.STRING)
    private OrderStatus previousStatus;

    /**
     * Description du changement
     */
    @Column(length = 500)
    private String description;

    /**
     * Notes additionnelles
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Qui a effectué le changement
     * Format: "SYSTEM", "CUSTOMER:123", "PARTNER:456", "COURIER:789", "ADMIN:000"
     */
    @Column(length = 100)
    private String updatedBy;

    /**
     * Type de l'acteur (SYSTEM, CUSTOMER, PARTNER, COURIER, ADMIN)
     */
    @Column(length = 50)
    private String actorType;

    /**
     * ID de l'acteur (si applicable)
     */
    private Long actorId;

    /**
     * Localisation au moment du changement (pour COURIER)
     */
    @Column(length = 100)
    private String location;

    /**
     * Date/heure du changement
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // ==================== MÉTHODES STATIQUES UTILITAIRES ====================

    /**
     * Créer une entrée d'historique pour un changement système
     */
    public static OrderStatusHistory systemChange(Long orderId, OrderStatus previousStatus, 
                                                   OrderStatus newStatus, String description) {
        return OrderStatusHistory.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .status(newStatus)
                .description(description)
                .updatedBy("SYSTEM")
                .actorType("SYSTEM")
                .build();
    }

    /**
     * Créer une entrée d'historique pour un changement par le client
     */
    public static OrderStatusHistory customerChange(Long orderId, Long customerId,
                                                     OrderStatus previousStatus, 
                                                     OrderStatus newStatus, String description) {
        return OrderStatusHistory.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .status(newStatus)
                .description(description)
                .updatedBy("CUSTOMER:" + customerId)
                .actorType("CUSTOMER")
                .actorId(customerId)
                .build();
    }

    /**
     * Créer une entrée d'historique pour un changement par le partenaire
     */
    public static OrderStatusHistory partnerChange(Long orderId, Long partnerId,
                                                    OrderStatus previousStatus, 
                                                    OrderStatus newStatus, String description) {
        return OrderStatusHistory.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .status(newStatus)
                .description(description)
                .updatedBy("PARTNER:" + partnerId)
                .actorType("PARTNER")
                .actorId(partnerId)
                .build();
    }

    /**
     * Créer une entrée d'historique pour un changement par le livreur
     */
    public static OrderStatusHistory courierChange(Long orderId, Long courierId,
                                                    OrderStatus previousStatus, 
                                                    OrderStatus newStatus, String description,
                                                    String location) {
        return OrderStatusHistory.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .status(newStatus)
                .description(description)
                .updatedBy("COURIER:" + courierId)
                .actorType("COURIER")
                .actorId(courierId)
                .location(location)
                .build();
    }

    /**
     * Créer une entrée d'historique pour un changement par un admin
     */
    public static OrderStatusHistory adminChange(Long orderId, Long adminId,
                                                  OrderStatus previousStatus, 
                                                  OrderStatus newStatus, String description,
                                                  String notes) {
        return OrderStatusHistory.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .status(newStatus)
                .description(description)
                .notes(notes)
                .updatedBy("ADMIN:" + adminId)
                .actorType("ADMIN")
                .actorId(adminId)
                .build();
    }
}
