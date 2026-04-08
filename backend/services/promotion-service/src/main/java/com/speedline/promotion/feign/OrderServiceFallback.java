package com.speedline.promotion.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fail-open fallback: if Order Service is down, returns 0
 * so FIRST_ORDER rule is ignored (lets the user through).
 */
@Component
@Slf4j
public class OrderServiceFallback implements OrderServiceClient {

    @Override
    public long getOrderCount(Long userId) {
        log.warn("[Feign Fallback] Order Service unavailable — fail-open for userId={}", userId);
        return 0;
    }
}
