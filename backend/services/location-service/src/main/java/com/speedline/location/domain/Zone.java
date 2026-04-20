package com.speedline.location.domain;

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
 * Entité Zone - Zones de livraison.
 * Table : adm_zones (lowercase, see V12 migration).
 */
@Entity
@Table(name = "adm_zones", indexes = {
    @Index(name = "idx_adm_zones_name", columnList = "name"),
    @Index(name = "idx_adm_zones_type", columnList = "type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Ville de la zone
     */
    @Column(length = 100)
    private String city;

    /**
     * Type de zone
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ZoneType type;

    /**
     * Polygone de la zone (JSON array de coordonnées)
     * Format: [[lat1,lon1],[lat2,lon2],...]
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String boundaryJson;

    /**
     * Frais de livraison pour cette zone
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    /**
     * Frais de service pour cette zone
     */
    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    /**
     * Temps de livraison minimum estimé (minutes)
     */
    private Integer minDeliveryTime;

    /**
     * Temps de livraison maximum estimé (minutes)
     */
    private Integer maxDeliveryTime;

    /**
     * Rayon de livraison approximatif pour cette zone (en kilomètres).
     * Champ optionnel utilisé principalement pour l'affichage et la configuration.
     */
    @Column(name = "radius_km")
    private Integer radiusKm;

    /**
     * Nombre minimum de livreurs internes actifs requis dans la zone.
     */
    @Column(name = "min_active_internal_couriers")
    private Integer minActiveInternalCouriers;

    /**
     * Capacité maximale de commandes simultanées dans la zone.
     */
    @Column(name = "max_simultaneous_orders")
    private Integer maxSimultaneousOrders;

    /**
     * Rayon d'extension inter-zones (en kilomètres).
     */
    @Column(name = "inter_zone_extension_radius_km")
    private Integer interZoneExtensionRadiusKm;

    /**
     * Délai maximal avant ré-affectation inter-zone (en minutes).
     */
    @Column(name = "max_inter_zone_reassignment_delay_minutes")
    private Integer maxInterZoneReassignmentDelayMinutes;

    /**
     * Affectations des livreurs internes avec jours et horaires.
     */
    @Builder.Default
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ZoneInternalCourierAssignment> internalCourierAssignments = new java.util.ArrayList<>();

    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ZoneType {
        DELIVERY,
        RESTRICTED,
        PREMIUM,
        EXPRESS
    }
}
