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
public class AddressSnapshot {
    private Long id;
    private String street;
    private String city;
    private String formattedAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String deliveryInstructions;
}
