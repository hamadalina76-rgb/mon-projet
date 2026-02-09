package com.speedline.user.dto;

import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.domain.Customer.VipLevel;
import com.speedline.user.domain.CustomerPreferences;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Profil complet d'un client")
public class CustomerDTO {

    @Schema(description = "ID unique du profil client", example = "1")
    private Long id;
    
    @Schema(description = "ID de l'utilisateur dans auth-service", example = "42")
    private Long userId;
    
    // Informations de base (depuis auth-service)
    @Schema(description = "Email du client", example = "client@example.com")
    private String email;
    
    @Schema(description = "Prénom du client", example = "Ahmed")
    private String firstName;
    
    @Schema(description = "Nom du client", example = "Ben Ali")
    private String lastName;
    
    @Schema(description = "Numéro de téléphone", example = "+216 55 123 456")
    private String phoneNumber;
    
    @Schema(description = "URL de la photo de profil")
    private String profilePicture;
    
    // Données du profil client
    @Schema(description = "Solde du wallet en TND", example = "150.50")
    private BigDecimal walletBalance;
    
    @Schema(description = "Nombre de points de fidélité", example = "2500")
    private Integer loyaltyPoints;
    
    @Schema(description = "Nombre total de commandes", example = "45")
    private Integer totalOrders;
    
    @Schema(description = "Montant total dépensé en TND", example = "1250.75")
    private BigDecimal totalSpent;
    
    @Schema(description = "Date de la dernière commande")
    private LocalDateTime lastOrderDate;
    
    // Statut et VIP
    @Schema(description = "Statut du compte", example = "ACTIVE")
    private CustomerStatus status;
    
    @Schema(description = "Indique si le client est VIP", example = "true")
    private Boolean isVip;
    
    @Schema(description = "Niveau VIP", example = "GOLD")
    private VipLevel vipLevel;
    
    // Parrainage
    @Schema(description = "Code de parrainage unique du client", example = "SPDABC12345")
    private String referralCode;
    
    @Schema(description = "Nombre de parrainages réussis", example = "5")
    private Integer successfulReferrals;
    
    // Préférences
    @Schema(description = "Préférences du client")
    private CustomerPreferences preferences;
    
    // Adresses
    @Schema(description = "Liste des adresses du client")
    private List<AddressDTO> addresses;
    
    @Schema(description = "Adresse par défaut")
    private AddressDTO defaultAddress;
    
    // Timestamps
    @Schema(description = "Date de création du profil")
    private LocalDateTime createdAt;
    
    @Schema(description = "Date de dernière mise à jour")
    private LocalDateTime updatedAt;

    /**
     * Nom complet du client
     */
    @Schema(description = "Nom complet du client", example = "Ahmed Ben Ali")
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
