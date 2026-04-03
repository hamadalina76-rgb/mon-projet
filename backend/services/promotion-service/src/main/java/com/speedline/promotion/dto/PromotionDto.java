package com.speedline.promotion.dto;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionStatus;
import com.speedline.promotion.domain.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionDto(
    Long id,
    String code,
    String name,
    String description,
    PromotionType type,
    BigDecimal value,
    BigDecimal maximumDiscount,
    BigDecimal minimumOrder,
    Integer usageLimitTotal,
    Integer usageCount,
    Integer usageLimitPerUser,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Boolean isActive,
    PromotionStatus status,
    List<Long> applicablePartnerIds,
    List<Long> applicableCategoryIds,
    List<Long> applicableZoneIds,
    Boolean firstOrderOnly,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static PromotionDto from(Promotion p) {
        return new PromotionDto(
            p.getId(),
            p.getCode(),
            p.getName(),
            p.getDescription(),
            p.getType(),
            p.getValue(),
            p.getMaximumDiscount(),
            p.getMinimumOrder(),
            p.getUsageLimit(),
            p.getUsageCount(),
            p.getUsageLimitPerUser(),
            p.getStartDate(),
            p.getEndDate(),
            p.getIsActive(),
            p.getStatus(),
            parseIds(p.getApplicablePartnerIds()),
            parseIds(p.getApplicableCategoryIds()),
            parseIds(p.getApplicableZoneIds()),
            p.getFirstOrderOnly(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }

    public static List<Long> parseIds(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        String trimmed = json.trim().replaceAll("[\\[\\]\\s]", "");
        if (trimmed.isEmpty()) return List.of();
        return java.util.Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
