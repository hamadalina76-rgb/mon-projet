package com.speedline.delivery.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.websocket.TrackingWebSocketHandler.CourierPositionUpdatedEvent;
import com.speedline.delivery.websocket.TrackingWebSocketHandler.PositionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler pour /ws/tracking/{orderId}.
 * Gère les connexions clients et diffuse les événements de position
 * reçus via CourierPositionUpdatedEvent.
 */
public class TrackingBroadcastWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TrackingBroadcastWebSocketHandler.class);

    private final Map<String, Set<WebSocketSession>> sessionsByOrderId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String orderId = extractOrderId(session);
        sessionsByOrderId
                .computeIfAbsent(orderId, id -> ConcurrentHashMap.newKeySet())
                .add(session);

        log.info("Tracking WS connection established, orderId={}, sessionId={}", orderId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("Ignoring client message on tracking channel: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String orderId = extractOrderId(session);
        sessionsByOrderId.computeIfPresent(orderId, (id, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });

        log.info("Tracking WS connection closed, orderId={}, sessionId={}, status={}",
                orderId, session.getId(), status);
    }

    /**
     * Écoute les évènements internes et pousse les positions aux clients
     * de tracking. Pour l'instant, la diffusion n'est pas filtrée par
     * orderId (faute de mapping courierId -> orderId côté domaine),
     * mais l'enveloppe respecte déjà le contrat COURIER_POSITION.
     */
    @EventListener
    public void onCourierPositionUpdated(CourierPositionUpdatedEvent event) {
        if (sessionsByOrderId.isEmpty()) {
            return;
        }

        PositionPayload p = event.payload();

        Map<String, Object> payload = Map.of(
                "type", "COURIER_POSITION",
                "payload", Map.of(
                        "courierId", event.courierId(),
                        "lat", p.lat(),
                        "lng", p.lng(),
                        "heading", p.heading(),
                        "estimatedArrivalMin", null
                )
        );

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);

            sessionsByOrderId.values().forEach(sessions ->
                    sessions.forEach(session -> {
                        if (session.isOpen()) {
                            try {
                                session.sendMessage(message);
                            } catch (IOException e) {
                                log.warn("Failed to send tracking update to session {}: {}",
                                        session.getId(), e.getMessage());
                            }
                        }
                    })
            );
        } catch (Exception e) {
            log.error("Failed to broadcast COURIER_POSITION update: {}", e.getMessage(), e);
        }
    }

    private String extractOrderId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "/ws/tracking";
        int idx = path.lastIndexOf('/');
        return (idx >= 0 && idx < path.length() - 1) ? path.substring(idx + 1) : "unknown";
    }
}

