package com.speedline.promotion.validation;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Builds and executes the Chain of Responsibility.
 * All {@link PromotionValidator} beans are auto-wired and sorted by their
 * {@link Order @Order} annotation.
 */
@Component
public class ValidationChain {

    private final PromotionValidator head;

    public ValidationChain(List<PromotionValidator> validators) {
        // Sort by @Order value
        validators.sort(Comparator.comparingInt(v -> {
            Order order = v.getClass().getAnnotation(Order.class);
            return order != null ? order.value() : Integer.MAX_VALUE;
        }));

        // Wire the chain: 1 → 2 → … → 9
        for (int i = 0; i < validators.size() - 1; i++) {
            validators.get(i).setNext(validators.get(i + 1));
        }
        this.head = validators.isEmpty() ? null : validators.get(0);
    }

    /**
     * Runs every validator in order.
     * The first failing validator throws a {@link com.speedline.promotion.exception.PromotionException}.
     */
    public void validate(ValidationContext context) {
        if (head != null) {
            head.validate(context);
        }
    }
}
