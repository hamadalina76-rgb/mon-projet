package com.speedline.user.dto;

import com.speedline.user.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour mettre à jour un profil livreur
 * Tous les champs sont optionnels - seuls les champs fournis sont mis à jour
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de mise à jour du profil livreur. Seuls les champs fournis sont mis à jour.")
public class CourierUpdateRequest {

    @Schema(description = "Type de véhicule", example = "MOTORCYCLE")
    private VehicleType vehicleType;

    @Size(max = 20, message = "Le numéro d'immatriculation ne doit pas dépasser 20 caractères")
    @Schema(description = "Numéro d'immatriculation du véhicule", example = "12345-A-67")
    private String vehicleNumber;

    @Size(max = 100, message = "Le modèle ne doit pas dépasser 100 caractères")
    @Schema(description = "Marque et modèle du véhicule", example = "Honda PCX 125")
    private String vehicleModel;

    @Size(max = 30, message = "La couleur ne doit pas dépasser 30 caractères")
    @Schema(description = "Couleur du véhicule", example = "Noir")
    private String vehicleColor;

    @Size(max = 100, message = "La zone de livraison ne doit pas dépasser 100 caractères")
    @Schema(description = "Zone de livraison préférée", example = "Casablanca Centre")
    private String preferredDeliveryZone;

    @Min(value = 1, message = "Le rayon minimum est de 1 km")
    @Max(value = 50, message = "Le rayon maximum est de 50 km")
    @Schema(description = "Rayon maximum de livraison en km", example = "15", minimum = "1", maximum = "50")
    private Integer maxDeliveryRadius;

    @Pattern(regexp = "^(?i)TN[0-9]{2}[0-9A-Z]{20}$", message = "Format IBAN tunisien invalide")
    @Schema(description = "IBAN pour les virements (format tunisien)", example = "TN5914207207100707129648")
    private String bankIban;

    @Size(max = 100, message = "Le nom du titulaire ne doit pas dépasser 100 caractères")
    @Schema(description = "Nom du titulaire du compte bancaire", example = "Ahmed Benali")
    private String bankAccountHolder;

    @Size(max = 50, message = "Le numéro d'identité ne doit pas dépasser 50 caractères")
    @Schema(description = "Numéro de la carte d'identité", example = "AB123456")
    private String identityNumber;

    @Schema(description = "URL de l'image recto de la carte d'identité")
    private String identityDocumentFrontImage;

    @Schema(description = "URL de l'image verso de la carte d'identité")
    private String identityDocumentBackImage;

    @Size(max = 50, message = "Le numéro de permis ne doit pas dépasser 50 caractères")
    @Schema(description = "Numéro du permis de conduire", example = "ABC-12345-6789")
    private String drivingLicenseNumber;

    @Schema(description = "URL de l'image du permis de conduire")
    private String drivingLicenseImage;

    @Schema(description = "Date d'expiration du permis de conduire", example = "2026-12-31")
    private LocalDate drivingLicenseExpiry;
}
