package com.speedline.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Corps de requête PATCH /products/{productId}/availability.
 * TC-13 : toggle isAvailable=false → produit non commandable côté client.
 */
@Data
public class UpdateAvailabilityRequest {

    @NotNull(message = "Le champ isAvailable est obligatoire")
    private Boolean isAvailable;
}
