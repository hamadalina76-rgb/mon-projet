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

import java.time.LocalDateTime;
import java.util.Map;

/**
 * GCP Pub/Sub subscriber for partner events.
 * Uses Spring Integration with PubSubInboundChannelAdapter.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PartnerEventSubscriber {

    private static final String SUBSCRIPTION = "partner-events-notification-sub";

    private final NotificationRepository notificationRepository;
    private final NotificationServiceImpl notificationService;
    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;

    @Bean
    public MessageChannel partnerEventsChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter partnerEventsInboundAdapter(
            @Qualifier("partnerEventsChannel") MessageChannel channel) {
        log.info("========== INITIALIZING PUB/SUB SUBSCRIBER ==========");
        log.info("Subscription: {}", SUBSCRIPTION);
        
        PubSubInboundChannelAdapter adapter = 
            new PubSubInboundChannelAdapter(pubSubTemplate, SUBSCRIPTION);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        
        log.info("========== PUB/SUB SUBSCRIBER INITIALIZED ==========");
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "partnerEventsChannel")
    public MessageHandler partnerEventsMessageHandler() {
        return message -> {
            try {
                String payload = (String) message.getPayload();
                log.info("========== RECEIVED PARTNER EVENT FROM PUB/SUB ==========");
                log.info("Payload: {}", payload);

                Map<String, Object> event = objectMapper.readValue(payload, Map.class);
                log.info("Parsed event: {}", event);
                
                handlePartnerEvent(event);

                // Acknowledge the message
                BasicAcknowledgeablePubsubMessage originalMessage = 
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, 
                                            BasicAcknowledgeablePubsubMessage.class);
                if (originalMessage != null) {
                    originalMessage.ack();
                    log.info("Message ACKed successfully");
                }
            } catch (Exception e) {
                log.error("========== ERROR PROCESSING PARTNER EVENT ==========");
                log.error("Error: {}", e.getMessage(), e);
                BasicAcknowledgeablePubsubMessage originalMessage = 
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, 
                                            BasicAcknowledgeablePubsubMessage.class);
                if (originalMessage != null) {
                    originalMessage.nack();
                }
            }
        };
    }

    private void handlePartnerEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Number partnerIdNum = (Number) event.get("partnerId");
        Number userIdNum = (Number) event.get("userId");
        String businessName = (String) event.get("businessName");
        String brandName = (String) event.get("brandName");
        String reason = (String) event.get("reason");
        String email = (String) event.get("email");

        Long partnerId = partnerIdNum != null ? partnerIdNum.longValue() : null;
        Long userId = userIdNum != null ? userIdNum.longValue() : null;

        String displayName = brandName != null && !brandName.isEmpty() ? brandName : businessName;

        log.info("Processing event: type={}, partnerId={}, userId={}, name={}, email={}", 
                 eventType, partnerId, userId, displayName, email);

        switch (eventType) {
            case "PARTNER_REQUEST_SUBMITTED":
                handlePartnerRequestSubmitted(partnerId, userId, displayName);
                break;
            case "PARTNER_APPROVED":
                handlePartnerApproved(partnerId, userId, displayName, email);
                break;
            case "PARTNER_ACTIVATED":
                handlePartnerActivated(partnerId, userId, displayName, email);
                break;
            case "PARTNER_REJECTED":
                handlePartnerRejected(partnerId, userId, displayName, reason);
                break;
            case "PARTNER_SUSPENDED":
                handlePartnerSuspended(partnerId, userId, displayName, reason);
                break;
            case "PARTNER_DEACTIVATED":
                handlePartnerDeactivated(partnerId, userId, displayName, reason);
                break;
            case "PARTNER_INFO_REQUESTED":
                handlePartnerInfoRequested(partnerId, userId, displayName, reason);
                break;
            default:
                log.warn("Unknown partner event type: {}", eventType);
        }
    }

    private void handlePartnerRequestSubmitted(Long partnerId, Long userId, String displayName) {
        log.info("========== HANDLING PARTNER REQUEST SUBMITTED ==========");

        Notification adminNotif = Notification.builder()
                .userId(0L)
                .type(NotificationType.PARTNER)
                .title("Nouvelle demande partenaire")
                .message("Le partenaire \"" + displayName + "\" a soumis une demande d'inscription.")
                .data(Map.of("partnerId", partnerId, "action", "REVIEW_PARTNER"))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Saving admin notification to database...");
        adminNotif = notificationRepository.save(adminNotif);
        log.info("Notification saved with ID: {}", adminNotif.getId());
        
        log.info("Pushing notification to admin WebSocket topic...");
        notificationService.pushToAdminTopic(adminNotif);
        log.info("========== COMPLETED PARTNER REQUEST SUBMITTED ==========");
    }

    private void handlePartnerApproved(Long partnerId, Long userId, String displayName, String email) {
        log.info("========== HANDLING PARTNER APPROVED ==========");
        log.info("Partner approved: partnerId={}, userId={}, name={}, email={}", partnerId, userId, displayName, email);

        // Create and save in-app notification
        Notification partnerNotif = Notification.builder()
                .userId(userId)
                .type(NotificationType.PARTNER)
                .title("Votre compte a été approuvé !")
                .message("Félicitations ! Votre demande de partenariat pour \"" + displayName + "\" a été approuvée. Vous pouvez maintenant commencer à recevoir des commandes.")
                .data(Map.of("partnerId", partnerId, "action", "PARTNER_APPROVED", "newStatus", "ACTIVE"))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        partnerNotif = notificationRepository.save(partnerNotif);
        log.info("Notification saved with ID: {}", partnerNotif.getId());
        
        // Push WebSocket notification
        notificationService.pushToPartnerTopic(partnerId, partnerNotif);
        log.info("WebSocket notification pushed to partner {}", partnerId);
        
        // Send confirmation email
        try {
            log.info("Sending approval confirmation email...");
            
            if (email != null && !email.isEmpty()) {
                Map<String, Object> emailVars = Map.of(
                    "partnerName", displayName,
                    "dashboardUrl", "http://localhost:4200/dashboard"
                );
                
                notificationService.sendEmail(
                    email,
                    "🎉 Votre compte partenaire SpeedLine a été approuvé !",
                    "partner-approved-email",
                    emailVars
                );
                log.info("✅ Approval email sent to: {}", email);
            } else {
                log.warn("Cannot send email: partner email not found for partnerId {}", partnerId);
            }
        } catch (Exception e) {
            log.error("Failed to send approval email for partner {}: {}", partnerId, e.getMessage());
            // Don't throw - notification already saved
        }
        
        log.info("========== COMPLETED PARTNER APPROVED ==========");
    }

    private void handlePartnerActivated(Long partnerId, Long userId, String displayName, String email) {
        log.info("========== HANDLING PARTNER ACTIVATED ==========");
        log.info("Partner activated: partnerId={}, userId={}, name={}, email={}", partnerId, userId, displayName, email);

        // Create and save in-app notification (different message from approval)
        Notification partnerNotif = Notification.builder()
                .userId(userId)
                .type(NotificationType.PARTNER)
                .title("Compte réactivé")
                .message("Votre compte partenaire \"" + displayName + "\" a été réactivé. Vous pouvez à nouveau recevoir des commandes.")
                .data(Map.of("partnerId", partnerId, "action", "PARTNER_ACTIVATED", "newStatus", "ACTIVE"))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        partnerNotif = notificationRepository.save(partnerNotif);
        log.info("Notification saved with ID: {}", partnerNotif.getId());
        
        // Push WebSocket notification
        notificationService.pushToPartnerTopic(partnerId, partnerNotif);
        log.info("WebSocket notification pushed to partner {}", partnerId);
        
        log.info("========== COMPLETED PARTNER ACTIVATED ==========");
    }

    private void handlePartnerDeactivated(Long partnerId, Long userId, String displayName, String reason) {
        log.info("Partner deactivated: partnerId={}, userId={}, reason={}", partnerId, userId, reason);

        String message = "Votre compte partenaire \"" + displayName + "\" a été désactivé.";
        if (reason != null && !reason.isEmpty()) {
            message += " Raison : " + reason;
        }
        message += " Pour réactiver votre compte, veuillez contacter le support.";

        Notification partnerNotif = Notification.builder()
                .userId(userId)
                .type(NotificationType.PARTNER)
                .title("Compte partenaire désactivé")
                .message(message)
                .data(Map.of("partnerId", partnerId, "action", "PARTNER_DEACTIVATED", "newStatus", "INACTIVE", "reason", reason != null ? reason : ""))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        partnerNotif = notificationRepository.save(partnerNotif);
        notificationService.pushToPartnerTopic(partnerId, partnerNotif);
        log.info("Deactivation notification sent to partner {}", partnerId);
    }

    private void handlePartnerRejected(Long partnerId, Long userId, String displayName, String reason) {
        log.info("Partner rejected: partnerId={}, userId={}, reason={}", partnerId, userId, reason);

        String message = "Votre demande de partenariat pour \"" + displayName + "\" a été refusée.";
        if (reason != null && !reason.isEmpty()) {
            message += " Raison : " + reason;
        }

        Notification partnerNotif = Notification.builder()
                .userId(userId)
                .type(NotificationType.PARTNER)
                .title("Demande de partenariat refusée")
                .message(message)
                .data(Map.of("partnerId", partnerId, "action", "PARTNER_REJECTED", "newStatus", "REJECTED", "reason", reason != null ? reason : ""))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        partnerNotif = notificationRepository.save(partnerNotif);
        notificationService.pushToPartnerTopic(partnerId, partnerNotif);
    }

    private void handlePartnerSuspended(Long partnerId, Long userId, String displayName, String reason) {
        log.info("Partner suspended: partnerId={}, userId={}", partnerId, userId);

        String message = "Votre compte partenaire \"" + displayName + "\" a été suspendu.";
        if (reason != null && !reason.isEmpty()) {
            message += " Raison : " + reason;
        }

        Notification partnerNotif = Notification.builder()
                .userId(userId)
                .type(NotificationType.PARTNER)
                .title("Compte partenaire suspendu")
                .message(message)
                .data(Map.of("partnerId", partnerId, "action", "PARTNER_SUSPENDED", "newStatus", "SUSPENDED", "reason", reason != null ? reason : ""))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        partnerNotif = notificationRepository.save(partnerNotif);
        notificationService.pushToPartnerTopic(partnerId, partnerNotif);
    }

    private void handlePartnerInfoRequested(Long partnerId, Long userId, String displayName, String message) {
        log.info("Partner info requested: partnerId={}, userId={}, message={}", partnerId, userId, message);

        String notificationMessage = "Des informations complémentaires sont requises pour votre demande de partenariat \"" + displayName + "\".";
        if (message != null && !message.isEmpty()) {
            notificationMessage += "\n\n" + message;
        }

        Notification partnerNotif = Notification.builder()
                .userId(userId)
                .type(NotificationType.PARTNER)
                .title("Informations complémentaires demandées")
                .message(notificationMessage)
                .data(Map.of(
                    "partnerId", partnerId,
                    "action", "PARTNER_INFO_REQUESTED",
                    "newStatus", "DOCUMENTS_MISSING",
                    "adminMessage", message != null ? message : ""
                ))
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .isSent(true)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        partnerNotif = notificationRepository.save(partnerNotif);
        notificationService.pushToPartnerTopic(partnerId, partnerNotif);
        log.info("Info request notification sent to partner {}", partnerId);
    }
}
