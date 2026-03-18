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
    /** Alias ticket: nom affiché (businessName) */
    private String name;
    private String brandName;
    private String slug;
    private PartnerType type;
    private String description;
    private String shortDescription;
    private String logo;
    /** Alias ticket: URL du logo */
    private String logoUrl;
    private String coverImage;
    /** Alias ticket: URL de la couverture */
    private String coverUrl;
    private String phoneNumber;
    private String email;
    
    // Informations légales
    private String legalStatus;
    private String tva;
    private String legalRepFirstName;
    private String legalRepLastName;
    private String position;
    
    // Informations bancaires
    private String accountHolderName;
    private String iban;
    private String bankName;
    private String currency;
    
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

    /** Type de commission (PERCENTAGE ou MARKUP) */
    private String commissionType;

    /** Taux de commission (ticket: commissionRate) */
    private BigDecimal commissionRate;
    
    // Paramètres de livraison
    private Integer preparationTime;
    private BigDecimal deliveryFee;
    private BigDecimal minimumOrder;
    private BigDecimal freeDeliveryThreshold;
    
    // Modes de paiement
    private Boolean acceptOnlinePayment;
    private Boolean acceptCashPayment;
    
    // Statistiques
    private BigDecimal rating;
    private Integer totalRatings;
    /** Alias ticket: nombre d'avis */
    private Integer reviewCount;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    
    // Documents
    private String kbisUrl;
    private String idCardUrl;
    private String insuranceUrl;
    private String ribUrl;
    private String photosJson;

    // Catégories et tags
    private List<Long> categoryIds;
    private List<String> tags;
    
    // Horaires (simplifié pour l'affichage)
    private String openingHoursDisplay;
    /** Alias ticket: horaires parsés (liste jour/plages) */
    private List<?> openingHours;
    private String scheduleExceptionsDisplay; // JSON des exceptions (jours fériés, fermetures)

    // Notes internes (admin)
    private String internalNotes;
    
    // Timestamps
    private LocalDateTime createdAt;
        private LocalDateTime updatedAt; // Added updatedAt field
    
    // Distance (calculée côté client ou service)
    private Double distanceKm;
    private Integer estimatedDeliveryTime;
}
