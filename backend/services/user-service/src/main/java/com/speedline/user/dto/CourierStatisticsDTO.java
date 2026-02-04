package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les statistiques d'un livreur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierStatisticsDTO {

    private Long courierId;
    
    // Statistiques de livraison
    private Integer totalDeliveries;
    private Integer successfulDeliveries;
    private Integer cancelledDeliveries;
    private Double successRate;
    private Integer averageDeliveryTime;
    
    // Notes
    private BigDecimal rating;
    private Integer totalRatings;
    private Integer fiveStarRatings;
    private Integer fourStarRatings;
    private Integer threeStarRatings;
    private Integer twoStarRatings;
    private Integer oneStarRatings;
    
    // Finances
    private BigDecimal totalEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal monthlyEarnings;
    private BigDecimal availableBalance;
    
    // Distance
    private BigDecimal totalDistanceTravelled;
    private BigDecimal weeklyDistanceTravelled;
    
    // Temps
    private Integer totalOnlineHours;
    private Integer weeklyOnlineHours;
}
