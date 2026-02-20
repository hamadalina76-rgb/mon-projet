package com.speedline.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO pour créer un profil partner initial
 * Utilisé par auth-service pour appeler partner-service lors de l'inscription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerRequest {
    
    /**
     * ID de l'utilisateur dans auth-service
     */
    private Long userId;
    
    /**
     * Email du partenaire
     */
    private String email;
    
    /**
     * Prénom du propriétaire
     */
    private String firstName;
    
    /**
     * Nom du propriétaire
     */
    private String lastName;
    
    /**
     * Numéro de téléphone
     */
    private String phoneNumber;
}
