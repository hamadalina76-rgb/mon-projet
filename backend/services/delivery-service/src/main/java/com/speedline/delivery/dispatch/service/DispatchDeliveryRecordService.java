package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.contract.model.Assignment;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Crée l'enregistrement {@link com.speedline.delivery.domain.Delivery} en base à partir d'une affectation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchDeliveryRecordService {

    private final DeliveryService deliveryService;

    public void createFromAssignment(PendingOrder order, Assignment assignment) {
        if (order == null || order.getId() == null || assignment == null
                || assignment.getCourierId() == null) {
            return;
        }
        try {
            deliveryService.createDelivery(
                    order.getId(),
                    firstNonBlank(order.getOrderNumber(), String.valueOf(order.getId())),
                    order.getCustomerName(),
                    order.getCustomerPhone(),
                    order.getPartnerName(),
                    toBd(order.getPartnerLat()),
                    toBd(order.getPartnerLon()),
                    order.getPickupAddress(),
                    toBd(order.getCustomerLat()),
                    toBd(order.getCustomerLon()),
                    order.getDropoffAddress(),
                    order.getDeliveryInstructions(),
                    order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO,
                    assignment.getCourierId(),
                    assignment.getBundleId(),
                    assignment.getEtaPickupMin(),
                    assignment.getEtaDeliveryMin());
        } catch (Exception ex) {
            log.warn("Could not create delivery record for orderId={}: {}", order.getId(), ex.getMessage());
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static BigDecimal toBd(Double v) {
        return v == null ? null : BigDecimal.valueOf(v);
    }
}
