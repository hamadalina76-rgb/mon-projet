package com.speedline.partner.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Corps de requête PATCH /partners/{id}/menu/products/{productId}/stock.
 */
@Data
public class UpdateStockRequest {

    @NotNull(message = "La quantité est obligatoire")
    @Min(0)
    private Integer quantity;

    @Min(0)
    private Integer lowStockThreshold;

    private Boolean isTrackingEnabled;
}
