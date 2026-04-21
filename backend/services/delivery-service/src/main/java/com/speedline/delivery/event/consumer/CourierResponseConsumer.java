package com.speedline.delivery.event.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.speedline.delivery.dispatch.service.CourierResponseTimeoutTracker;
import com.speedline.delivery.dispatch.service.RefusalHandler;
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

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CourierResponseConsumer {

    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;
    private final RefusalHandler refusalHandler;
    private final CourierResponseTimeoutTracker timeoutTracker;

    @Value("${dispatch.courier-response.refusal-subscription:courier-order-refused-delivery-sub}")
    private String refusalSubscription;

    @Bean
    public MessageChannel courierResponseDeliveryChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter courierResponseInboundAdapter(
            @Qualifier("courierResponseDeliveryChannel") MessageChannel channel) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, refusalSubscription);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "courierResponseDeliveryChannel")
    public MessageHandler courierResponseMessageHandler() {
        return message -> {
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
                    .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            try {
                final String payload = (String) message.getPayload();
                final Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
                });
                String eventType = String.valueOf(event.getOrDefault("eventType", "")).toUpperCase();
                if ("COURIER_ORDER_REFUSED".equals(eventType)) {
                    Long orderId = parseLong(event.get("orderId"));
                    Long courierId = parseLong(event.get("courierId"));
                    String reason = String.valueOf(event.getOrDefault("reason", "OTHER"));
                    refusalHandler.handleRefusal(orderId, courierId, reason);
                }
                if (originalMessage != null) {
                    originalMessage.ack();
                }
            } catch (Exception ex) {
                log.error("Error handling courier response event: {}", ex.getMessage(), ex);
                if (originalMessage != null) {
                    originalMessage.nack();
                }
            }
        };
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
