package com.speedline.promotion.dto;

import com.speedline.promotion.domain.PromotionType;

import java.math.BigDecimal;

public record ValidatePromotionResponse(
    boolean isValid,
    Long promotionId,
    BigDecimal discountAmount,
    PromotionType discountType,
    BigDecimal originalSubtotal,
    BigDecimal newSubtotal,
    BigDecimal deliveryFee,
    BigDecimal total,
    String message
) {}
