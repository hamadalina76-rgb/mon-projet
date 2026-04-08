package com.speedline.order.client;

import com.speedline.order.client.dto.promotion.PromotionApiResponse;
import com.speedline.order.client.dto.promotion.PromotionApplyRequest;
import com.speedline.order.client.dto.promotion.PromotionValidateRequest;
import com.speedline.order.client.dto.promotion.PromotionValidateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "promotion-service")
public interface PromotionServiceClient {

    @PostMapping("/promotions/validate")
    PromotionApiResponse<PromotionValidateResponse> validatePromotion(
            @RequestBody PromotionValidateRequest request
    );

    @PostMapping("/promotions/{code}/apply")
    PromotionApiResponse<PromotionValidateResponse> applyPromotion(
            @PathVariable("code") String code,
            @RequestBody PromotionApplyRequest request
    );
}
