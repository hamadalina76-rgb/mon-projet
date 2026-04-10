package com.speedline.notification.service;

import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;

    public void handleOrderStatusChangedEvent(Map<String, Object> event) {
        Number orderIdNum = (Number) event.get("orderId");
        String orderNumber = (String) event.get("orderNumber");
        Number customerIdNum = (Number) event.get("customerId");
        Number partnerIdNum = (Number) event.get("partnerId");
        String newStatus = (String) event.get("newStatus");
        String previousStatus = (String) event.get("previousStatus");
        String actorType = (String) event.get("actorType");

        Long orderId = orderIdNum != null ? orderIdNum.longValue() : null;
        Long customerId = customerIdNum != null ? customerIdNum.longValue() : null;
        Long partnerId = partnerIdNum != null ? partnerIdNum.longValue() : null;

        if (orderId == null || customerId == null || newStatus == null) {
            log.warn("ORDER_STATUS_CHANGED missing required fields, skipping. event={}", event);
            return;
        }

        log.info("Processing ORDER_STATUS_CHANGED: orderId={}, orderNumber={}, {} -> {}, customerId={}",
                orderId, orderNumber, previousStatus, newStatus, customerId);

        // Send notification to customer
        String title = "Commande #" + (orderNumber != null ? orderNumber : orderId);
        String message = STATUS_MESSAGES_FR.getOrDefault(newStatus,
                "Le statut de votre commande a changé : " + newStatus);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("orderNumber", orderNumber != null ? orderNumber : "");
        data.put("previousStatus", previousStatus);
        data.put("newStatus", newStatus);
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
    }
}
