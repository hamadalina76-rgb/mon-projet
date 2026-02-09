package com.speedline.user.dto;

import com.speedline.user.domain.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Adresse de livraison d'un client")
public class AddressDTO {

    @Schema(description = "ID unique de l'adresse", example = "1")
    private Long id;

    @Schema(description = "ID du client propriétaire", example = "123")
    private Long customerId;

    @Schema(description = "ID de l'utilisateur (auth-service)", example = "456")
    private Long userId;

    // Type et label
    @Schema(description = "Type d'adresse", example = "HOME")
    private AddressType type;

    @Schema(description = "Label personnalisé", example = "Chez maman")
    private String label;

    // Adresse complète
    @Schema(description = "Numéro et nom de rue", example = "123 Avenue Habib Bourguiba")
    private String street;

    @Schema(description = "Nom ou numéro du bâtiment", example = "Résidence Les Jardins")
    private String building;

    @Schema(description = "Étage", example = "3")
    private String floor;

    @Schema(description = "Numéro d'appartement", example = "B12")
    private String apartment;

    @Schema(description = "Code d'accès à l'immeuble", example = "1234")
    private String accessCode;

    @Schema(description = "Ville", example = "Tunis")
    private String city;

    @Schema(description = "Code postal", example = "1000")
    private String postalCode;

    @Schema(description = "Gouvernorat/Région", example = "Tunis")
    private String state;

    @Schema(description = "Pays", example = "Tunisie")
    private String country;

    // Coordonnées GPS
    @Schema(description = "Latitude GPS", example = "36.8065", minimum = "-90", maximum = "90")
    private BigDecimal latitude;

    @Schema(description = "Longitude GPS", example = "10.1815", minimum = "-180", maximum = "180")
    private BigDecimal longitude;

    @Schema(description = "Adresse formatée complète", example = "123 Avenue Habib Bourguiba, Résidence Les Jardins, Étage 3, Apt B12, Tunis 1000, Tunisie")
    private String formattedAddress;

    // Instructions
    @Schema(description = "Instructions de livraison", example = "Sonner 2 fois, laisser devant la porte")
    private String deliveryInstructions;

    @Schema(description = "Point de repère", example = "En face de la pharmacie")
    private String landmark;

    @Schema(description = "Numéro de téléphone pour cette adresse", example = "+216 98 123 456")
    private String contactPhone;

    @Schema(description = "Nom du contact", example = "Mohamed Ali")
    private String contactName;

    // Flags
    @Schema(description = "Adresse par défaut", example = "true")
    private Boolean isDefault;

    @Schema(description = "Adresse vérifiée (GPS confirmé)", example = "false")
    private Boolean isVerified;

    // Timestamps
    @Schema(description = "Date de création")
    private LocalDateTime createdAt;

    @Schema(description = "Dernière utilisation")
    private LocalDateTime lastUsedAt;
}
