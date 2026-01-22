package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    // TODO: Implémenter pour récupérer infos courier
}
