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
public class ZoneSnapshot {
    private Long id;
    private String name;
    private BigDecimal deliveryFee;
    private BigDecimal serviceFee;
    private Boolean inZone;
}
