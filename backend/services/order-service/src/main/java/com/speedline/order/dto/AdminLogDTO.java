package com.speedline.order.dto;

import com.speedline.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private OrderStatus previousStatus;
    private String description;
    private String notes;
    private String actorType;
    private Long actorId;
    private String updatedBy;
    private LocalDateTime timestamp;
}
