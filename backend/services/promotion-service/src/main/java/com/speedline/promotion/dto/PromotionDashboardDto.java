package com.speedline.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionDashboardDto(
    // KPIs
    long activeCount,
    long usagesThisMonth,
    BigDecimal totalDiscountTnd,
    double usageRatePct,

    // Top 5 promotions (bar chart)
    List<TopPromotion> top5,

    // Daily usage last 30 days (line chart)
    List<DailyUsage> dailyUsages,

    // Alerts: promos expiring within 7 days
    List<ExpiringPromotion> expiringAlerts
) {
    public record TopPromotion(
        Long id,
        String code,
        String name,
        long usageCount
    ) {}

    public record DailyUsage(
        LocalDate date,
        long count
    ) {}

    public record ExpiringPromotion(
        Long id,
        String code,
        String name,
        LocalDateTime endDate,
        long daysRemaining
    ) {}
}
