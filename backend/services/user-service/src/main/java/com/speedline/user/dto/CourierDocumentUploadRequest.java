package com.speedline.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour uploader un document de livreur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête d'upload de document pour un livreur")
public class CourierDocumentUploadRequest {

    /**
     * Type de document à uploader
     */
    @Schema(description = "Type de document", example = "CIN_FRONT", 
            allowableValues = {"CIN_FRONT", "CIN_BACK", "LICENSE", "INSURANCE", "VEHICLE", "PROFILE_PHOTO"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Le type de document est obligatoire")
    private DocumentType documentType;

    /**
     * URL du document uploadé (après upload sur le storage)
     */
    @Schema(description = "URL du document uploadé", 
            example = "https://storage.speedline.tn/documents/courier-123/cin.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "L'URL du document est obligatoire")
    @Size(max = 500, message = "L'URL ne doit pas dépasser 500 caractères")
    private String documentUrl;

    /**
     * Numéro du document (optionnel, ex: numéro CIN, numéro permis)
     */
    @Schema(description = "Numéro du document", example = "12345678")
    @Size(max = 100, message = "Le numéro du document ne doit pas dépasser 100 caractères")
    private String documentNumber;

    /**
     * Date d'expiration du document (format: YYYY-MM-DD)
     */
    @Schema(description = "Date d'expiration (format ISO)", example = "2027-12-31")
    private String expiryDate;

    /**
     * Types de documents acceptés
     */
    public enum DocumentType {
        /**
         * Carte d'identité nationale - Face avant
         */
        CIN_FRONT,
        
        /**
         * Carte d'identité nationale - Face arrière
         */
        CIN_BACK,
        
        /**
         * Permis de conduire
         */
        LICENSE,
        
        /**
         * Attestation d'assurance
         */
        INSURANCE,
        
        /**
         * Documents liés au véhicule (carte grise, etc.)
         */
        VEHICLE,
        
        /**
         * Photo de profil du livreur
         */
        PROFILE_PHOTO
    }
}
