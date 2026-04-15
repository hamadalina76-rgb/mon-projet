package com.speedline.notification.service;

import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusChangedNotificationService {

    private static final Map<String, String> STATUS_MESSAGES_FR = Map.of(
            "CONFIRMED", "Votre commande a été confirmée",
            "PREPARING", "Votre commande est en cours de préparation",
            "READY_FOR_PICKUP", "Votre commande est prête",
            "PICKED_UP", "Le livreur a récupéré votre commande",
            "IN_DELIVERY", "Votre commande est en cours de livraison",
            "DELIVERED", "Votre commande a été livrée",
            "CANCELLED", "Votre commande a été annulée"
    );

    private final NotificationServiceImpl notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public void handleOrderStatusChangedEvent(Map<String, Object> event) {
        final Long orderId = parseLong(event.get("orderId"));
        final String orderNumber = parseString(event.get("orderNumber"));
        final Long customerId = parseLong(event.get("customerId"));
        final Long partnerId = parseLong(event.get("partnerId"));
        final String previousStatus = parseString(event.get("previousStatus"));
        final String actorType = parseString(event.get("actorType"));

        // Some producers send "status" while others send "newStatus".
        final String newStatus = firstNonBlank(
                parseString(event.get("newStatus")),
                parseString(event.get("status"))
        );

        if (orderId == null || customerId == null || newStatus == null) {
            log.warn("ORDER_STATUS_CHANGED missing required fields, skipping. event={}", event);
            return;
        }

        // PREPARING transitions are already emitted as ORDER_ACCEPTED to avoid duplicates.
        if ("PREPARING".equalsIgnoreCase(newStatus)) {
            log.debug("Skipping PREPARING ORDER_STATUS_CHANGED event (handled by ORDER_ACCEPTED). event={}", event);
            return;
        }

        log.info("Processing ORDER_STATUS_CHANGED: orderId={}, orderNumber={}, {} -> {}, customerId={}",
                orderId, orderNumber, previousStatus, newStatus, customerId);

        // Send notification to customer
        String title = "Commande #" + (orderNumber != null ? orderNumber : orderId);
        String message = STATUS_MESSAGES_FR.getOrDefault(newStatus,
                "Le statut de votre commande a changé : " + newStatus);

        Map<String, Object> data = new HashMap<>();
        data.put("id", orderId);
        data.put("orderId", orderId);
        data.put("orderNumber", orderNumber != null ? orderNumber : "");
        data.put("partnerId", partnerId);
        data.put("previousStatus", previousStatus);
        data.put("status", newStatus);
        data.put("newStatus", newStatus);
        data.put("actorType", actorType != null ? actorType : "SYSTEM");
        data.put("eventType", "ORDER_STATUS_CHANGED");
        data.put("action", "ORDER_STATUS_CHANGED");

        // Customer notification (real-time via WebSocket + FCM push)
        notificationService.sendNotification(
                customerId,
                NotificationType.ORDER,
                title,
                message,
                data,
                NotificationChannel.IN_APP
        );
        log.info("ORDER_STATUS_CHANGED notification sent to customer {} for order {}", customerId, orderId);

        // Broadcast timeline event to admin-panel via WebSocket
        String description = (String) event.get("description");
        Map<String, Object> timelineEntry = new HashMap<>();
        timelineEntry.put("orderId", orderId);
        timelineEntry.put("status", newStatus);
        timelineEntry.put("previousStatus", previousStatus);
        timelineEntry.put("description", description != null ? description : message);
        timelineEntry.put("actorType", actorType);
        timelineEntry.put("timestamp", LocalDateTime.now().toString());

        try {
            messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/timeline", timelineEntry);
            log.info("Timeline broadcast sent to /topic/orders/{}/timeline: {} -> {}", orderId, previousStatus, newStatus);
        } catch (Exception ex) {
            log.warn("Failed to broadcast timeline for orderId={}: {}", orderId, ex.getMessage());
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

        private static String firstNonBlank(String first, String second) {
                if (first != null && !first.isBlank()) {
                        return first;
                }
                if (second != null && !second.isBlank()) {
                        return second;
                }
                return null;
        }
}
