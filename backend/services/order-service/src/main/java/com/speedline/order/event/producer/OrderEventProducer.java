package com.speedline.order.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.speedline.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * GCP Pub/Sub Producer pour les événements Order
 * - OrderCreatedEvent
 * - OrderConfirmedEvent
 * - OrderCancelledEvent
 * - OrderCompletedEvent
 * - OrderStatusChangedEvent
 * 
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final int PUBLISH_TIMEOUT_SECONDS = 10;

    private final ObjectProvider<PubSubTemplate> pubSubTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${order.events.topic:order-events}")
    private String orderEventsTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        if (event == null) {
            return;
        }

        final PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        if (pubSubTemplate == null) {
            log.debug("Pub/Sub indisponible: événement ORDER_CREATED ignoré orderId={}", event.getOrderId());
            return;
        }

        try {
            final String payload = objectMapper.writeValueAsString(event);

            final String messageId = pubSubTemplate.publish(orderEventsTopic, payload)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("ORDER_CREATED published topic={} messageId={} orderId={}", orderEventsTopic, messageId, event.getOrderId());
        } catch (Exception ex) {
            log.warn("Impossible de publier ORDER_CREATED pour orderId={}", event.getOrderId(), ex);
        }
    }
}
