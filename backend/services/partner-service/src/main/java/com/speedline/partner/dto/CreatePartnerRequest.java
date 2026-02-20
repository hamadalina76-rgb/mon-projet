package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO pour créer un profil partner initial (depuis auth-service)
 * Endpoint interne: POST /partners/internal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerRequest {
    
    /**
     * ID de l'utilisateur dans auth-service (obligatoire)
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
