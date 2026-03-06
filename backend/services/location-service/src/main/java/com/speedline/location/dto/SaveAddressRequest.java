package com.speedline.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requête pour sauvegarder l'adresse confirmée par le client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Adresse confirmée par le client (GPS ou saisie manuelle)")
public class SaveAddressRequest {

    @Schema(description = "ID utilisateur (JWT subject ou 'anonymous')", example = "user-uuid-123")
    private String userId;

    @Schema(description = "ID numérique du client (user-service customers.id)", example = "42")
    private Long customerId;

    @Schema(description = "Adresse complète formatée", example = "Ave. Hedi Chaker, Sfax 3000, Tunisie")
    private String formattedAddress;

    @Schema(description = "Rue", example = "Avenue Hedi Chaker")
    private String street;

    @Schema(description = "Ville", example = "Sfax")
    private String city;

    @Schema(description = "Gouvernorat / Région", example = "Sfax")
    private String state;

    @Schema(description = "Code postal", example = "3000")
    private String postalCode;

    @Schema(description = "Pays", example = "Tunisie")
    private String country;

    @Schema(description = "Latitude GPS", example = "34.7398")
    private BigDecimal latitude;

    @Schema(description = "Longitude GPS", example = "10.7600")
    private BigDecimal longitude;

    @Schema(description = "Type d'adresse: HOME / WORK / OTHER", example = "HOME")
    private String addressType;

    @Schema(description = "Label personnalisé (si type=OTHER)", example = "Bureau parent")
    private String customLabel;

    @Schema(description = "Définir comme adresse par défaut", example = "true")
    private Boolean isDefault;
}
