package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/couriers/{id}")
    Map<String, Object> getCourierById(@PathVariable("id") Long courierId);

    @GetMapping("/couriers/profile")
    Map<String, Object> getCourierProfileByUserId(@RequestHeader("X-User-Id") Long userId);
}
