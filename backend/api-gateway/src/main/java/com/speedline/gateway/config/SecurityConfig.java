package com.speedline.gateway.config;

import com.speedline.gateway.security.JwtAuthenticationWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Spring Cloud Gateway
 * - Enables JWT authentication via JwtAuthenticationWebFilter
 * - Protects all endpoints except /api/v1/auth/**
 * - Configures CORS for frontend origins
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final List<String> ALLOWED_ORIGINS = List.of(
        "https://admin-panel-392205979525.europe-west1.run.app",
        "https://partner-dashboard-392205979525.europe-west1.run.app",
        "https://courier-app-392205979525.europe-west1.run.app",
        "https://customer-app-392205979525.europe-west1.run.app",
        "http://localhost:4200",
        "http://localhost:4201",
        "http://localhost:4202"
    );

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;

    public SecurityConfig(JwtAuthenticationWebFilter jwtAuthenticationWebFilter) {
        this.jwtAuthenticationWebFilter = jwtAuthenticationWebFilter;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Add JWT authentication filter
            .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            
            // CORS configuration
            .cors(Customizer.withDefaults())
            
            // Disable CSRF (stateless JWT authentication)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Allow OPTIONS preflight requests
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Allow WebSocket/SockJS handshake endpoints
                .pathMatchers("/ws/**").permitAll()
                
                // Public authentication endpoints
                .pathMatchers("/api/v1/auth/**").permitAll()
                
                // Public actuator endpoints
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // All other requests require authentication
                .anyExchange().authenticated()
            )
            
            // Disable default login/logout (using JWT, not sessions)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)

            // Ensure 401/403 responses still carry CORS headers for browser clients
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((exchange, e) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    addCorsHeaders(response.getHeaders(), exchange.getRequest().getHeaders().getOrigin());
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response.setComplete();
                })
                .accessDeniedHandler((exchange, e) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    addCorsHeaders(response.getHeaders(), exchange.getRequest().getHeaders().getOrigin());
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return response.setComplete();
                })
            )
            
            .build();
    }

    private void addCorsHeaders(HttpHeaders headers, String origin) {
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.set(HttpHeaders.VARY, "Origin");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
            headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        }
    }
}
