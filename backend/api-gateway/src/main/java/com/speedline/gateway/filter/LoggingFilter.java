package com.speedline.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = Instant.now().toEpochMilli();
        
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        
        logger.info("[{}] Incoming request: {} {} from {}",
            requestId,
            request.getMethod(),
            request.getURI().getPath(),
            request.getRemoteAddress()
        );

        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                long duration = Instant.now().toEpochMilli() - startTime;
                logger.info("[{}] Response: {} in {}ms",
                    requestId,
                    exchange.getResponse().getStatusCode(),
                    duration
                );
            }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
