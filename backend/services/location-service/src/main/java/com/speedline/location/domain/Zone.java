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
 * Entité Zone - Zones de livraison
 */
@Entity
@Table(name = "zones", indexes = {
    @Index(name = "idx_zone_name", columnList = "name"),
    @Index(name = "idx_zone_type", columnList = "type")
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
     * Temps de livraison minimum estimé (minutes)
     */
    private Integer minDeliveryTime;

    /**
     * Temps de livraison maximum estimé (minutes)
     */
    private Integer maxDeliveryTime;

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
