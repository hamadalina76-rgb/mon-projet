package com.speedline.promotion.validation;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.exception.PromotionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationChainTest {

    @Test
    @DisplayName("Chain exécute les validators dans l'ordre chaîné")
    void executesInOrder() {
        List<String> trace = new ArrayList<>();

        AbstractPromotionValidator v1 = new TracingValidator(trace, "A");
        AbstractPromotionValidator v2 = new TracingValidator(trace, "B");
        AbstractPromotionValidator v3 = new TracingValidator(trace, "C");

        // Wire manually
        v1.setNext(v2);
        v2.setNext(v3);

        Promotion p = Promotion.builder().id(1L).code("X").type(PromotionType.PERCENTAGE).isActive(true).build();
        v1.validate(ValidationContext.builder().promotion(p).userId(1L).orderSubtotal(BigDecimal.TEN).build());

        assertEquals(List.of("A", "B", "C"), trace);
    }

    @Test
    @DisplayName("Chain s'arrête au premier échec")
    void stopsOnFirstFailure() {
        List<String> trace = new ArrayList<>();

        AbstractPromotionValidator v1 = new TracingValidator(trace, "A");
        AbstractPromotionValidator failing = new FailingValidator();
        AbstractPromotionValidator v3 = new TracingValidator(trace, "C");

        v1.setNext(failing);
        failing.setNext(v3);

        Promotion p = Promotion.builder().id(1L).code("X").type(PromotionType.PERCENTAGE).isActive(true).build();
        assertThrows(PromotionException.class, () ->
            v1.validate(ValidationContext.builder().promotion(p).userId(1L).orderSubtotal(BigDecimal.TEN).build()));

        assertEquals(List.of("A"), trace); // C never reached
    }

    // ------ stubs ------

    static class TracingValidator extends AbstractPromotionValidator {
        private final List<String> trace;
        private final String label;

        TracingValidator(List<String> trace, String label) {
            this.trace = trace;
            this.label = label;
        }

        @Override
        public void validate(ValidationContext context) {
            trace.add(label);
            forward(context);
        }
    }

    static class FailingValidator extends AbstractPromotionValidator {
        @Override
        public void validate(ValidationContext context) {
            throw new PromotionException("FAIL", "stub failure");
        }
    }
}
