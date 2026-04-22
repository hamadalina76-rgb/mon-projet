package com.speedline.delivery.event.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.event.PendingOrderEnricher;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.dispatch.service.ScheduledOrderRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.Locale;
import java.util.Map;
import java.time.Instant;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PendingOrderEnricher pendingOrderEnricher;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final ScheduledOrderRedisRepository scheduledOrderRedisRepository;
    private final DispatchProperties properties;
    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;

    @Value("${dispatch.order-events.subscription:order-events-delivery-sub}")
    private String subscription;

    @Bean
    public MessageChannel orderEventsDeliveryChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter orderEventsInboundAdapter(
            @Qualifier("orderEventsDeliveryChannel") MessageChannel channel) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscription);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "orderEventsDeliveryChannel")
    public MessageHandler orderEventsMessageHandler() {
        return message -> {
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
                    .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            try {
                final String payload = (String) message.getPayload();
                final Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
                });
                final String eventType = resolveEventType(event);
                if ("ORDER_CREATED".equals(eventType)) {
                    pendingOrderEnricher.enrichOptional(event).ifPresent(order -> {
                        Instant nowWithLead = Instant.now().plusSeconds(properties.getScheduledOrders().getLeadTimeMinutes() * 60L);
                        if (Boolean.TRUE.equals(order.getIsScheduled())
                                && order.getScheduledDeliveryAt() != null
                                && order.getScheduledDeliveryAt().isAfter(nowWithLead)) {
                            scheduledOrderRedisRepository.add(order);
                        } else {
                            pendingOrderRedisRepository.add(order);
                        }
                    });
                }
                if (originalMessage != null) {
                    originalMessage.ack();
                }
            } catch (Exception ex) {
                log.error("Error handling order event: {}", ex.getMessage(), ex);
                if (originalMessage != null) {
                    originalMessage.nack();
                }
            }
        };
    }

    private static String resolveEventType(Map<String, Object> event) {
        final Object rawType = event.get("eventType");
        if (rawType == null) {
            return "ORDER_CREATED";
        }
        final String normalized = String.valueOf(rawType).trim();
        if (normalized.isEmpty()) {
            return "ORDER_CREATED";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
