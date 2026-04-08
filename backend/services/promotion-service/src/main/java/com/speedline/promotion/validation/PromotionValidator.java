package com.speedline.promotion.validation;

/**
 * Chain of Responsibility — each validator checks one condition
 * and delegates to the next link in the chain.
 */
public interface PromotionValidator {

    void validate(ValidationContext context);

    void setNext(PromotionValidator next);
}
