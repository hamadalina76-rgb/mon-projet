package com.speedline.order.event;

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
public class OrderStatusChangedEvent {

    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private Long partnerId;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String actorType;
    private String description;
    private LocalDateTime timestamp;
}
