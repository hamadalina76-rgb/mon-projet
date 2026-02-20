package com.speedline.partner.dto;

import com.speedline.partner.domain.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO pour compléter le profil partenaire (depuis frontend)
 * Endpoint public: PUT /partners/{id} ou POST /partners/complete-profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletePartnerProfileRequest {
    
    // ======== Business Information ========
    private String businessName;
    private String brandName;
    private PartnerType partnerType;
    private String description;
    
    // ======== Address ========
    private String address;
    private String city;
    private String postalCode;
    private String state;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // ======== Legal Information ========
    private String legalStatus;
    private String tva;
    private String legalRepFirstName;
    private String legalRepLastName;
    private String position;
    
    // ======== Bank Information ========
    private String accountHolderName;
    private String iban;
    private String bankName;
    private String currency;
    
    // ======== Operational Configuration ========
    private Integer preparationTime;
    private Boolean acceptOnlinePayment;
    private Boolean acceptCashPayment;
    private BigDecimal minimumOrder;
    private Boolean noMinimum;
    private String openingHoursJson; // JSON string
    
    // ======== Presentation ========
    private String shortDescription;
    private String fullDescription;
    private String tags; // Comma-separated string
    
    // ======== Terms ========
    private Boolean acceptTerms;
    private Boolean acceptPrivacy;
}
