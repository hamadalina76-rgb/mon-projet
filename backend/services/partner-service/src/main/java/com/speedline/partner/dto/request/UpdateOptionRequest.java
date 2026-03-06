package com.speedline.partner.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Corps de requête pour modifier une option (OptionValue).
 * Tous les champs sont optionnels (patch sémantique).
 */
@Data
public class UpdateOptionRequest {

    @Size(max = 100)
    private String name;

    @DecimalMin(value = "0.00", message = "Le prix supplémentaire ne peut pas être négatif")
    private BigDecimal priceModifier;

    private Boolean isDefault;

    private Boolean isAvailable;

    private Integer position;
}
