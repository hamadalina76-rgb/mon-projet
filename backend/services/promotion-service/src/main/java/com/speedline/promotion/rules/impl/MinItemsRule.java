package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.stereotype.Component;

/**
 * MIN_ITEMS — minimum number of items in the order.
 * targetValue: e.g. "3"
 */
@Component
public class MinItemsRule implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.MIN_ITEMS;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        int minItems = Integer.parseInt(rule.getTargetValue());
        int actual = context.getItemCount() != null ? context.getItemCount() : 0;
        if (actual < minItems) {
            throw new PromotionException("MIN_ITEMS_NOT_MET",
                    "Minimum " + minItems + " articles requis");
        }
    }
}
