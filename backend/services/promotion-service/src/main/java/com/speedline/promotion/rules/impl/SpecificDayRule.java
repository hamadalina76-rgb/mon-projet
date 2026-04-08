package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * SPECIFIC_DAY — promotion valid only on a specific day of the week.
 * targetValue: "MONDAY", "TUESDAY", etc.
 */
@Component
public class SpecificDayRule implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.SPECIFIC_DAY;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        DayOfWeek required = DayOfWeek.valueOf(rule.getTargetValue().toUpperCase());
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if (required != today) {
            throw new PromotionException("WRONG_DAY",
                    "Ce code n'est valable que le " + required);
        }
    }
}
