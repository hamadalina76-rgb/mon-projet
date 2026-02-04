package com.speedline.user.dto;

import com.speedline.user.domain.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour Address - Utilisé pour les réponses API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private Long id;
    private Long customerId;
    private Long userId;
    
    // Type et label
    private AddressType type;
    private String label;
    
    // Adresse complète
    private String street;
    private String building;
    private String floor;
    private String apartment;
    private String accessCode;
    private String city;
    private String postalCode;
    private String state;
    private String country;
    
    // Coordonnées GPS
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String formattedAddress;
    
    // Instructions
    private String deliveryInstructions;
    private String landmark;
    private String contactPhone;
    private String contactName;
    
    // Flags
    private Boolean isDefault;
    private Boolean isVerified;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
