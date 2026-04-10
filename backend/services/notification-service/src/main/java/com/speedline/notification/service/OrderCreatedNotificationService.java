package com.speedline.notification.service;

import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds in-app + WebSocket notification for partner when an order is created.
 * Used by Pub/Sub consumer and by the HTTP internal fallback (no GCP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;

    public void handleOrderCreatedEvent(Map<String, Object> event) {
        Number orderIdNum = (Number) event.get("orderId");
        String orderNumber = (String) event.get("orderNumber");
        Number partnerIdNum = (Number) event.get("partnerId");
        Number partnerUserIdNum = (Number) event.get("partnerUserId");
        Number customerIdNum = (Number) event.get("customerId");
        Number itemCountNum = (Number) event.get("itemCount");

        BigDecimal total = parseBigDecimal(event.get("total"));

        Long orderId = orderIdNum != null ? orderIdNum.longValue() : null;
        Long partnerId = partnerIdNum != null ? partnerIdNum.longValue() : null;
        Long partnerUserId = partnerUserIdNum != null ? partnerUserIdNum.longValue() : null;
        Long customerId = customerIdNum != null ? customerIdNum.longValue() : null;
        int itemCount = itemCountNum != null ? itemCountNum.intValue() : 0;

        if (partnerId == null || orderId == null) {
            log.warn("Order event missing partnerId or orderId, skipping. event={}", event);
            return;
        }

        // Inbox REST : GET /notifications/{userId} utilise l'id utilisateur (JWT), pas partnerId
        final Long notificationUserId = partnerUserId != null ? partnerUserId : partnerId;
        if (partnerUserId == null) {
            log.warn("ORDER_CREATED sans partnerUserId — notification peut être absente de la cloche (userId={} utilisé en repli)",
                    notificationUserId);
        }

        log.info("Processing ORDER_CREATED: orderId={}, orderNumber={}, partnerId={}, partnerUserId={}, total={}",
                orderId, orderNumber, partnerId, partnerUserId, total);

        String title = "Nouvelle commande #" + (orderNumber != null ? orderNumber : orderId);
        String message = buildOrderMessage(itemCount, total);

        Map<String, Object> data = new HashMap<>();
        data.put("id", orderId);
        data.put("orderId", orderId);
        data.put("orderNumber", orderNumber != null ? orderNumber : "");
        data.put("customerId", customerId);
        data.put("partnerId", partnerId);
        if (partnerUserId != null) {
            data.put("partnerUserId", partnerUserId);
        }
        data.put("total", total != null ? total.toPlainString() : "0");
        data.put("itemCount", itemCount);
        data.put("action", "ORDER_NEW");
        data.put("status", event.getOrDefault("status", "PENDING"));

        Notification notification = Notification.builder()
                .userId(notificationUserId)
                .type(NotificationType.ORDER)
                .title(title)
                .message(message)
                .data(data)
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Order notification saved with ID: {}", notification.getId());

        notificationService.pushToPartnerTopic(partnerId, notification);
        log.info("ORDER_CREATED WebSocket push sent to partner {}", partnerId);

        // Also notify admin in real-time about new order
        Notification adminNotification = Notification.builder()
                .userId(0L)
                .type(NotificationType.ORDER)
                .title("Nouvelle commande #" + (orderNumber != null ? orderNumber : orderId))
                .message(message)
                .data(data)
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        adminNotification = notificationRepository.save(adminNotification);
        notificationService.pushToAdminTopic(adminNotification);
        log.info("ORDER_CREATED WebSocket push sent to admin topic");
    }

    private static String buildOrderMessage(int itemCount, BigDecimal total) {
        String itemLabel = itemCount == 1 ? "article" : "articles";
        if (total != null) {
            return itemCount + " " + itemLabel + " • " + total.toPlainString() + " MAD";
        }
        return itemCount + " " + itemLabel;
    }

    private static BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
