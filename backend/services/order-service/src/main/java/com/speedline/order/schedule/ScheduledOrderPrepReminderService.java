package com.speedline.order.schedule;

import com.speedline.order.client.PartnerServiceClient;
import com.speedline.order.client.dto.PartnerSnapshot;
import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderStatus;
import com.speedline.order.event.producer.OrderEventProducer;
import com.speedline.order.repository.OrderRepository;
import com.speedline.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envoie un rappel partenaire lorsque {@code maintenant} atteint
 * {@code scheduledDeliveryTime - (bufferMinutes + suggestedPreparationMinutes)}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledOrderPrepReminderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final PartnerServiceClient partnerServiceClient;
    private final OrderService orderService;

    @Value("${order.scheduled-prep-reminder.enabled:true}")
    private boolean enabled;

    @Value("${order.scheduled-prep-reminder.buffer-minutes:15}")
    private int bufferMinutes;

    @Transactional
    public void sendDueReminders(java.time.LocalDateTime now) {
        if (!enabled) {
            return;
        }

        final List<Order> due = orderRepository.findScheduledOrdersDueForPrepReminder(now, bufferMinutes);
        if (due.isEmpty()) {
            return;
        }

        log.info("Scheduled prep reminder: {} commande(s) éligible(s)", due.size());

        for (Order order : due) {
            try {
                final int prep = order.getSuggestedPreparationMinutes() != null
                        ? Math.max(1, order.getSuggestedPreparationMinutes())
                        : 15;

                Long partnerUserId = null;
                try {
                    final PartnerSnapshot partner = partnerServiceClient.getPartnerById(order.getPartnerId());
                    if (partner != null && partner.getUserId() != null) {
                        partnerUserId = partner.getUserId();
                    }
                } catch (Exception ex) {
                    log.warn("Impossible de résoudre partnerUserId pour rappel planifié partnerId={} : {}",
                            order.getPartnerId(), ex.getMessage());
                }

                final Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("eventType", "ORDER_SCHEDULED_PREP_REMINDER");
                payload.put("orderId", order.getId());
                payload.put("orderNumber", order.getOrderNumber());
                payload.put("partnerId", order.getPartnerId());
                if (partnerUserId != null) {
                    payload.put("partnerUserId", partnerUserId);
                }
                payload.put("customerId", order.getCustomerId());
                payload.put("scheduledDeliveryTime", order.getScheduledDeliveryTime().toString());
                payload.put("prepMinutes", prep);
                payload.put("bufferMinutes", bufferMinutes);
                payload.put("prepLeadMinutes", prep + bufferMinutes);
                payload.put("status", order.getStatus().name());
                payload.put("total", order.getTotal() != null ? order.getTotal().toPlainString() : "0");

                orderEventProducer.publishOrderPayload(payload);

                order.setScheduledPrepReminderSent(true);
                orderRepository.save(order);
            } catch (Exception ex) {
                log.error("Échec rappel préparation planifiée orderId={}", order.getId(), ex);
            }
        }
    }

    @Transactional
    public void autoStartPreparingWhenDue(java.time.LocalDateTime now) {
        if (!enabled) {
            return;
        }

        final List<Order> due = orderRepository.findScheduledOrdersDueForAutoPreparing(now);
        if (due.isEmpty()) {
            return;
        }

        log.info("Scheduled auto-preparing: {} commande(s) à démarrer", due.size());

        for (Order order : due) {
            try {
                if (order.getStatus() != OrderStatus.CONFIRMED) {
                    continue;
                }

                orderService.updateStatus(
                        order.getId(),
                        OrderStatus.PREPARING,
                        "SYSTEM",
                        null,
                        "Préparation démarrée automatiquement (commande planifiée)"
                );

                publishDueNowReminder(order);
            } catch (Exception ex) {
                log.error("Échec démarrage auto prépa orderId={}", order.getId(), ex);
            }
        }
    }

    private void publishDueNowReminder(Order order) {
        final int prep = order.getSuggestedPreparationMinutes() != null
                ? Math.max(1, order.getSuggestedPreparationMinutes())
                : 15;

        Long partnerUserId = null;
        try {
            final PartnerSnapshot partner = partnerServiceClient.getPartnerById(order.getPartnerId());
            if (partner != null && partner.getUserId() != null) {
                partnerUserId = partner.getUserId();
            }
        } catch (Exception ex) {
            log.warn("Impossible de résoudre partnerUserId pour rappel 'due-now' partnerId={} : {}",
                    order.getPartnerId(), ex.getMessage());
        }

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "ORDER_SCHEDULED_PREP_REMINDER");
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("partnerId", order.getPartnerId());
        if (partnerUserId != null) {
            payload.put("partnerUserId", partnerUserId);
        }
        payload.put("customerId", order.getCustomerId());
        payload.put("scheduledDeliveryTime", order.getScheduledDeliveryTime().toString());
        payload.put("prepMinutes", prep);
        payload.put("bufferMinutes", 0);
        payload.put("prepLeadMinutes", 0);
        payload.put("status", OrderStatus.PREPARING.name());
        payload.put("action", "ORDER_SCHEDULED_PREP_REMINDER");
        payload.put("total", order.getTotal() != null ? order.getTotal().toPlainString() : "0");

        orderEventProducer.publishOrderPayload(payload);
    }
}
