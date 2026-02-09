package com.speedline.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Statistiques détaillées d'un livreur")
public class CourierStatisticsDTO {

    @Schema(description = "ID du livreur", example = "1")
    private Long courierId;
    
    // Statistiques de livraison
    @Schema(description = "Nombre total de livraisons effectuées", example = "523")
    private Integer totalDeliveries;

    @Schema(description = "Nombre de livraisons réussies", example = "512")
    private Integer successfulDeliveries;

    @Schema(description = "Nombre de livraisons annulées", example = "11")
    private Integer cancelledDeliveries;

    @Schema(description = "Taux de réussite en pourcentage (0-100)", example = "97.9")
    private Double successRate;

    @Schema(description = "Temps moyen de livraison en minutes", example = "28")
    private Integer averageDeliveryTime;
    
    // Notes
    @Schema(description = "Note moyenne (1.0-5.0)", example = "4.8")
    private BigDecimal rating;

    @Schema(description = "Nombre total d'évaluations reçues", example = "156")
    private Integer totalRatings;

    @Schema(description = "Nombre d'évaluations 5 étoiles", example = "98")
    private Integer fiveStarRatings;

    @Schema(description = "Nombre d'évaluations 4 étoiles", example = "42")
    private Integer fourStarRatings;

    @Schema(description = "Nombre d'évaluations 3 étoiles", example = "12")
    private Integer threeStarRatings;

    @Schema(description = "Nombre d'évaluations 2 étoiles", example = "3")
    private Integer twoStarRatings;

    @Schema(description = "Nombre d'évaluations 1 étoile", example = "1")
    private Integer oneStarRatings;
    
    // Finances
    @Schema(description = "Gains totaux depuis l'inscription en MAD", example = "15230.50")
    private BigDecimal totalEarnings;

    @Schema(description = "Gains de la semaine en cours en MAD", example = "1250.00")
    private BigDecimal weeklyEarnings;

    @Schema(description = "Gains du mois en cours en MAD", example = "4500.00")
    private BigDecimal monthlyEarnings;

    @Schema(description = "Solde disponible pour retrait en MAD", example = "850.00")
    private BigDecimal availableBalance;
    
    // Distance
    @Schema(description = "Distance totale parcourue en km", example = "2345.6")
    private BigDecimal totalDistanceTravelled;

    @Schema(description = "Distance parcourue cette semaine en km", example = "156.8")
    private BigDecimal weeklyDistanceTravelled;
    
    // Temps
    @Schema(description = "Heures totales en ligne", example = "856")
    private Integer totalOnlineHours;

    @Schema(description = "Heures en ligne cette semaine", example = "42")
    private Integer weeklyOnlineHours;
}
