package com.speedline.promotion.validation;

/**
 * Base class that holds the next-link reference and forwards after
 * the concrete check passes.
 */
public abstract class AbstractPromotionValidator implements PromotionValidator {

    private PromotionValidator next;

    @Override
    public void setNext(PromotionValidator next) {
        this.next = next;
    }

    protected void forward(ValidationContext context) {
        if (next != null) {
            next.validate(context);
        }
    }
}
