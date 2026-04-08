package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PartnerRestrictionRule implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.PARTNER_RESTRICTION;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        if (context.getPartnerId() != null && rule.getTargetValue() != null) {
            Set<Long> allowed = Arrays.stream(rule.getTargetValue().split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
            if (!allowed.contains(context.getPartnerId())) {
                throw PromotionException.partnerNotEligible(context.getPromotion().getCode());
            }
        }
    }
}
