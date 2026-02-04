package com.speedline.user.dto;

import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.domain.Customer.VipLevel;
import com.speedline.user.domain.CustomerPreferences;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour Customer - Utilisé pour les réponses API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {

    private Long id;
    private Long userId;
    
    // Informations de base (depuis auth-service)
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePicture;
    
    // Données du profil client
    private BigDecimal walletBalance;
    private Integer loyaltyPoints;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private LocalDateTime lastOrderDate;
    
    // Statut et VIP
    private CustomerStatus status;
    private Boolean isVip;
    private VipLevel vipLevel;
    
    // Parrainage
    private String referralCode;
    private Integer successfulReferrals;
    
    // Préférences
    private CustomerPreferences preferences;
    
    // Adresses
    private List<AddressDTO> addresses;
    private AddressDTO defaultAddress;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Nom complet du client
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
