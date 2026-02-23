package com.speedline.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour un partenaire dans une zone
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerInZoneDTO {
    private Long id;
    private String businessName;
    private String address;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive;
    private Boolean acceptsOrders;
}
