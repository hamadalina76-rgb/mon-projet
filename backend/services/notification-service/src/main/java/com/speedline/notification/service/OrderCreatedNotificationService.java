package com.speedline.notification.service;

import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final SimpMessagingTemplate messagingTemplate;

    public void handleOrderCreatedEvent(Map<String, Object> event) {
        final Object eventType = event.get("eventType");
        if ("ORDER_SCHEDULED_PREP_REMINDER".equals(eventType)) {
            handleScheduledPrepReminder(event);
            return;
        }

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

    private void handleScheduledPrepReminder(Map<String, Object> event) {
        Number orderIdNum = (Number) event.get("orderId");
        String orderNumber = (String) event.get("orderNumber");
        Number partnerIdNum = (Number) event.get("partnerId");
        Number partnerUserIdNum = (Number) event.get("partnerUserId");
        Number prepLeadNum = (Number) event.get("prepLeadMinutes");
        String scheduledRaw = event.get("scheduledDeliveryTime") != null
                ? event.get("scheduledDeliveryTime").toString()
                : null;

        Long orderId = orderIdNum != null ? orderIdNum.longValue() : null;
        Long partnerId = partnerIdNum != null ? partnerIdNum.longValue() : null;
        Long partnerUserId = partnerUserIdNum != null ? partnerUserIdNum.longValue() : null;
        int prepLead = prepLeadNum != null ? prepLeadNum.intValue() : 30;

        if (partnerId == null || orderId == null) {
            log.warn("ORDER_SCHEDULED_PREP_REMINDER missing partnerId or orderId, skipping. event={}", event);
            return;
        }

        final Long notificationUserId = partnerUserId != null ? partnerUserId : partnerId;

        String slotLabel = scheduledRaw != null ? scheduledRaw : "";
        if (scheduledRaw != null) {
            try {
                LocalDateTime slot = LocalDateTime.parse(scheduledRaw);
                slotLabel = slot.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            } catch (Exception ignored) {
                // garder la valeur brute
            }
        }

        String title = "Commande planifiée — préparation";
        String message = String.format(
                "La commande #%s a un créneau à %s. Prévoyez %d min (prépa + marge).",
                orderNumber != null ? orderNumber : orderId,
                slotLabel,
                prepLead
        );

        Map<String, Object> data = new HashMap<>();
        data.put("id", orderId);
        data.put("orderId", orderId);
        data.put("orderNumber", orderNumber != null ? orderNumber : "");
        data.put("partnerId", partnerId);
        if (partnerUserId != null) {
            data.put("partnerUserId", partnerUserId);
        }
        data.put("scheduledDeliveryTime", scheduledRaw != null ? scheduledRaw : "");
        data.put("prepLeadMinutes", prepLead);
        data.put("action", "ORDER_SCHEDULED_PREP_REMINDER");
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
        notificationService.pushToPartnerTopic(partnerId, notification);
        log.info("ORDER_SCHEDULED_PREP_REMINDER pushed partnerId={} orderId={}", partnerId, orderId);
    }

    public void handleOrderStatusChangedEvent(Map<String, Object> event) {
        final String eventType = parseString(event.get("eventType"));
        final String actorType = parseString(event.get("actorType"));
        final String status = parseString(event.get("status"));

        // Partner acceptance is represented by PREPARING status, often sent as ORDER_ACCEPTED.
        if (!"PREPARING".equalsIgnoreCase(status)) {
            log.debug("Skipping order status event that is not a partner acceptance: {}", event);
            return;
        }

        final Long orderId = parseLong(event.get("orderId"));
        final Long customerId = parseLong(event.get("customerId"));
        if (orderId == null || customerId == null) {
            log.warn("ORDER_ACCEPTED event missing orderId/customerId, skipping. event={}", event);
            return;
        }

        final String orderNumber = parseString(event.get("orderNumber"));
        final Long partnerId = parseLong(event.get("partnerId"));
        final String estimatedDeliveryTime = parseString(event.get("estimatedDeliveryTime"));

        final String orderLabel = orderNumber != null ? "#" + orderNumber : "#" + orderId;
        final String title = "Commande acceptee";
        String message = "Votre commande " + orderLabel + " a ete acceptee par le partenaire.";
        if (estimatedDeliveryTime != null) {
            message += " Livraison estimee: " + estimatedDeliveryTime + ".";
        }

        final Map<String, Object> data = new HashMap<>();
        data.put("id", orderId);
        data.put("orderId", orderId);
        data.put("orderNumber", orderNumber != null ? orderNumber : "");
        data.put("customerId", customerId);
        data.put("partnerId", partnerId);
        data.put("status", status);
        data.put("actorType", actorType != null ? actorType : "PARTNER");
        data.put("eventType", eventType != null ? eventType : "ORDER_ACCEPTED");
        data.put("action", "ORDER_ACCEPTED");

        notificationService.sendNotification(
                customerId,
                NotificationType.ORDER,
                title,
                message,
                data,
                NotificationChannel.PUSH
        );

        log.info("ORDER_ACCEPTED notification sent to customer {} for order {}", customerId, orderId);

        // Admin order detail + liste : même canal STOMP que ORDER_STATUS_CHANGED (acceptation partenaire = PREPARING)
        final String previousStatus = parseString(event.get("previousStatus"));
        final String description = parseString(event.get("description"));
        final Map<String, Object> timelineEntry = new HashMap<>();
        timelineEntry.put("orderId", orderId);
        timelineEntry.put("status", status);
        timelineEntry.put("previousStatus", previousStatus);
        timelineEntry.put("description", description != null ? description : message);
        timelineEntry.put("actorType", data.get("actorType"));
        timelineEntry.put("timestamp", LocalDateTime.now().toString());
        try {
            messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/timeline", timelineEntry);
            log.info("ORDER_ACCEPTED timeline broadcast for orderId={}", orderId);
        } catch (Exception ex) {
            log.warn("Failed ORDER_ACCEPTED timeline orderId={}: {}", orderId, ex.getMessage());
        }

        notificationService.sendAdminBroadcast(NotificationType.ORDER, title, message, data);
        log.info("ORDER_ACCEPTED admin WebSocket broadcast for orderId={}", orderId);
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

    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String parseString(Object value) {
        if (value == null) {
            return null;
        }
        final String normalized = value.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
