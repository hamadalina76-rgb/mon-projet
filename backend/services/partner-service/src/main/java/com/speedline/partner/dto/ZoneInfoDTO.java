package com.speedline.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO représentant une zone (données venant du location-service) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneInfoDTO {
    private Long id;
    private String name;
    private String description;
    private String city;
    private String type;
    private String boundaryJson;
    private BigDecimal deliveryFee;
    private Integer minDeliveryTime;
    private Integer maxDeliveryTime;
    private Boolean isActive;
    private Integer radiusKm;
    private LocalDateTime assignedAt;
}
