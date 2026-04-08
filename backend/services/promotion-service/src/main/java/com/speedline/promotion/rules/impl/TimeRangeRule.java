package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * TIME_RANGE — promotion valid only within a time window.
 * targetValue format: "11:00-14:00"
 */
@Component
public class TimeRangeRule implements RuleEvaluator {

    @Override
    public RuleType supportedType() {
        return RuleType.TIME_RANGE;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        String[] parts = rule.getTargetValue().split("-");
        LocalTime start = LocalTime.parse(parts[0].trim());
        LocalTime end = LocalTime.parse(parts[1].trim());
        LocalTime now = LocalTime.now();

        if (now.isBefore(start) || now.isAfter(end)) {
            throw new PromotionException("OUTSIDE_TIME_RANGE",
                    "Ce code n'est valable qu'entre " + start + " et " + end);
        }
    }
}
