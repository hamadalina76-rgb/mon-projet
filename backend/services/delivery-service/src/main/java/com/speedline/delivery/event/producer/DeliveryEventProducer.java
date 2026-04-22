package com.speedline.delivery.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.speedline.delivery.dispatch.event.CourierInactivityAlertEvent;
import com.speedline.delivery.dispatch.event.CourierIncidentAlertEvent;
import com.speedline.delivery.dispatch.event.CourierRefusalEscalationEvent;
import com.speedline.delivery.dispatch.event.DispatchAssignedEvent;
import com.speedline.delivery.dispatch.event.PartnerPreparationStartEvent;
import com.speedline.delivery.dispatch.event.ScheduledOrderNoCourierAlertEvent;
import com.speedline.delivery.dispatch.event.UrgentOrderUnassignedAlertEvent;
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

    @Value("${dispatch.admin-alerts.topic:admin-alerts}")
    private String adminAlertsTopic;

    @Value("${dispatch.admin-alerts.hr-topic:hr-notifications}")
    private String hrNotificationsTopic;

    @Value("${dispatch.admin-alerts.reliability-topic:courier-reliability-events}")
    private String reliabilityTopic;

    @Value("${dispatch.partner-notifications.topic:partner-notifications}")
    private String partnerNotificationsTopic;

    public void publishAssigned(DispatchAssignedEvent event) {
        if (event == null) return;
        publish(dispatchEventsTopic, event.getEventType(), event.getOrderId(), event);
    }

    public void publishRefusalEscalation(CourierRefusalEscalationEvent event) {
        if (event == null) return;
        String topic = switch (String.valueOf(event.getEscalationType())) {
            case "HR" -> hrNotificationsTopic;
            case "SCORE_DEGRADATION" -> reliabilityTopic;
            default -> adminAlertsTopic;
        };
        publish(topic, event.getEventType(), event.getOrderId(), event);
    }

    public void publishInactivityAlert(CourierInactivityAlertEvent event) {
        if (event == null) return;
        publish(adminAlertsTopic, event.getEventType(), null, event);
    }

    public void publishCourierIncidentAlert(CourierIncidentAlertEvent event) {
        if (event == null) return;
        publish(adminAlertsTopic, event.getEventType(), event.getOrderId(), event);
    }

    public void publishUrgentOrderUnassignedAlert(UrgentOrderUnassignedAlertEvent event) {
        if (event == null) return;
        publish(adminAlertsTopic, event.getEventType(), event.getOrderId(), event);
    }

    public void publishScheduledOrderNoCourierAlert(ScheduledOrderNoCourierAlertEvent event) {
        if (event == null) return;
        publish(adminAlertsTopic, event.getEventType(), event.getOrderId(), event);
    }

    public void publishPartnerPreparationStart(PartnerPreparationStartEvent event) {
        if (event == null) return;
        publish(partnerNotificationsTopic, event.getEventType(), event.getOrderId(), event);
    }

    private void publish(String topic, String eventName, Long orderId, Object payload) {
        final PubSubTemplate pubSubTemplate = pubSubTemplateProvider.getIfAvailable();
        if (pubSubTemplate == null) {
            log.warn("PubSubTemplate absent — {} non publié topic={} orderId={}. En local: PUBSUB_EMULATOR_HOST=localhost:8090.",
                    eventName, topic, orderId);
            return;
        }
        try {
            final String json = objectMapper.writeValueAsString(payload);
            final String messageId = pubSubTemplate.publish(topic, json)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("{} published topic={} messageId={} orderId={}",
                    eventName, topic, messageId, orderId);
        } catch (Exception ex) {
            log.warn("Impossible de publier {} sur topic={} orderId={}",
                    eventName, topic, orderId, ex);
        }
    }
}
