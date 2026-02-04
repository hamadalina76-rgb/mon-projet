package com.speedline.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * Résolveur par défaut pour le rate‑limiting.
     * On le marque @Primary pour que Spring Cloud Gateway
     * l'injecte comme seul KeyResolver global, même si d'autres
     * beans de type KeyResolver existent.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "anonymous";
            return Mono.just(ip);
        };
    }

    /**
     * Résolveur basé uniquement sur l'adresse IP.
     * Peut être utilisé explicitement par nom de bean dans la config si besoin.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "anonymous"
        );
    }
}
