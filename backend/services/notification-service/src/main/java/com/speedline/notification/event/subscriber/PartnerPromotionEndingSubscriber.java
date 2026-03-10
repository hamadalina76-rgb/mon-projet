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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * GCP Pub/Sub subscriber for partner promotion ending events (topic: partner-promotion-ending).
 * Event: PROMOTION_ENDING_SOON — notifies the partner that a product promotion ends in 3 days.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PartnerPromotionEndingSubscriber {

    private static final String SUBSCRIPTION = "partner-promotion-ending-notification-sub";

    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;
    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;

    @Bean
    public MessageChannel partnerPromotionEndingChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter partnerPromotionEndingInboundAdapter(
            @Qualifier("partnerPromotionEndingChannel") MessageChannel channel) {
        log.info("Initializing Pub/Sub subscriber for partner-promotion-ending: {}", SUBSCRIPTION);
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, SUBSCRIPTION);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "partnerPromotionEndingChannel")
    public MessageHandler partnerPromotionEndingMessageHandler() {
        return message -> {
            try {
                String payload = (String) message.getPayload();
                log.debug("Received partner promotion ending event: {}", payload);
                Map<String, Object> event = objectMapper.readValue(payload, Map.class);
                handlePromotionEndingEvent(event);
                BasicAcknowledgeablePubsubMessage originalMessage =
                        message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
                if (originalMessage != null) originalMessage.ack();
            } catch (Exception e) {
                log.error("Error processing partner promotion ending event: {}", e.getMessage(), e);
                BasicAcknowledgeablePubsubMessage originalMessage =
                        message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
                if (originalMessage != null) originalMessage.nack();
            }
        };
    }

    private void handlePromotionEndingEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (!"PROMOTION_ENDING_SOON".equals(eventType)) {
            log.warn("Unknown promotion ending event type: {}", eventType);
            return;
        }
        Number partnerIdNum = (Number) event.get("partnerId");
        Long partnerId = partnerIdNum != null ? partnerIdNum.longValue() : null;
        Number partnerUserIdNum = (Number) event.get("partnerUserId");
        Long partnerUserId = partnerUserIdNum != null ? partnerUserIdNum.longValue() : null;
        Number productIdNum = (Number) event.get("productId");
        Long productId = productIdNum != null ? productIdNum.longValue() : null;
        String productName = (String) event.get("productName");
        if (productName == null) productName = "";
        String promotionEndDateStr = (String) event.get("promotionEndDate");
        Number daysLeftNum = (Number) event.get("daysLeft");
        int daysLeft = daysLeftNum != null ? daysLeftNum.intValue() : 3;

        if (partnerId == null) {
            log.warn("Promotion ending event missing partnerId, skipping");
            return;
        }

        Long notificationUserId = partnerUserId != null && partnerUserId != 0L ? partnerUserId : 0L;

        String endDateFormatted = promotionEndDateStr;
        if (promotionEndDateStr != null && !promotionEndDateStr.isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(promotionEndDateStr);
                endDateFormatted = d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {
                // keep raw string
            }
        } else {
            endDateFormatted = "";
        }

        String title = "Promotion bientôt terminée";
        String message = daysLeft == 1
                ? String.format("Votre promotion « %s » se termine demain (fin le %s).", productName, endDateFormatted)
                : String.format("Votre promotion « %s » se termine dans %d jours (fin le %s).", productName, daysLeft, endDateFormatted);

        Notification notif = Notification.builder()
                .userId(notificationUserId)
                .type(NotificationType.PROMOTION)
                .title(title)
                .message(message)
                .data(Map.of(
                        "partnerId", partnerId,
                        "productId", productId != null ? productId : 0,
                        "productName", productName,
                        "promotionEndDate", promotionEndDateStr != null ? promotionEndDateStr : "",
                        "eventType", "PROMOTION_ENDING_SOON"))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(java.time.LocalDateTime.now())
                .createdAt(java.time.LocalDateTime.now())
                .build();
        notif = notificationRepository.save(notif);
        notificationService.pushToPartnerTopic(partnerId, notif);
        log.info("Promotion-ending notification sent to partner {} for product {}", partnerId, productName);
    }
}
