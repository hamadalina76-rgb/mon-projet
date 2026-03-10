package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO pour l'affichage du stock d'un produit.
 * stockStatus: IN_STOCK, LOW_STOCK, OUT_OF_STOCK.
 */
@Data
@Builder
public class ProductStockDTO {

    private Long productId;
    private String productName;
    private String productImageUrl;
    private String categoryName;
    private Integer quantity;
    private Integer lowStockThreshold;
    private Boolean isTrackingEnabled;
    private Boolean isAvailable;
    /** IN_STOCK, LOW_STOCK, OUT_OF_STOCK */
    private String stockStatus;
    private LocalDateTime updatedAt;
}
