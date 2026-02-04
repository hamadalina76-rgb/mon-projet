package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour ProductAddon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAddonDTO {

    private Long id;
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    private Boolean isAvailable;
    private Integer maxQuantity;
    private String category;
    private Integer displayOrder;
    private Boolean isPopular;
}
