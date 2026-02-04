package com.speedline.user.dto;

import com.speedline.user.domain.AddressType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour mettre à jour une adresse existante
 * Tous les champs sont optionnels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressUpdateRequest {

    /**
     * Type d'adresse (HOME, WORK, OTHER)
     */
    private AddressType type;

    /**
     * Label personnalisé
     */
    @Size(max = 100, message = "Le label ne doit pas dépasser 100 caractères")
    private String label;

    /**
     * Numéro et nom de rue
     */
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
     * Ville
     */
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
     * Pays
     */
    private String country;

    /**
     * Latitude GPS
     */
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private BigDecimal latitude;

    /**
     * Longitude GPS
     */
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
    private Boolean isDefault;
}
