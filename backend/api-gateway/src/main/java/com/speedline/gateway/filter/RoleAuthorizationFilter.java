package com.speedline.gateway.filter;

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

/**
 * Filtre qui vérifie que le rôle de l'utilisateur (JWT) est autorisé pour la route demandée.
 * Défense en profondeur : même si le frontend filtre, le backend rejette les appels non autorisés.
 *
 * Routes admin (ADMIN, SUPER_ADMIN, etc.) : /api/v1/admins/**, /api/v1/admin/partners/**
 * Routes partenaire (PARTNER, PARTNER_OWNER, etc.) : /api/partners/** (opérations authentifiées)
 */
@Component
public class RoleAuthorizationFilter extends AbstractGatewayFilterFactory<RoleAuthorizationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    /** Routes réservées aux admins (CUSTOMER, COURIER, PARTNER exclus) */
    private static final List<String> ADMIN_PATH_PREFIXES = List.of(
        "/api/v1/admins",
        "/api/v1/admin-roles",
        "/api/v1/admin/partners",
        "/api/v1/admin/activity-logs"
    );

    /** Routes réservées aux partenaires (CUSTOMER, COURIER, ADMIN exclus) */
    private static final List<String> PARTNER_PATH_PREFIXES = List.of(
        "/api/partners/"
    );

    private static final List<String> ADMIN_ROLES = List.of(
        "ADMIN", "SUPER_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN", "CONTENT_MODERATOR"
    );

    private static final List<String> PARTNER_ROLES = List.of(
        "PARTNER", "PARTNER_OWNER", "PARTNER_MANAGER", "PARTNER_STAFF"
    );

    public RoleAuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return chain.filter(exchange);
            }

            String role = jwtUtil.extractRole(token);
            if (role == null) {
                return chain.filter(exchange);
            }

            if (isAdminPath(path) && !ADMIN_ROLES.contains(role)) {
                return onError(exchange, "Access denied. Admin role required.", HttpStatus.FORBIDDEN);
            }

            if (isPartnerPath(path) && !PARTNER_ROLES.contains(role) && !ADMIN_ROLES.contains(role)) {
                return onError(exchange, "Access denied. Partner or Admin role required.", HttpStatus.FORBIDDEN);
            }

            return chain.filter(exchange);
        };
    }

    private boolean isAdminPath(String path) {
        return ADMIN_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isPartnerPath(String path) {
        return PARTNER_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"message\":\"" + message + "\"}").getBytes();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    public static class Config {
    }
}
