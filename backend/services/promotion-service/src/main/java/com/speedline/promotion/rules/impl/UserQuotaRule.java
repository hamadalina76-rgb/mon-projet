package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.stereotype.Component;

/**
 * USER_QUOTA is already handled by UserQuotaValidator (maillon 5) — no-op.
 */
@Component
public class UserQuotaRule implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.USER_QUOTA;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        // Handled by UserQuotaValidator — intentional no-op
    }
}
