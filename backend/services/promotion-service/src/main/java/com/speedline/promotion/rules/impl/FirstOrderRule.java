package com.speedline.promotion.rules.impl;

import com.speedline.promotion.domain.PromotionRule;
import com.speedline.promotion.domain.RuleType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.feign.OrderServiceClient;
import com.speedline.promotion.rules.RuleEvaluator;
import com.speedline.promotion.validation.ValidationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FIRST_ORDER — calls Order Service via Feign to check user's order count.
 * Fail-open: if Order Service is down, the rule is ignored (fallback returns 0).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FirstOrderRule implements RuleEvaluator {

    private final OrderServiceClient orderServiceClient;

    @Override
    public RuleType supportedType() {
        return RuleType.FIRST_ORDER;
    }

    @Override
    public void evaluate(PromotionRule rule, ValidationContext context) {
        if (context.getUserId() == null) return;

        long orderCount = orderServiceClient.getOrderCount(context.getUserId());
        if (orderCount > 0) {
            throw new PromotionException("FIRST_ORDER_ONLY",
                    "Ce code est réservé aux premières commandes");
        }
    }
}
