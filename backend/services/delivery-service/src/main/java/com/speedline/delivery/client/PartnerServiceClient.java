package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "partner-service")
public interface PartnerServiceClient {

    @GetMapping("/partners/{id}")
    Map<String, Object> getPartnerById(@PathVariable("id") Long partnerId);
}
