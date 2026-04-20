package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/orders/{id}")
    Map<String, Object> getOrderById(@PathVariable("id") Long orderId);
}
