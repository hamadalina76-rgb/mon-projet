package com.speedline.promotion.dto;

import java.math.BigDecimal;

public record PromotionStatisticsDto(
    long activeCount,
    long inactiveCount,
    long expiredCount,
    long totalPromotions,
    long totalUsages,
    BigDecimal totalDiscount
) {}
