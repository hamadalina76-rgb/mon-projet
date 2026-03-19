package com.speedline.partner.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerFilterRequest {

    private BigDecimal lat;
    private BigDecimal lng;

    private Boolean isOpen;
    private String categoryId;
    private Double minRating;
    private Integer maxDeliveryTime;
    private Boolean freeDelivery;

    @Builder.Default
    private String sortBy = "distance";

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;
}
