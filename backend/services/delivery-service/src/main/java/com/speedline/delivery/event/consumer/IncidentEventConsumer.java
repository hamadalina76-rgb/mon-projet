package com.speedline.delivery.event.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.speedline.delivery.dispatch.service.IncidentHandler;
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

@Configuration
@RequiredArgsConstructor
@Slf4j
public class IncidentEventConsumer {

    private final ObjectMapper objectMapper;
    private final PubSubTemplate pubSubTemplate;
    private final IncidentHandler incidentHandler;

    @Value("${dispatch.delivery-incident.subscription:delivery-incident-delivery-sub}")
    private String subscription;

    @Bean
    public MessageChannel deliveryIncidentChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter deliveryIncidentInboundAdapter(
            @Qualifier("deliveryIncidentChannel") MessageChannel channel) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, subscription);
        adapter.setOutputChannel(channel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "deliveryIncidentChannel")
    public MessageHandler deliveryIncidentMessageHandler() {
        return message -> {
            BasicAcknowledgeablePubsubMessage originalMessage = message.getHeaders()
                    .get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            try {
                final String payload = (String) message.getPayload();
                final Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
                final String eventType = String.valueOf(event.getOrDefault("eventType", "DELIVERY_INCIDENT"))
                        .trim()
                        .toUpperCase(Locale.ROOT);
                if ("DELIVERY_INCIDENT".equals(eventType)) {
                    incidentHandler.handleIncident(event);
                }
                if (originalMessage != null) {
                    originalMessage.ack();
                }
            } catch (Exception ex) {
                log.error("Error handling delivery incident event: {}", ex.getMessage(), ex);
                if (originalMessage != null) {
                    originalMessage.nack();
                }
            }
        };
    }
}
