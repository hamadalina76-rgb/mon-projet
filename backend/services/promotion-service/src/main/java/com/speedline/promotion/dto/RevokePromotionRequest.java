package com.speedline.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevokePromotionRequest(

    @NotBlank
    String code,

    @NotNull
    Long userId,

    @NotNull
    Long orderId
) {}
