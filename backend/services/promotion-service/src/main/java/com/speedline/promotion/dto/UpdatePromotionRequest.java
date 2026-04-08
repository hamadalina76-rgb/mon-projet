package com.speedline.promotion.dto;

import com.speedline.promotion.domain.PromotionStatus;
import com.speedline.promotion.domain.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.speedline.promotion.domain.RuleType;

public record UpdatePromotionRequest(

    String name,

    String description,

    PromotionType type,

    BigDecimal value,

    @DecimalMin(value = "0")
    BigDecimal maximumDiscount,

    @DecimalMin(value = "0")
    BigDecimal minimumOrder,

    @Min(1)
    Integer usageLimitTotal,

    @Min(1)
    Integer usageLimitPerUser,

    LocalDateTime startDate,

    LocalDateTime endDate,

    List<Long> applicablePartnerIds,

    List<Long> applicableCategoryIds,

    List<Long> applicableZoneIds,

    Boolean firstOrderOnly,

    PromotionStatus status,

    List<RuleRequest> rules
) {
    public record RuleRequest(
        RuleType ruleType,
        String operator,
        String targetValue
    ) {}
}
