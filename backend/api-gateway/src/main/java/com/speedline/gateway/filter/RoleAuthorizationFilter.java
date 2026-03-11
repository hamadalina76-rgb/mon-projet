package com.speedline.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Filtre d'autorisation par rôle - défense en profondeur
 *
 * Objectif : même si le frontend filtre les routes, le backend les rejette:
 * - Admin panel peut accéder à /api/v1/admins/** uniquement si rôle = ADMIN/SUPER_ADMIN
 * - Partner panel peut accéder à /api/partners/** uniquement si rôle = PARTNER/...
 *
 * NOTE IMPORTANTE:
 * - Le JWT a déjà été validé par JwtAuthenticationWebFilter
 * - Le rôle est maintenant disponible dans le header X-User-Role
 *   (rajouté par JwtAuthenticationWebFilter après validation du token)
 * - Nous n'avons PLUS besoin de re-valider le token ici
 *
 * ROUTES PROTÉGÉES:
 * - Admin: /api/v1/admins/**, /api/v1/admin-roles/**, /api/v1/admin/activity-logs/**
 * - Partner: /api/partners/**
 */
@Component
public class RoleAuthorizationFilter extends AbstractGatewayFilterFactory<RoleAuthorizationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(RoleAuthorizationFilter.class);

    private static final List<String> ALLOWED_ORIGINS = List.of(
        "https://admin-panel-392205979525.europe-west1.run.app",
        "https://partner-dashboard-392205979525.europe-west1.run.app",
        "https://courier-app-392205979525.europe-west1.run.app",
        "https://customer-app-392205979525.europe-west1.run.app",
        "http://localhost:4200",
        "http://localhost:4201",
        "http://localhost:4202"
    );

    /** ✅ Routes réservées aux admins uniquement */
    private static final List<String> ADMIN_PATH_PREFIXES = List.of(
        "/api/v1/admins",
        "/api/v1/admin-roles",
        "/api/v1/admin/partners",
        "/api/v1/admin/activity-logs"
    );

    /** ✅ Routes réservées aux partenaires (ou admins qui peuvent tout faire) */
    private static final List<String> PARTNER_PATH_PREFIXES = List.of(
        "/api/partners/"
    );

    /** ✅ Rôles autorisés pour les routes Admin */
    private static final List<String> ADMIN_ROLES = List.of(
        "ADMIN", "SUPER_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN", "CONTENT_MODERATOR"
    );

    /** ✅ Rôles autorisés pour les routes Partner */
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

            // ✅ Ignorer les requêtes OPTIONS (preflight CORS)
            if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                return chain.filter(exchange);
            }

            // ✅ Vérifier s'il y a un header X-User-Role
            // Ce header est rajouté par JwtAuthenticationWebFilter APRÈS validation du token
            String userRole = normalizeRole(request.getHeaders().getFirst("X-User-Role"));

            // ✅ Si pas de rôle (personne non authentifiée), laisser passer
            // Spring Security va rejeter au niveau de @PreAuthorize si nécessaire
            if (userRole == null) {
                log.debug("Pas de header X-User-Role présent pour: {}", path);
                return chain.filter(exchange);
            }

            log.debug("Vérification d'autorisation - rôle: {}, path: {}", userRole, path);

            // ✅ RÈGLE 1: Routes admin - rôle doit être ADMIN ou SUPER_ADMIN
            if (isAdminPath(path) && !ADMIN_ROLES.contains(userRole)) {
                log.warn("Accès REFUSÉ à route admin - rôle {} insuffisant pour {}", userRole, path);
                return onError(exchange, "Accès refusé. Rôle admin requis.", HttpStatus.FORBIDDEN);
            }

            // ✅ RÈGLE 2: Routes partner - rôle doit être PARTNER ou ADMIN (admins = super-users)
            if (isPartnerPath(path)) {
                boolean isPartnerRole = PARTNER_ROLES.contains(userRole);
                boolean isAdminRole = ADMIN_ROLES.contains(userRole);
                
                if (!isPartnerRole && !isAdminRole) {
                    log.warn("Accès REFUSÉ à route partner - rôle {} insuffisant pour {}", userRole, path);
                    return onError(exchange, "Accès refusé. Rôle partenaire ou admin requis.", HttpStatus.FORBIDDEN);
                }
            }

            // ✅ Autorisation OK - continuer
            log.debug("Autorisation OK pour rôle {} sur {}", userRole, path);
            return chain.filter(exchange);
        };
    }

    /**
     * Normalise le role pour accepter ADMIN ou ROLE_ADMIN.
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
     * Vérifie si le chemin est une route réservée aux admins
     */
    private boolean isAdminPath(String path) {
        return ADMIN_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Vérifie si le chemin est une route réservée aux partenaires
     */
    private boolean isPartnerPath(String path) {
        return PARTNER_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Réponse d'erreur avec headers CORS pour que le navigateur puisse traiter
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            exchange.getResponse().getHeaders().set(HttpHeaders.VARY, "Origin");
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            exchange.getResponse().getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        }
        
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
