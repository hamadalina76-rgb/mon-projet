package com.speedline.partner.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Corps de requête pour modifier un produit.
 * Tous les champs sont optionnels (patch sémantique).
 */
@Data
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
    private BigDecimal price;

    private Long categoryId;

    private String description;

    @Size(max = 500)
    private String imageUrl;

    private Boolean isAvailable;

    private Boolean isPopular;

    private Integer preparationTimeMin;

    private Integer position;

    @Size(max = 500)
    private String tags;
}
