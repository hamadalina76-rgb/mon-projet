package com.speedline.order.event;

import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event publié quand une commande est créée
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private Long orderId;
    private String orderNumber;

    private Long customerId;
    private Long partnerId;

    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal serviceFee;
    private BigDecimal discount;
    private BigDecimal total;

    private Order.PaymentMethod paymentMethod;
    private Order.PaymentStatus paymentStatus;
    private OrderStatus status;

    private Integer itemCount;
    private LocalDateTime createdAt;
}
