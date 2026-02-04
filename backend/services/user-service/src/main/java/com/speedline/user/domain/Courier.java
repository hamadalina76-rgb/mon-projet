package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Courier (Livreur)
 * Profil livreur lié à User dans auth-service via userId
 */
@Entity
@Table(name = "couriers", indexes = {
    @Index(name = "idx_courier_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_courier_status", columnList = "status"),
    @Index(name = "idx_courier_available", columnList = "isAvailable, isOnline")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur dans auth-service
     * PAS de FK - communication inter-service
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    // ==================== INFORMATIONS VÉHICULE ====================

    /**
     * Type de véhicule utilisé
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VehicleType vehicleType;

    /**
     * Numéro d'immatriculation du véhicule
     */
    @Column(length = 50)
    private String vehicleNumber;

    /**
     * Marque et modèle du véhicule
     */
    @Column(length = 100)
    private String vehicleModel;

    /**
     * Couleur du véhicule
     */
    @Column(length = 50)
    private String vehicleColor;

    // ==================== DOCUMENTS ====================

    /**
     * Numéro du permis de conduire
     */
    @Column(length = 100)
    private String drivingLicenseNumber;

    /**
     * URL de l'image du permis de conduire
     */
    @Column(length = 500)
    private String drivingLicenseImage;

    /**
     * Date d'expiration du permis
     */
    private LocalDateTime drivingLicenseExpiry;

    /**
     * URL de la photo d'identité
     */
    @Column(length = 500)
    private String identityDocumentImage;

    /**
     * URL de la photo de profil
     */
    @Column(length = 500)
    private String profilePhoto;

    /**
     * Documents validés par l'admin
     */
    @Builder.Default
    private Boolean documentsVerified = false;

    // ==================== LOCALISATION ====================

    /**
     * Latitude de la position actuelle
     */
    @Column(precision = 10, scale = 8)
    private BigDecimal currentLatitude;

    /**
     * Longitude de la position actuelle
     */
    @Column(precision = 11, scale = 8)
    private BigDecimal currentLongitude;

    /**
     * Dernière mise à jour de la position
     */
    private LocalDateTime lastLocationUpdate;

    /**
     * Zone de livraison préférée (nom de la zone)
     */
    @Column(length = 100)
    private String preferredDeliveryZone;

    /**
     * Rayon maximum de livraison (en mètres)
     */
    @Builder.Default
    private Integer maxDeliveryRadius = 10000;

    // ==================== STATUT ET DISPONIBILITÉ ====================

    /**
     * Statut du compte livreur
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CourierStatus status = CourierStatus.PENDING_APPROVAL;

    /**
     * Disponible pour accepter des livraisons
     */
    @Builder.Default
    private Boolean isAvailable = false;

    /**
     * Actuellement en ligne (application ouverte)
     */
    @Builder.Default
    private Boolean isOnline = false;

    /**
     * ID de la livraison en cours (null si aucune)
     */
    private Long currentDeliveryId;

    // ==================== STATISTIQUES ====================

    /**
     * Note moyenne (1-5)
     */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Nombre total d'évaluations reçues
     */
    @Builder.Default
    private Integer totalRatings = 0;

    /**
     * Nombre total de livraisons effectuées
     */
    @Builder.Default
    private Integer totalDeliveries = 0;

    /**
     * Nombre de livraisons réussies
     */
    @Builder.Default
    private Integer successfulDeliveries = 0;

    /**
     * Nombre de livraisons annulées
     */
    @Builder.Default
    private Integer cancelledDeliveries = 0;

    /**
     * Temps de livraison moyen (en minutes)
     */
    @Builder.Default
    private Integer averageDeliveryTime = 0;

    /**
     * Distance totale parcourue (en km)
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalDistanceTravelled = BigDecimal.ZERO;

    // ==================== FINANCES ====================

    /**
     * Gains totaux
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    /**
     * Gains de la semaine en cours
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal weeklyEarnings = BigDecimal.ZERO;

    /**
     * Solde disponible pour retrait
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * IBAN pour les virements
     */
    @Column(length = 50)
    private String bankIban;

    /**
     * Nom du titulaire du compte bancaire
     */
    @Column(length = 100)
    private String bankAccountHolder;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Dernière connexion
     */
    private LocalDateTime lastLoginAt;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Mettre à jour la position GPS
     */
    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
        this.lastLocationUpdate = LocalDateTime.now();
    }

    /**
     * Passer en ligne
     */
    public void goOnline() {
        this.isOnline = true;
        this.isAvailable = true;
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Passer hors ligne
     */
    public void goOffline() {
        this.isOnline = false;
        this.isAvailable = false;
    }

    /**
     * Commencer une livraison
     */
    public void startDelivery(Long deliveryId) {
        this.currentDeliveryId = deliveryId;
        this.isAvailable = false;
        this.status = CourierStatus.BUSY;
    }

    /**
     * Terminer une livraison
     */
    public void completeDelivery(BigDecimal earnings, BigDecimal distance) {
        this.currentDeliveryId = null;
        this.isAvailable = true;
        this.status = CourierStatus.AVAILABLE;
        this.totalDeliveries++;
        this.successfulDeliveries++;
        this.totalEarnings = this.totalEarnings.add(earnings);
        this.weeklyEarnings = this.weeklyEarnings.add(earnings);
        this.availableBalance = this.availableBalance.add(earnings);
        this.totalDistanceTravelled = this.totalDistanceTravelled.add(distance);
    }

    /**
     * Mettre à jour la note moyenne
     */
    public void updateRating(BigDecimal newRating) {
        BigDecimal totalScore = this.rating.multiply(BigDecimal.valueOf(this.totalRatings));
        this.totalRatings++;
        this.rating = totalScore.add(newRating).divide(BigDecimal.valueOf(this.totalRatings), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Taux de réussite des livraisons
     */
    public double getSuccessRate() {
        if (totalDeliveries == 0) return 0;
        return (double) successfulDeliveries / totalDeliveries * 100;
    }
}
