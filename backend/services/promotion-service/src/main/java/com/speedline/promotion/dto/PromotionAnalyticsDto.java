package com.speedline.promotion.dto;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;

public record PromotionAnalyticsDto(
    Long promotionId,
    String code,
    String name,
    long totalApplied,
    long totalRevoked,
    long netUsage,
    BigDecimal totalDiscountGiven,
    long uniqueUsers,
    BigDecimal avgDiscountPerUse,
    Integer usageLimit,
    double usageRatePct,
    List<UsageLogEntry> usageLogs
) {
    public record UsageLogEntry(
        Long userId,
        Long orderId,
        BigDecimal discountAmount,
        String status,
        LocalDateTime createdAt
    ) {}
}
