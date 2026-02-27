package com.speedline.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la réponse de géocodage inverse (coordonnées → adresse)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse de géocodage inverse: adresse complète à partir de coordonnées GPS")
public class ReverseGeocodeResponse {

    @Schema(description = "Adresse complète formatée", example = "Ave. Hedi Chaker, Sfax 3000, Tunisie")
    private String formattedAddress;

    @Schema(description = "Numéro et nom de rue", example = "Avenue Hedi Chaker")
    private String street;

    @Schema(description = "Ville", example = "Sfax")
    private String city;

    @Schema(description = "Gouvernorat/Région", example = "Sfax")
    private String state;

    @Schema(description = "Code postal", example = "3000")
    private String postalCode;

    @Schema(description = "Pays", example = "Tunisie")
    private String country;

    @Schema(description = "Latitude confirmée", example = "36.8065")
    private BigDecimal latitude;

    @Schema(description = "Longitude confirmée", example = "10.1815")
    private BigDecimal longitude;

    @Schema(description = "ID Mapbox du lieu", example = "place.123456")
    private String placeId;

    @Schema(description = "Niveau de confiance (0.0 - 1.0)", example = "0.95")
    private Double confidence;
}
