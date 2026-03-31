package com.speedline.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-2)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = determineHttpStatus(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("status", status.value());
        payload.put("error", status.getReasonPhrase());
        payload.put("message", ex.getMessage());
        payload.put("path", exchange.getRequest().getURI().getPath());

        String errorResponse;
        try {
            errorResponse = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException jsonEx) {
            // Fallback JSON to avoid leaking malformed payloads to clients.
            errorResponse = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Gateway serialization error\"}";
        }

        byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus determineHttpStatus(Throwable ex) {
        if (ex instanceof org.springframework.web.server.ResponseStatusException) {
            return HttpStatus.valueOf(((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
