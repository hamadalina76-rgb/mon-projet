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

    /**
     * Propriétaire du compte partenaire (user-service) — pour persister la notification
     * sous le bon {@code userId} (GET /notifications/{userId} dans le dashboard).
     */
    private Long partnerUserId;

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

    /**
     * Coordonnees de livraison — necessaires au dispatch "Jarvis" pour pre-assigner
     * un livreur des ORDER_CREATED sans aller-retour order-service.
     */
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
}
