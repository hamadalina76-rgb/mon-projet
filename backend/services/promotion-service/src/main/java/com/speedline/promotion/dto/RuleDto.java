package com.speedline.promotion.dto;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;

public record RuleDto(
    Long id,
    RuleType ruleType,
    String operator,
    String targetValue
) {
    public static RuleDto from(PromotionRule r) {
        return new RuleDto(r.getId(), r.getRuleType(), r.getOperator(), r.getTargetValue());
    }
}
