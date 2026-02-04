package com.speedline.user.dto;

import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour Courier - Utilisé pour les réponses API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierDTO {

    private Long id;
    private Long userId;
    
    // Informations de base (depuis auth-service)
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePhoto;
    
    // Informations véhicule
    private VehicleType vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    private String vehicleColor;
    
    // Statut et disponibilité
    private CourierStatus status;
    private Boolean isAvailable;
    private Boolean isOnline;
    private Boolean documentsVerified;
    
    // Position actuelle
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private LocalDateTime lastLocationUpdate;
    
    // Statistiques
    private BigDecimal rating;
    private Integer totalRatings;
    private Integer totalDeliveries;
    private Integer successfulDeliveries;
    private Integer averageDeliveryTime;
    private Double successRate;
    
    // Finances (visible uniquement par le livreur)
    private BigDecimal totalEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal availableBalance;
    
    // Zone de livraison
    private String preferredDeliveryZone;
    private Integer maxDeliveryRadius;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    /**
     * Nom complet du livreur
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
