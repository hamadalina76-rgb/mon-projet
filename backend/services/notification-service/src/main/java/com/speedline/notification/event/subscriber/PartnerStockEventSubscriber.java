package com.speedline.notification.event.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.speedline.notification.domain.Notification;
import com.speedline.notification.domain.NotificationChannel;
import com.speedline.notification.domain.NotificationType;
import com.speedline.notification.repository.NotificationRepository;
import com.speedline.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * GCP Pub/Sub subscriber for partner product stock events (topic: partner-product-stock).
 * Events: PRODUCT_LOW_STOCK, PRODUCT_OUT_OF_STOCK. Alerts the partner via in-app notification.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PartnerStockEventSubscriber {

    private static final String SUBSCRIPTION = "partner-product-stock-notification-sub";

    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;
    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;
    private final MessageSource messageSource;

    @Bean
    public MessageChannel partnerStockEventsChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter partnerStockEventsInboundAdapter(
            @Qualifier("partnerStockEventsChannel") MessageChannel channel) {
        log.info("Initializing Pub/Sub subscriber for partner-product-stock: {}", SUBSCRIPTION);
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, SUBSCRIPTION);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "partnerStockEventsChannel")
    public MessageHandler partnerStockEventsMessageHandler() {
        return message -> {
            try {
                String payload = (String) message.getPayload();
                log.debug("Received partner stock event: {}", payload);
                Map<String, Object> event = objectMapper.readValue(payload, Map.class);
                handleStockEvent(event);
                BasicAcknowledgeablePubsubMessage originalMessage =
                        message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
                if (originalMessage != null) originalMessage.ack();
            } catch (Exception e) {
                log.error("Error processing partner stock event: {}", e.getMessage(), e);
                BasicAcknowledgeablePubsubMessage originalMessage =
                        message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
                if (originalMessage != null) originalMessage.nack();
            }
        };
    }

    private void handleStockEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Number partnerIdNum = (Number) event.get("partnerId");
        Long partnerId = partnerIdNum != null ? partnerIdNum.longValue() : null;
        Number partnerUserIdNum = (Number) event.get("partnerUserId");
        Long partnerUserId = partnerUserIdNum != null ? partnerUserIdNum.longValue() : null;
        Number productIdNum = (Number) event.get("productId");
        Long productId = productIdNum != null ? productIdNum.longValue() : null;
        String productName = (String) event.get("productName");
        if (productName == null) productName = "";

        if (partnerId == null) {
            log.warn("Stock event missing partnerId, skipping");
            return;
        }
        // Store under partner's userId so notification appears in partner's history (GET /notifications/{userId})
        Long notificationUserId = partnerUserId != null && partnerUserId != 0L ? partnerUserId : 0L;

        if ("PRODUCT_OUT_OF_STOCK".equals(eventType)) {
            String title = messageSource.getMessage("stock.out.title", null, Locale.FRENCH);
            String message = messageSource.getMessage("stock.out.message", new Object[]{productName}, Locale.FRENCH);
            Notification notif = Notification.builder()
                    .userId(notificationUserId)
                    .type(NotificationType.SYSTEM)
                    .title(title)
                    .message(message)
                    .data(Map.of("partnerId", partnerId, "productId", productId != null ? productId : 0, "productName", productName, "eventType", "PRODUCT_OUT_OF_STOCK"))
                    .channel(NotificationChannel.IN_APP)
                    .isRead(false)
                    .isSent(true)
                    .sentAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            notif = notificationRepository.save(notif);
            notificationService.pushToPartnerTopic(partnerId, notif);
            notificationService.pushToAdminTopic(notif);
            log.info("Out-of-stock notification sent to partner {} and admin for product {}", partnerId, productName);
        } else if ("PRODUCT_LOW_STOCK".equals(eventType)) {
            Number quantityNum = (Number) event.get("quantity");
            Number thresholdNum = (Number) event.get("threshold");
            int quantity = quantityNum != null ? quantityNum.intValue() : 0;
            int threshold = thresholdNum != null ? thresholdNum.intValue() : 0;
            String title = messageSource.getMessage("stock.low.title", null, Locale.FRENCH);
            String message = messageSource.getMessage("stock.low.message", new Object[]{productName, quantity, threshold}, Locale.FRENCH);
            Notification notif = Notification.builder()
                    .userId(notificationUserId)
                    .type(NotificationType.SYSTEM)
                    .title(title)
                    .message(message)
                    .data(Map.of("partnerId", partnerId, "productId", productId != null ? productId : 0, "productName", productName, "quantity", quantity, "threshold", threshold, "eventType", "PRODUCT_LOW_STOCK"))
                    .channel(NotificationChannel.IN_APP)
                    .isRead(false)
                    .isSent(true)
                    .sentAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            notif = notificationRepository.save(notif);
            notificationService.pushToPartnerTopic(partnerId, notif);
            notificationService.pushToAdminTopic(notif);
            log.info("Low-stock notification sent to partner {} and admin for product {}", partnerId, productName);
        } else {
            log.warn("Unknown partner stock event type: {}", eventType);
        }
    }
}
