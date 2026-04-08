package com.speedline.promotion.dto;

import jakarta.validation.constraints.NotNull;

public record RevokePromotionRequest(

    String code,

    @NotNull
    Long userId,

    @NotNull
    Long orderId
) {}
