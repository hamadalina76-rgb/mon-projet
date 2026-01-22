package com.speedline.order.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "partner-service")
public interface PartnerServiceClient {
    // TODO: Implémenter
}
