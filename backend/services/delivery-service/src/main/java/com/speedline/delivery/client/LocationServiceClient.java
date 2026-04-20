package com.speedline.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "location-service")
public interface LocationServiceClient {

        @GetMapping("/zones/find")
        Map<String, Object> findZoneForPoint(@RequestParam("latitude") BigDecimal latitude,
                                                                                 @RequestParam("longitude") BigDecimal longitude);

        @GetMapping("/zones/{id}")
        Map<String, Object> getZoneById(@PathVariable("id") Long zoneId);
}
