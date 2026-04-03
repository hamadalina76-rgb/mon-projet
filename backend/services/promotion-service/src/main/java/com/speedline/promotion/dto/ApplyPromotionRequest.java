package com.speedline.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ApplyPromotionRequest(

    @NotBlank
    String code,

    @NotNull
    Long userId,

    @NotNull
    Long orderId,

    @NotNull
    BigDecimal orderSubtotal,

    BigDecimal deliveryFee,

    Long partnerId,

    List<Long> categoryIds
) {}
