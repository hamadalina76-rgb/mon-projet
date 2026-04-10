package com.speedline.order.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.speedline.order.event.OrderCreatedEvent;
import com.speedline.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publie ORDER_CREATED sur le topic Pub/Sub {@code order-events},
 * comme {@code PartnerEventPublisher} publie sur {@code partner-events}.
 * Le {@link com.speedline.notification.event.consumer.OrderEventConsumer} consomme ce topic
 * et pousse la notification WebSocket au partenaire.
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
            log.warn(
                    "PubSubTemplate absent (spring.cloud.gcp.pubsub.enabled=false ou GCP mal configuré) — "
                            + "ORDER_CREATED non publié orderId={}. "
                            + "En local : démarrez l'émulateur Pub/Sub et utilisez PUBSUB_EMULATOR_HOST=localhost:8090.",
                    event.getOrderId());
            return;
        }

        try {
            final String payload = objectMapper.writeValueAsString(event);

            final String messageId = pubSubTemplate.publish(orderEventsTopic, payload)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("ORDER_CREATED published topic={} messageId={} orderId={}",
                    orderEventsTopic, messageId, event.getOrderId());
        } catch (Exception ex) {
            log.warn("Impossible de publier ORDER_CREATED sur topic={} orderId={}",
                    orderEventsTopic, event.getOrderId(), ex);
        }
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event == null) return;

        final PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        if (pubSubTemplate == null) {
            log.warn("PubSubTemplate absent — ORDER_STATUS_CHANGED non publié orderId={}.", event.getOrderId());
            return;
        }

        try {
            final String payload = objectMapper.writeValueAsString(event);
            final String messageId = pubSubTemplate.publish(orderEventsTopic, payload)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("ORDER_STATUS_CHANGED published topic={} messageId={} orderId={} {}->{}",
                    orderEventsTopic, messageId, event.getOrderId(),
                    event.getPreviousStatus(), event.getNewStatus());
        } catch (Exception ex) {
            log.warn("Impossible de publier ORDER_STATUS_CHANGED sur topic={} orderId={}",
                    orderEventsTopic, event.getOrderId(), ex);
        }
    }
}
