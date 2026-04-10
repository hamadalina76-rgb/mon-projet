package com.speedline.gateway.security;

import com.speedline.gateway.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Filtre WebFlux chargé de valider et extraire le JWT depuis le header Authorization
 * et de définir le SecurityContext pour les vérifications d'autorisation en aval.
 *
 * IMPORTANT :
 * - Valide la signature et l'expiration du token JWT
 * - Extrait userId, role, email depuis les claims du token
 * - Crée une JwtAuthentication avec authorities ROLE_{role}
 * - Transmet les infos via headers X-User-Id, X-User-Role, X-User-Email aux services en aval
 * - Répond 401 si le token est invalide ou expiré
 */
@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationWebFilter.class);

    private static final List<String> ALLOWED_ORIGINS = List.of(
        "https://admin-panel-392205979525.europe-west1.run.app",
        "https://partner-dashboard-392205979525.europe-west1.run.app",
        "https://courier-app-392205979525.europe-west1.run.app",
        "https://customer-app-392205979525.europe-west1.run.app",
        "http://localhost:4200",
        "http://localhost:4201",
        "http://localhost:4202"
    );

    private final JwtUtil jwtUtil;

    // ✅ Endpoints publics qui ne nécessitent PAS d'authentification
    // ✅ Ces chemins seront ignorés par ce filtre
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/",       // Tous les endpoints /auth (login, register, etc.)
        "/ws/",                 // WebSocket
        "/actuator/health",    // Health check
        "/actuator/info"        // Info endpoint
    );

    public JwtAuthenticationWebFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    @SuppressWarnings("null")
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // ✅ ÉTAPE 1: Vérifier si le chemin est public
        if (isPublicPath(path)) {
            log.debug("Chemin public accepté sans authentification: {}", path);
            return chain.filter(exchange);
        }

        // ✅ ÉTAPE 2: Extraire le header Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token fourni - laisser Spring Security gérer l'unauthorized
            log.warn("Authorization header manquant ou invalide pour: {}", path);
            return chain.filter(exchange);
        }

        // ✅ ÉTAPE 3: Extraire le token sans le préfixe "Bearer "
        String token = authHeader.substring(7);

        try {
            // ✅ ÉTAPE 4: Valider le token JWT (signature + expiration)
            if (!jwtUtil.validateToken(token)) {
                log.error("Token JWT invalide ou expiré pour: {}", path);
                addCorsHeaders(exchange.getResponse().getHeaders(), request.getHeaders().getOrigin());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // ✅ ÉTAPE 5: Extraire les claims du token
            //   - userId: l'ID de l'utilisateur (Long ou String selon la génération)
            //   - role: le rôle de l'utilisateur (ADMIN, SUPER_ADMIN, PARTNER, etc.)
            //   - email: l'email de l'utilisateur
            String userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            String email = jwtUtil.extractEmail(token);
            String fullName = jwtUtil.extractFullName(token);

            String normalizedRole = normalizeRole(role);

            if (userId == null || normalizedRole == null) {
                log.error("Claims JWT incomplètes pour: {}", path);
                addCorsHeaders(exchange.getResponse().getHeaders(), request.getHeaders().getOrigin());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            log.debug("Token valide - userId={}, role={}, email={}, path={}", userId, normalizedRole, email, path);

            // ✅ ÉTAPE 6: Créer l'objet Authentication avec les autorités
            //   - Authority = "ROLE_{role}" (ex: ROLE_ADMIN, ROLE_PARTNER)
            //   - Cela compatible avec hasRole() et hasAuthority() en @PreAuthorize
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + normalizedRole);
            JwtAuthentication authentication = new JwtAuthentication(
                userId,
                email,
                normalizedRole,
                Collections.singletonList(authority)
            );

            // ✅ ÉTAPE 7: Transmettre les infos utilisateur aux services en aval via headers
            //   - X-User-Id: ID de l'utilisateur (pour les jointures BD, logs, etc.)
            //   - X-User-Role: Rôle de l'utilisateur (pour les logs, audit, etc.)
            //   - X-User-Email: Email de l'utilisateur (pour les logs, notifications, etc.)
            ServerHttpRequest.Builder requestBuilder = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", normalizedRole)
                .header("X-User-Email", email);
            if (fullName != null && !fullName.isBlank()) {
                requestBuilder.header("X-User-Name", fullName);
            }
            ServerHttpRequest modifiedRequest = requestBuilder.build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

            // ✅ ÉTAPE 8: Définir le SecurityContext et continuer la chaîne de filtres
            return chain.filter(modifiedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception e) {
            // ⚠️ ERREUR: Token validation failure (invalid signature, parsing error, etc.)
            log.error("Erreur lors de la validation du token JWT pour {}: {}", path, e.getMessage(), e);
            addCorsHeaders(exchange.getResponse().getHeaders(), request.getHeaders().getOrigin());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * Vérifie si le chemin fait partie des endpoints publics (sans authentification)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Normalise le role JWT pour accepter ADMIN et ROLE_ADMIN.
     */
    private String normalizeRole(String rawRole) {
        if (rawRole == null) {
            return null;
        }
        String value = rawRole.trim();
        if (value.isEmpty()) {
            return null;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return upper.startsWith("ROLE_") ? upper.substring(5) : upper;
    }

    /**
     * Ajoute les headers CORS nécessaires pour les réponses d'erreur (401, 403)
     * afin que le navigateur browser puisse même les traiter en cas d'erreur
     */
    private void addCorsHeaders(HttpHeaders headers, String origin) {
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.set(HttpHeaders.VARY, "Origin");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        }
    }

    /**
     * Implémentation personnalisée d'Authentication pour JWT
     * Stocke l'userId, email, et rôle pour un accès facile après la validation
     */
    public static class JwtAuthentication extends UsernamePasswordAuthenticationToken {
        private final String userId;
        private final String email;
        private final String role;

        public JwtAuthentication(String userId, String email, String role, List<SimpleGrantedAuthority> authorities) {
            super(email, null, authorities);
            this.userId = userId;
            this.email = email;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        @Override
        public Object getPrincipal() {
            return email;
        }
    }
}
