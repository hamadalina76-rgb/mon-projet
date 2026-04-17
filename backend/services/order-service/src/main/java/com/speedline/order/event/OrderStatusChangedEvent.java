package com.speedline.order.event;

import com.speedline.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event publie quand le statut d'une commande change.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {

    @Builder.Default
    private String eventType = "ORDER_STATUS_CHANGED";

    private Long orderId;
    private String orderNumber;

    private Long customerId;
    private Long partnerId;
    private Long partnerUserId;
    private OrderStatus previousStatus;
    private OrderStatus status;

    private OrderStatus newStatus;
    private String actorType;
    private Long actorId;

    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime updatedAt;
    private String description;
    private LocalDateTime timestamp;
}