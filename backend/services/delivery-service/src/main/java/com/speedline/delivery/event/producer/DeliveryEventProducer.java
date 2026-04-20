package com.speedline.delivery.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * GCP Pub/Sub producer for dispatch/delivery events.
 *
 * <p>Mirrors {@code com.speedline.order.event.producer.OrderEventProducer}:
 * uses {@link ObjectProvider} so the bean does not fail to start when Pub/Sub
 * is disabled (tests, offline dev); publishes JSON with a 10 s timeout.
 *
 * <p>DISP-101 publishes {@link DispatchAssignedEvent} on {@code dispatch-events}.
 * Future events ({@code DeliveryPickedUp}, {@code DeliveryCompleted}) reuse the
 * same topic to keep the consumer set small.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventProducer {

    private static final int PUBLISH_TIMEOUT_SECONDS = 10;

    private final ObjectProvider<PubSubTemplate> pubSubTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${dispatch.events.topic:dispatch-events}")
    private String dispatchEventsTopic;

    public void publishAssigned(DispatchAssignedEvent event) {
        if (event == null) return;
        publish(event.getEventType(), event.getOrderId(), event);
    }

    private void publish(String eventName, Long orderId, Object payload) {
        final PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        if (pubSubTemplate == null) {
            log.warn("PubSubTemplate absent — {} non publié orderId={}. En local: PUBSUB_EMULATOR_HOST=localhost:8090.",
                    eventName, orderId);
            return;
        }
        try {
            final String json = objectMapper.writeValueAsString(payload);
            final String messageId = pubSubTemplate.publish(dispatchEventsTopic, json)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("{} published topic={} messageId={} orderId={}",
                    eventName, dispatchEventsTopic, messageId, orderId);
        } catch (Exception ex) {
            log.warn("Impossible de publier {} sur topic={} orderId={}",
                    eventName, dispatchEventsTopic, orderId, ex);
        }
    }
}
