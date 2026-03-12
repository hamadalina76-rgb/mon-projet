package com.speedline.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Désactive la protection CSRF pour l'API Gateway.
 * Les API REST utilisent JWT (stateless), pas de session/cookies → CSRF non applicable.
 * Sans cette config, les requêtes POST (login, etc.) reçoivent 403 "An expected CSRF token cannot be found".
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
