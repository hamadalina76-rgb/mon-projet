package com.speedline.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.speedline.notification.service.OrderCreatedNotificationService;
import com.speedline.notification.service.OrderStatusChangedNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.Map;

/**
 * Consomme le topic {@code order-events} via GCP Pub/Sub (émulateur ou cloud),
 * identique à {@code PartnerEventSubscriber} qui consomme {@code partner-events}.
 *
 * NB: pas de @ConditionalOnBean — comme PartnerEventSubscriber, on injecte
 * PubSubTemplate directement. @ConditionalOnBean est évalué avant que
 * l'auto-config GCP enregistre le bean, ce qui supprimait silencieusement
 * tout le subscriber.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String SUBSCRIPTION = "order-events-notification-sub";

    private final OrderCreatedNotificationService orderCreatedNotificationService;
    private final OrderStatusChangedNotificationService orderStatusChangedNotificationService;
    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;

    @Bean
    public MessageChannel orderEventsChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter orderEventsInboundAdapter(
            @Qualifier("orderEventsChannel") MessageChannel channel) {
        log.info("========== INITIALIZING ORDER EVENTS PUB/SUB SUBSCRIBER ==========");
        log.info("Subscription: {}", SUBSCRIPTION);

        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, SUBSCRIPTION);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);

        log.info("========== ORDER EVENTS PUB/SUB SUBSCRIBER INITIALIZED ==========");
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "orderEventsChannel")
    public MessageHandler orderEventsMessageHandler() {
        return message -> {
            BasicAcknowledgeablePubsubMessage originalMessage =
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE,
                            BasicAcknowledgeablePubsubMessage.class);
            try {
                String payload = (String) message.getPayload();
                log.info("========== RECEIVED ORDER EVENT FROM PUB/SUB ==========");
                log.info("Payload: {}", payload);

                Map<String, Object> event = objectMapper.readValue(
                        payload,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );

                // Route based on event type: STATUS_CHANGED events have "newStatus" field
                if (event.containsKey("newStatus")) {
                    orderStatusChangedNotificationService.handleOrderStatusChangedEvent(event);
                } else {
                    orderCreatedNotificationService.handleOrderCreatedEvent(event);
                }

                if (originalMessage != null) {
                    originalMessage.ack();
                    log.info("Order event ACKed successfully");
                }
            } catch (Exception e) {
                log.error("========== ERROR PROCESSING ORDER EVENT ==========");
                log.error("Error: {}", e.getMessage(), e);
                if (originalMessage != null) {
                    originalMessage.nack();
                }
            }
        };
    }
}
