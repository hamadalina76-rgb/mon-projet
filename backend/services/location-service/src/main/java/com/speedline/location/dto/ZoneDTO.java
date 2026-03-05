package com.speedline.location.dto;

import com.speedline.location.domain.Zone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO complet pour Zone avec toutes les informations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneDTO {
    private Long id;
    private String name;
    private String description;
    private String city;
    private Zone.ZoneType type;
    private String boundaryJson;
    private BigDecimal deliveryFee;
    private Integer minDeliveryTime;
    private Integer maxDeliveryTime;
    private Boolean isActive;
    /**
     * Rayon de livraison approximatif pour cette zone (en kilomètres).
     */
    private Integer radiusKm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Statistiques
    private Long partnersCount;
    private BigDecimal areaKm2;
    private Double[] center; // [latitude, longitude]
}
