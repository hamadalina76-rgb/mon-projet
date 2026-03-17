package com.speedline.delivery.websocket;

import com.speedline.delivery.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Handshake interceptor that validates JWT for WebSocket connections.
 * Accepts token from:
 * - Authorization: Bearer &lt;jwt&gt; (header)
 * - ?token=&lt;jwt&gt; (query param, for mobile clients that cannot set headers)
 * On success, stores userId (courierId) and role in session attributes.
 */
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(
            org.springframework.http.server.ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String token = null;

        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null || token.isBlank()) {
            var queryParams = request.getURI().getQuery();
            if (queryParams != null) {
                for (String pair : queryParams.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "token".equalsIgnoreCase(pair.substring(0, eq).trim())) {
                        token = pair.substring(eq + 1).trim();
                        break;
                    }
                }
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake refused: missing token (header Authorization: Bearer <jwt> or ?token=<jwt>)");
            return false;
        }
        if (!jwtUtil.validateToken(token)) {
            log.warn("WebSocket handshake refused: invalid JWT");
            return false;
        }

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        attributes.put("userId", userId);
        attributes.put("role", role);

        log.info("WebSocket handshake accepted for userId={}, role={}", userId, role);
        return true;
    }

    @Override
    public void afterHandshake(
            org.springframework.http.server.ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }
}

