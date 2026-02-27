package com.speedline.gateway.filter;

import com.speedline.gateway.security.TokenBlacklistChecker;
import com.speedline.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistChecker tokenBlacklistChecker;

    private static final List<String> OPEN_ENDPOINTS = List.of(
        "/auth/login",
        "/auth/admin/login",
        "/auth/register",
        "/auth/refresh",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/auth/verify-email",
        "/auth/check-email",
        "/auth/health",
        "/partners",
        "/categories",
        "/products"
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Check if endpoint is open
            if (isOpenEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Check for Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.validateToken(token)) {
                    return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                }

                // Extract user info
                String userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);

                // Vérification Redis (blacklist + user bloqué)
                return tokenBlacklistChecker.isBlacklisted(token)
                        .flatMap(isBlacklisted -> {
                            if (isBlacklisted) {
                                return onError(exchange, "Token blacklisted", HttpStatus.UNAUTHORIZED);
                            }
                            return tokenBlacklistChecker.isUserBlocked(userId)
                                    .flatMap(isBlocked -> {
                                        if (isBlocked) {
                                            return onError(exchange, "User blocked", HttpStatus.UNAUTHORIZED);
                                        }

                                        ServerHttpRequest modifiedRequest = request.mutate()
                                                .header("X-User-Id", userId)
                                                .header("X-User-Role", role)
                                                .build();

                                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                    });
                        })
                        .onErrorResume(e -> {
                            // Mode dégradé : si Redis tombe, on ignore la blacklist et on continue
                            ServerHttpRequest modifiedRequest = request.mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Role", role)
                                    .build();
                            return chain.filter(exchange.mutate().request(modifiedRequest).build());
                        });

            } catch (Exception e) {
                return onError(exchange, "Token validation failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::contains);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
