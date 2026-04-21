package com.speedline.delivery.compensation;

import com.speedline.delivery.client.OrderServiceClient;
import com.speedline.delivery.client.PaymentServiceClient;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.metrics.DispatchMetrics;
import com.speedline.delivery.domain.Delivery;
import com.speedline.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LateDeliveryCompensationService {

    private final DispatchProperties dispatchProperties;
    private final DeliveryRepository deliveryRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final DispatchMetrics dispatchMetrics;

    public void evaluateAndCompensate(Delivery delivery) {
        if (delivery == null || delivery.getBundleId() == null) {
            return;
        }
        if (!dispatchProperties.getBundling().getCompensation().isEnabled()) {
            return;
        }
        if (delivery.getDeliveredAt() == null || delivery.getPromisedDeliveryAt() == null) {
            return;
        }
        if (delivery.getCompensationIssuedAt() != null) {
            return;
        }

        Duration lateness = Duration.between(delivery.getPromisedDeliveryAt(), delivery.getDeliveredAt());
        if (lateness.toMinutes() <= dispatchProperties.getBundling().getCompensation().getThresholdMinutes()) {
            return;
        }

        BigDecimal orderTotal = fetchOrderTotal(delivery.getOrderId());
        if (orderTotal == null) {
            return;
        }

        BigDecimal amount = orderTotal
                .multiply(BigDecimal.valueOf(dispatchProperties.getBundling().getCompensation().getPercentOfTotal()))
                .setScale(2, RoundingMode.HALF_UP);

        try {
            Long customerId = resolveCustomerId(delivery.getOrderId());
            if (customerId == null) {
                return;
            }

            paymentServiceClient.creditCustomer(customerId,
                    new PaymentServiceClient.CreditRequest(
                            amount,
                            dispatchProperties.getBundling().getCompensation().getCurrency(),
                            "delivery:" + delivery.getId(),
                            "Bundle late delivery compensation"));

            delivery.setCompensationIssuedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            dispatchMetrics.recordBundleCompensationIssued(0L);
        } catch (Exception ex) {
            log.warn("compensation failed deliveryId={} reason={}", delivery.getId(), ex.getMessage());
            dispatchMetrics.recordBundleCompensationFailed();
        }
    }

    private BigDecimal fetchOrderTotal(Long orderId) {
        try {
            Map<String, Object> payload = orderServiceClient.getOrderById(orderId);
            if (payload == null) {
                return null;
            }
            Object total = payload.get("total");
            if (total == null) {
                total = payload.get("totalAmount");
            }
            if (total == null) {
                total = payload.get("amount");
            }
            return total == null ? null : new BigDecimal(String.valueOf(total));
        } catch (Exception ex) {
            log.warn("unable to fetch order total orderId={} reason={}", orderId, ex.getMessage());
            return null;
        }
    }

    private Long resolveCustomerId(Long orderId) {
        try {
            Map<String, Object> payload = orderServiceClient.getOrderById(orderId);
            if (payload == null) {
                return null;
            }
            Object customerId = payload.get("customerId");
            if (customerId == null) {
                return null;
            }
            return Long.parseLong(String.valueOf(customerId));
        } catch (Exception ex) {
            log.warn("unable to resolve customer id for orderId={} reason={}", orderId, ex.getMessage());
            return null;
        }
    }
}
