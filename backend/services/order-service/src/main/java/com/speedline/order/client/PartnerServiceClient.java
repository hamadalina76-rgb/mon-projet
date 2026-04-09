package com.speedline.order.client;

import com.speedline.order.client.dto.PartnerSnapshot;
import com.speedline.order.client.dto.ProductSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "partner-service", url = "${PARTNER_SERVICE_URL:http://localhost:8083}")
public interface PartnerServiceClient {

    @GetMapping("/partners/{partnerId}")
    PartnerSnapshot getPartnerById(@PathVariable("partnerId") Long partnerId);

    @GetMapping("/partners/{partnerId}/menu/products/{productId}")
    ProductSnapshot getProductById(
            @PathVariable("partnerId") Long partnerId,
            @PathVariable("productId") Long productId
    );
}
