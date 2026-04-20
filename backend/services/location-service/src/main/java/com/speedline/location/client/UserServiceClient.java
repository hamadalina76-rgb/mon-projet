package com.speedline.location.client;

import com.speedline.location.config.FeignConfig;
import com.speedline.location.dto.ZoneCourierCountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserServiceClient {

    @GetMapping("/v1/admin/couriers/zones/courier-counts")
    List<ZoneCourierCountDTO> getZoneCourierCounts(@RequestParam("zoneIds") List<Long> zoneIds);
}
