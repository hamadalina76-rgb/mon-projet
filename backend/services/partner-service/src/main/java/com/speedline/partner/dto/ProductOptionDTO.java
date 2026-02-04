package com.speedline.partner.dto;

import com.speedline.partner.domain.ProductOption.OptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour ProductOption
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDTO {

    private Long id;
    private Long productId;
    private String name;
    private String description;
    private OptionType type;
    private Boolean isRequired;
    private Integer minSelection;
    private Integer maxSelection;
    private Integer displayOrder;
    private List<OptionValueDTO> values;
}
