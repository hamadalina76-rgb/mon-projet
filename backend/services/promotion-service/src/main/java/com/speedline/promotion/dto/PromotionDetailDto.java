package com.speedline.promotion.dto;

import java.math.BigDecimal;

public record PromotionDetailDto(
    PromotionDto promotion,
    long totalApplied,
    long totalRevoked,
    BigDecimal totalRevenue,
    long uniqueUsers
) {}
