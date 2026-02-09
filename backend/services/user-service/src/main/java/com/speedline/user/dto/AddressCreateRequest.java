package com.speedline.user.dto;

import com.speedline.user.domain.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour créer une nouvelle adresse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de création d'une nouvelle adresse")
public class AddressCreateRequest {

    /**
     * Type d'adresse (HOME, WORK, OTHER)
     */
    @Schema(description = "Type d'adresse", example = "HOME", defaultValue = "HOME")
    @Builder.Default
    private AddressType type = AddressType.HOME;

    /**
     * Label personnalisé
     */
    @Schema(description = "Label personnalisé", example = "Chez maman", maxLength = 100)
    @Size(max = 100, message = "Le label ne doit pas dépasser 100 caractères")
    private String label;

    /**
     * Numéro et nom de rue (obligatoire)
     */
    @Schema(description = "Numéro et nom de rue", example = "123 Avenue Habib Bourguiba", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La rue est obligatoire")
    @Size(max = 255, message = "La rue ne doit pas dépasser 255 caractères")
    private String street;

    /**
     * Nom ou numéro du bâtiment
     */
    @Size(max = 100, message = "Le bâtiment ne doit pas dépasser 100 caractères")
    private String building;

    /**
     * Étage
     */
    @Size(max = 50, message = "L'étage ne doit pas dépasser 50 caractères")
    private String floor;

    /**
     * Numéro d'appartement
     */
    @Size(max = 50, message = "L'appartement ne doit pas dépasser 50 caractères")
    private String apartment;

    /**
     * Code d'accès à l'immeuble
     */
    @Size(max = 50, message = "Le code d'accès ne doit pas dépasser 50 caractères")
    private String accessCode;

    /**
     * Ville (obligatoire)
     */
    @Schema(description = "Ville", example = "Tunis", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
    private String city;

    /**
     * Code postal
     */
    @Size(max = 20, message = "Le code postal ne doit pas dépasser 20 caractères")
    private String postalCode;

    /**
     * Gouvernorat/Région
     */
    @Size(max = 100, message = "La région ne doit pas dépasser 100 caractères")
    private String state;

    /**
     * Pays (par défaut: Tunisie)
     */
    @Schema(description = "Pays", example = "Tunisie", defaultValue = "Tunisie")
    @Builder.Default
    private String country = "Tunisie";

    /**
     * Latitude GPS
     */
    @Schema(description = "Latitude GPS", example = "36.8065", minimum = "-90", maximum = "90")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private BigDecimal latitude;

    /**
     * Longitude GPS
     */
    @Schema(description = "Longitude GPS", example = "10.1815", minimum = "-180", maximum = "180")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private BigDecimal longitude;

    /**
     * ID de lieu Google/Mapbox
     */
    private String placeId;

    /**
     * Instructions de livraison
     */
    @Size(max = 500, message = "Les instructions ne doivent pas dépasser 500 caractères")
    private String deliveryInstructions;

    /**
     * Point de repère
     */
    @Size(max = 255, message = "Le point de repère ne doit pas dépasser 255 caractères")
    private String landmark;

    /**
     * Numéro de téléphone pour cette adresse
     */
    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String contactPhone;

    /**
     * Nom du contact
     */
    @Size(max = 100, message = "Le nom du contact ne doit pas dépasser 100 caractères")
    private String contactName;

    /**
     * Définir comme adresse par défaut
     */
    @Schema(description = "Définir comme adresse par défaut", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean isDefault = false;
}
