package com.speedline.order.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    // TODO: Implémenter
}
