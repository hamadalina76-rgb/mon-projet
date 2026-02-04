package com.speedline.delivery.dto;

import com.speedline.delivery.domain.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour Delivery
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDTO {

    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long courierId;
    
    // Informations livreur
    private String courierName;
    private String courierPhone;
    
    // Informations client
    private String customerName;
    private String customerPhone;
    
    // Informations partenaire
    private String partnerName;
    
    // Statut
    private DeliveryStatus status;
    private String statusLabel;
    
    // Localisation pickup
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private String pickupAddress;
    
    // Localisation dropoff
    private BigDecimal dropoffLatitude;
    private BigDecimal dropoffLongitude;
    private String dropoffAddress;
    private String deliveryInstructions;
    
    // Distance et durée
    private BigDecimal estimatedDistance;
    private BigDecimal actualDistance;
    private Integer estimatedDuration;
    private Integer actualDuration;
    
    // Temps
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime estimatedArrival;
    
    // Preuve de livraison
    private String proofOfDeliveryImage;
    private String deliveryCode;
    
    // Gains
    private BigDecimal deliveryFee;
    private BigDecimal tip;
    private BigDecimal courierEarnings;
    
    // Position actuelle du livreur
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private LocalDateTime lastLocationUpdate;
    
    // Tracking points (optionnel, pour tracking détaillé)
    private List<TrackingPointDTO> trackingPoints;
}
