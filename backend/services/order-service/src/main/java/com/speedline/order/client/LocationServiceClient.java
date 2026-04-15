package com.speedline.order.client;

import com.speedline.order.client.dto.ZoneSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "location-service", url = "${LOCATION_SERVICE_URL:http://localhost:8088}")
public interface LocationServiceClient {

    @GetMapping("/zones/find")
    ZoneSnapshot findZoneForPoint(@RequestParam("latitude") java.math.BigDecimal latitude,
                                  @RequestParam("longitude") java.math.BigDecimal longitude);
}
