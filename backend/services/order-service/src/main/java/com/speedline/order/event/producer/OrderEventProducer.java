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

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Publie les evenements de commande sur le topic Pub/Sub {@code order-events},
 * comme {@code PartnerEventPublisher} publie sur {@code partner-events}.
 * Le {@link com.speedline.notification.event.consumer.OrderEventConsumer} consomme ce topic
 * et pousse les notifications vers les applications cibles.
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
        publishEvent("ORDER_CREATED", event == null ? null : event.getOrderId(), event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event == null) {
            return;
        }

        final String eventName = event.getEventType() == null || event.getEventType().isBlank()
                ? "ORDER_STATUS_CHANGED"
                : event.getEventType();
        publishEvent(eventName, event.getOrderId(), event);
    }

    private void publishEvent(String eventName, Long orderId, Object payloadObject) {
        if (payloadObject == null) {
            return;
        }

        final PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        if (pubSubTemplate == null) {
            log.warn(
                    "PubSubTemplate absent (spring.cloud.gcp.pubsub.enabled=false ou GCP mal configuré) — "
                            + "{} non publie orderId={}. "
                            + "En local : démarrez l'émulateur Pub/Sub et utilisez PUBSUB_EMULATOR_HOST=localhost:8090.",
                    eventName,
                    orderId);
            return;
        }

        try {
            final String json = objectMapper.writeValueAsString(payloadObject);

            final String messageId = pubSubTemplate.publish(orderEventsTopic, json)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("{} published topic={} messageId={} orderId={}",
                    eventName,
                    orderEventsTopic,
                    messageId,
                    orderId);
        } catch (Exception ex) {
            log.warn("Impossible de publier {} sur topic={} orderId={}",
                    eventName,
                    orderEventsTopic,
                    orderId,
                    ex);
        }
    }

    /**
     * Publie un événement générique (ex. {@code ORDER_SCHEDULED_PREP_REMINDER}) sur le même topic que {@link OrderCreatedEvent}.
     */
    public void publishOrderPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }

        final PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        if (pubSubTemplate == null) {
            log.warn(
                    "PubSubTemplate absent — événement commande non publié payload={}",
                    payload.get("eventType"));
            return;
        }

        try {
            final String json = objectMapper.writeValueAsString(payload);

            final String messageId = pubSubTemplate.publish(orderEventsTopic, json)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Order event published topic={} messageId={} eventType={} orderId={}",
                    orderEventsTopic,
                    messageId,
                    payload.get("eventType"),
                    payload.get("orderId"));
        } catch (Exception ex) {
            log.warn("Impossible de publier événement commande sur topic={} payload={}",
                    orderEventsTopic, payload.get("eventType"), ex);
        }
    }
}
