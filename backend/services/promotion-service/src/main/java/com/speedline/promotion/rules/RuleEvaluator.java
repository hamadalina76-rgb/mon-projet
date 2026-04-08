package com.speedline.promotion.rules;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.validation.ValidationContext;

/**
 * Strategy pattern — each RuleType has its own evaluator.
 */
public interface RuleEvaluator {

    RuleType supportedType();

    void evaluate(PromotionRule rule, ValidationContext context);
}
