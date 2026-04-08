package com.speedline.promotion.validation.validators;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryEligibilityValidatorTest {

    private CategoryEligibilityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CategoryEligibilityValidator();
    }

    @Test
    @DisplayName("Catégorie non éligible → exception")
    void shouldThrowWhenCategoryNotAllowed() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .applicableCategoryIds("[1, 2, 3]").build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .categoryIds(List.of(99L)).build();

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("CATEGORY_NOT_ELIGIBLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Catégorie dans la liste → passe")
    void shouldPassWhenCategoryAllowed() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .applicableCategoryIds("[1, 2, 3]").build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(1L)
                .orderSubtotal(BigDecimal.TEN)
                .categoryIds(List.of(2L)).build();

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
