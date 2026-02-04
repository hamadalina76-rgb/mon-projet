package com.speedline.delivery.dto;

import com.speedline.delivery.domain.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour TrackingPoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingPointDTO {

    private Long id;
    private Long deliveryId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;
    private BigDecimal speed;
    private BigDecimal bearing;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime timestamp;
}
