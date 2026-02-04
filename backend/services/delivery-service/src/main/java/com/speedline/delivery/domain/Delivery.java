package com.speedline.delivery.domain;

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
 * Entité Delivery - Gestion des livraisons
 */
@Entity
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_delivery_order", columnList = "orderId", unique = true),
    @Index(name = "idx_delivery_courier", columnList = "courierId"),
    @Index(name = "idx_delivery_status", columnList = "status"),
    @Index(name = "idx_delivery_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers la commande (order-service)
     */
    @Column(nullable = false, unique = true)
    private Long orderId;

    /**
     * Numéro de commande (copie pour affichage)
     */
    @Column(length = 50)
    private String orderNumber;

    /**
     * Référence vers le livreur (user-service)
     */
    @Column(nullable = false)
    private Long courierId;

    // ==================== DONNÉES DÉNORMALISÉES ====================

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

    /**
     * Nom du client (copie)
     */
    @Column(length = 255)
    private String customerName;

    /**
     * Téléphone du client (copie)
     */
    @Column(length = 20)
    private String customerPhone;

    /**
     * Nom du partenaire (copie)
     */
    @Column(length = 255)
    private String partnerName;

    // ==================== STATUT ====================

    /**
     * Statut de la livraison
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    // ==================== LOCALISATION PICKUP ====================

    /**
     * Latitude du point de pickup (partenaire)
     */
    @Column(precision = 10, scale = 8)
    private BigDecimal pickupLatitude;

    /**
     * Longitude du point de pickup
     */
    @Column(precision = 11, scale = 8)
    private BigDecimal pickupLongitude;

    /**
     * Adresse de pickup (texte)
     */
    @Column(length = 500)
    private String pickupAddress;

    // ==================== LOCALISATION DROPOFF ====================

    /**
     * Latitude du point de dropoff (client)
     */
    @Column(precision = 10, scale = 8)
    private BigDecimal dropoffLatitude;

    /**
     * Longitude du point de dropoff
     */
    @Column(precision = 11, scale = 8)
    private BigDecimal dropoffLongitude;

    /**
     * Adresse de dropoff (texte)
     */
    @Column(length = 500)
    private String dropoffAddress;

    /**
     * Instructions de livraison
     */
    @Column(length = 500)
    private String deliveryInstructions;

    // ==================== DISTANCE ET DURÉE ====================

    /**
     * Distance totale estimée (en km)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedDistance;

    /**
     * Distance réellement parcourue (en km)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal actualDistance;

    /**
     * Durée estimée (en minutes)
     */
    private Integer estimatedDuration;

    /**
     * Durée réelle (en minutes)
     */
    private Integer actualDuration;

    // ==================== TIMESTAMPS ====================

    /**
     * Heure d'assignation au livreur
     */
    private LocalDateTime assignedAt;

    /**
     * Heure d'acceptation par le livreur
     */
    private LocalDateTime acceptedAt;

    /**
     * Heure d'arrivée au pickup
     */
    private LocalDateTime arrivedAtPickupAt;

    /**
     * Heure de récupération de la commande
     */
    private LocalDateTime pickedUpAt;

    /**
     * Heure de début de livraison
     */
    private LocalDateTime inTransitAt;

    /**
     * Heure d'arrivée au dropoff
     */
    private LocalDateTime arrivedAtDropoffAt;

    /**
     * Heure de livraison effective
     */
    private LocalDateTime deliveredAt;

    /**
     * Heure d'annulation (si applicable)
     */
    private LocalDateTime cancelledAt;

    // ==================== PREUVE DE LIVRAISON ====================

    /**
     * URL de la photo de preuve de livraison
     */
    @Column(length = 500)
    private String proofOfDeliveryImage;

    /**
     * Signature du client (URL de l'image)
     */
    @Column(length = 500)
    private String customerSignature;

    /**
     * Code de confirmation donné par le client
     */
    @Column(length = 10)
    private String deliveryCode;

    /**
     * Notes du livreur à la livraison
     */
    @Column(length = 500)
    private String deliveryNotes;

    // ==================== GAINS ====================

    /**
     * Frais de livraison
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    /**
     * Pourboire
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tip = BigDecimal.ZERO;

    /**
     * Gains totaux du livreur pour cette livraison
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal courierEarnings;

    // ==================== ANNULATION ====================

    /**
     * Raison de l'annulation (si applicable)
     */
    @Column(length = 500)
    private String cancellationReason;

    /**
     * Qui a annulé (COURIER, CUSTOMER, SYSTEM, ADMIN)
     */
    @Column(length = 50)
    private String cancelledBy;

    // ==================== TIMESTAMPS SYSTÈME ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ==================== RELATIONS ====================

    /**
     * Points de tracking GPS
     */
    @OneToMany(mappedBy = "deliveryId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TrackingPoint> trackingPoints = new ArrayList<>();

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Générer un code de livraison aléatoire
     */
    @PrePersist
    public void generateDeliveryCode() {
        if (this.deliveryCode == null) {
            this.deliveryCode = String.format("%04d", (int) (Math.random() * 10000));
        }
    }

    /**
     * Calculer la durée réelle de la livraison
     */
    public void calculateActualDuration() {
        if (assignedAt != null && deliveredAt != null) {
            this.actualDuration = (int) java.time.Duration.between(assignedAt, deliveredAt).toMinutes();
        }
    }

    /**
     * Vérifier si la livraison peut être annulée
     */
    public boolean isCancellable() {
        return status == DeliveryStatus.PENDING ||
               status == DeliveryStatus.ASSIGNED ||
               status == DeliveryStatus.ACCEPTED;
    }

    /**
     * Vérifier si la livraison est terminée
     */
    public boolean isCompleted() {
        return status == DeliveryStatus.DELIVERED ||
               status == DeliveryStatus.CANCELLED ||
               status == DeliveryStatus.FAILED;
    }

    /**
     * Vérifier si la livraison est en cours
     */
    public boolean isInProgress() {
        return status == DeliveryStatus.ACCEPTED ||
               status == DeliveryStatus.ARRIVED_AT_PICKUP ||
               status == DeliveryStatus.PICKED_UP ||
               status == DeliveryStatus.IN_TRANSIT ||
               status == DeliveryStatus.ARRIVED_AT_DROPOFF;
    }
}
