package com.speedline.location.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign error for method {}: Status {}, Reason: {}",
                    methodKey, response.status(), response.reason());
            return new RuntimeException(
                    String.format("Failed to call %s. Status: %d, Reason: %s",
                            methodKey, response.status(), response.reason()));
        };
    }
}
