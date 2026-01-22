package com.speedline.user.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign Client pour communiquer avec Order Service
 */
@FeignClient(name = "order-service")
public interface OrderServiceClient {
    // TODO: Implémenter les méthodes
}
