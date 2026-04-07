package com.speedline.order.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSnapshot {

    private Long id;
    private Long partnerId;

    private String name;
    private String description;
    private String imageUrl;

    private BigDecimal price;
    private Boolean isAvailable;
    private Integer preparationTimeMin;
}
