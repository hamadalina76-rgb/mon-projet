package com.speedline.delivery.websocket;

import com.speedline.delivery.client.UserServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * WebSocket Handler pour tracking temps réel.
 * Endpoint: /ws/location
 * Chaque livreur envoie ses positions via messages JSON.
 */
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TrackingWebSocketHandler.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserServiceClient userServiceClient;
    private final Map<String, WebSocketSession> activeSessionsByCourier = new ConcurrentHashMap<>();
    private final Duration positionTtl = Duration.ofSeconds(30);

    public TrackingWebSocketHandler(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            UserServiceClient userServiceClient
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        resolveCourierId(session).ifPresentOrElse(courierId -> {
            var isCourier = getStringAttribute(session, "role")
                    .filter(TrackingWebSocketHandler::isCourierRole)
                    .isPresent();

            if (!isCourier) {
                log.warn("Refusing WebSocket connection: invalid role or missing courier id");
                closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Courier role required"));
                return;
            }

            var existing = activeSessionsByCourier.put(courierId, session);
            Optional.ofNullable(existing)
                    .filter(WebSocketSession::isOpen)
                    .filter(s -> s != session)
                    .ifPresent(s -> {
                        log.info("Closing previous WebSocket session for courier {}", courierId);
                        closeQuietly(s, CloseStatus.NORMAL.withReason("New connection established"));
                    });

            markCourierOnline(courierId);
            log.info("WebSocket connection established for courier {}", courierId);
        }, () -> {
            log.warn("Refusing WebSocket connection: invalid role or missing courier id");
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Courier role required"));
        });
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        resolveCourierId(session).ifPresentOrElse(courierId -> {
            String payload = message.getPayload();
            log.debug("Received WS message from courier {}: {}", courierId, payload);

            try {
                PositionUpdateMessage update =
                        objectMapper.readValue(payload, PositionUpdateMessage.class);

                if (!"POSITION_UPDATE".equalsIgnoreCase(update.type())) {
                    log.debug("Ignoring WS message with unsupported type={} from courier={}",
                            update.type(), courierId);
                    return;
                }

                PositionPayload p = update.payload();
                if (p == null) {
                    log.warn("Received POSITION_UPDATE without payload for courier {}", courierId);
                    return;
                }

                // Stocker une structure simple en Redis (JSON sérialisé)
                String redisKey = Objects.requireNonNull("courier:%s:position".formatted(courierId));
                String redisValue = Objects.requireNonNull(objectMapper.writeValueAsString(p));
                stringRedisTemplate.opsForValue().set(
                        redisKey,
                        redisValue,
                        Objects.requireNonNull(positionTtl)
                );

                String onlineKey = Objects.requireNonNull("courier:%s:isOnline".formatted(courierId));
                stringRedisTemplate.opsForValue().set(
                        onlineKey,
                        "true",
                        Objects.requireNonNull(positionTtl)
                );

                log.info("Updated Redis position for courier {}", courierId);

                // Publier un évènement interne courier.position.updated
                eventPublisher.publishEvent(new CourierPositionUpdatedEvent(courierId, p));
            } catch (Exception e) {
                log.error("Failed to process position update for courier {}: {}", courierId, e.getMessage(), e);
            }
        }, () -> {
            log.warn("Received message without courierId, closing session");
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("Missing courier id"));
        });
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage textMessage) {
            handleTextMessage(session, textMessage);
        } else {
            log.warn("Ignoring non-text WebSocket message from session {}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String courierId = (String) session.getAttributes().get("userId");
        if (courierId != null) {
            WebSocketSession existing = activeSessionsByCourier.get(courierId);
            if (existing == session) {
                activeSessionsByCourier.remove(courierId);
            }
            log.info("WebSocket connection closed for courier {} with status {}", courierId, status);
        } else {
            log.info("WebSocket connection closed for anonymous session {} with status {}", session.getId(), status);
        }
        // Le passage hors ligne s’appuie sur le TTL Redis.
    }

    /**
     * Push a typed JSON message to a specific courier's WebSocket session.
     * No-op if the courier has no active session.
     */
    public void sendToCourier(String courierId, String type, Map<String, Object> payload) {
        if (courierId == null) return;
        WebSocketSession session = activeSessionsByCourier.get(courierId);
        if (session == null || !session.isOpen()) {
            log.debug("No active WS session for courier {}, skipping push of type={}", courierId, type);
            return;
        }
        try {
            Map<String, Object> envelope = Map.of("type", type, "payload", payload);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
            log.info("Pushed type={} to courier {}", type, courierId);
        } catch (Exception e) {
            log.warn("Failed to push type={} to courier {}: {}", type, courierId, e.getMessage());
        }
    }

    private void markCourierOnline(String courierId) {
        String onlineKey = Objects.requireNonNull("courier:%s:isOnline".formatted(courierId));
        stringRedisTemplate.opsForValue().set(
                onlineKey,
                "true",
                Objects.requireNonNull(positionTtl)
        );
    }

    private void closeQuietly(WebSocketSession session, @NonNull CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.warn("Failed to close session {}: {}", session.getId(), e.getMessage());
        }
    }

    private static Optional<String> getStringAttribute(WebSocketSession session, String key) {
        return Optional.ofNullable(session.getAttributes().get(key))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(s -> !s.isBlank());
    }

    private Optional<String> resolveCourierId(WebSocketSession session) {
        Optional<String> cached = getStringAttribute(session, "courierId");
        if (cached.isPresent()) {
            return cached;
        }
        Optional<String> rawUserId = getStringAttribute(session, "userId");
        if (rawUserId.isEmpty()) {
            return Optional.empty();
        }
        String userId = rawUserId.get();
        try {
            Long uid = Long.parseLong(userId);
            Map<String, Object> profile = userServiceClient.getCourierProfileByUserId(uid);
            Object id = profile == null ? null : profile.get("id");
            if (id != null) {
                String resolved = String.valueOf(id).trim();
                if (!resolved.isBlank()) {
                    session.getAttributes().put("courierId", resolved);
                    return Optional.of(resolved);
                }
            }
        } catch (Exception ex) {
            log.warn("Could not resolve courier profile ID from userId={} (fallback to userId key): {}", userId, ex.getMessage());
        }
        session.getAttributes().put("courierId", userId);
        return Optional.of(userId);
    }

    private static boolean isCourierRole(String role) {
        if (role == null || role.isBlank()) return false;

        // Accepts: "COURIER", "ROLE_COURIER", "ROLE_DELIVERY", "DELIVERY_AGENT",
        // comma/space-separated role lists, etc.
        return Stream.of(role.split("[,\\s]+"))
                .filter(s -> !s.isBlank())
            .map(token -> token.toUpperCase())
            .anyMatch(token -> token.contains("COURIER")
                || token.contains("DELIVERY")
                || token.contains("DRIVER")
                || token.contains("LIVREUR"));
    }

    /**
     * Message WebSocket entrant.
     */
    public record PositionUpdateMessage(String type, PositionPayload payload) {
    }

    /**
     * Payload de position normalisé.
     */
    public record PositionPayload(
            double lat,
            double lng,
            Double accuracy,
            Double speed,
            Double heading,
            Integer batteryLevel,
            String timestamp
    ) {
    }

    /**
     * Évènement interne publié à chaque mise à jour de position.
     * Peut être relayé vers Kafka / PubSub si nécessaire.
     */
    public record CourierPositionUpdatedEvent(
            String courierId,
            PositionPayload payload
    ) {
    }
}

