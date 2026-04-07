package com.speedline.order.client.dto.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidateRequest {

    private String code;
    private Long userId;
    private BigDecimal orderSubtotal;
    private BigDecimal deliveryFee;
    private Long partnerId;
    private List<Long> categoryIds;
    private List<Long> productIds;
}
