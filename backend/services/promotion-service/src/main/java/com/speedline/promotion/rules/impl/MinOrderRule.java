package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MinOrderRule implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.MIN_ORDER;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        BigDecimal min = new BigDecimal(rule.getTargetValue());
        if (context.getOrderSubtotal().compareTo(min) < 0) {
            throw PromotionException.minimumOrderNotMet(context.getPromotion().getCode());
        }
    }
}
