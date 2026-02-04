package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour OptionValue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionValueDTO {

    private Long id;
    private Long optionId;
    private String name;
    private String description;
    private BigDecimal priceModifier;
    private Boolean isAvailable;
    private Boolean isDefault;
    private Integer displayOrder;
    private String displayText;
}
