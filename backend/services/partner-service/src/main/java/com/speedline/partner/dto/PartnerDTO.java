package com.speedline.partner.dto;

import com.speedline.partner.domain.PartnerStatus;
import com.speedline.partner.domain.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour Partner - Utilisé pour les réponses API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDTO {

    private Long id;
    private Long userId;
    
    // Informations de base
    private String businessName;
    private String slug;
    private PartnerType type;
    private String description;
    private String logo;
    private String coverImage;
    private String phoneNumber;
    private String email;
    
    // Adresse
    private String address;
    private String city;
    private String postalCode;
    private String state;
    private String country;
    
    // Localisation GPS
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer deliveryRadius;
    
    // Statut
    private PartnerStatus status;
    private Boolean isActive;
    private Boolean acceptsOrders;
    private Boolean isVerified;
    private Boolean isPremium;
    private Boolean isFeatured;
    private Boolean isCurrentlyOpen;
    
    // Paramètres de livraison
    private Integer preparationTime;
    private BigDecimal deliveryFee;
    private BigDecimal minimumOrder;
    private BigDecimal freeDeliveryThreshold;
    
    // Statistiques
    private BigDecimal rating;
    private Integer totalRatings;
    private Integer totalOrders;
    
    // Catégories et tags
    private List<Long> categoryIds;
    private List<String> tags;
    
    // Horaires (simplifié pour l'affichage)
    private String openingHoursDisplay;
    
    // Timestamps
    private LocalDateTime createdAt;
    
    // Distance (calculée côté client ou service)
    private Double distanceKm;
    private Integer estimatedDeliveryTime;
}
