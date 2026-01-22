package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "location-service")
public interface LocationServiceClient {
    // TODO: Implémenter
}
