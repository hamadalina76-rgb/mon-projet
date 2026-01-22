package com.speedline.partner.client;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign Client pour Location Service
 */
@FeignClient(name = "location-service")
public interface LocationServiceClient {
    // TODO: Implémenter
}
