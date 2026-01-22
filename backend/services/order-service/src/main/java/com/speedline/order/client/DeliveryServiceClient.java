package com.speedline.order.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "delivery-service")
public interface DeliveryServiceClient {
    // TODO: Implémenter
}
