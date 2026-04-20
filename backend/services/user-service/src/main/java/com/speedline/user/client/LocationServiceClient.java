package com.speedline.user.client;

import com.speedline.user.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for location-service zone synchronization.
 */
@FeignClient(name = "location-service", configuration = FeignConfig.class)
public interface LocationServiceClient {

    @PostMapping("/zones/sync-courier-zones")
    void syncCourierZones(@RequestParam("courierId") Long courierId,
                          @RequestParam("zoneIds") List<Long> zoneIds);
}
