package com.speedline.promotion.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", fallback = OrderServiceFallback.class)
public interface OrderServiceClient {

    @GetMapping("/orders/user/{userId}/count")
    long getOrderCount(@PathVariable("userId") Long userId);
}
