package com.speedline.promotion.validation.validators;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.domain.UserPromotion;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.repository.UserPromotionRepository;
import com.speedline.promotion.validation.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQuotaValidatorTest {

    @Mock
    private UserPromotionRepository userPromotionRepository;

    private UserQuotaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UserQuotaValidator(userPromotionRepository);
    }

    @Test
    @DisplayName("Cas 7 — Quota user atteint → 'Vous avez déjà utilisé ce code'")
    void shouldThrowWhenUserQuotaReached() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .usageLimitPerUser(1).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(42L)
                .orderSubtotal(BigDecimal.TEN).build();

        UserPromotion up = UserPromotion.builder().usageCount(1).build();
        when(userPromotionRepository.findByUserIdAndPromotionId(42L, 1L))
                .thenReturn(Optional.of(up));

        PromotionException ex = assertThrows(PromotionException.class,
                () -> validator.validate(ctx));
        assertEquals("USER_QUOTA_EXCEEDED", ex.getErrorCode());
    }

    @Test
    @DisplayName("User n'a jamais utilisé → passe")
    void shouldPassWhenNeverUsed() {
        Promotion p = Promotion.builder()
                .id(1L).code("PROMO10").type(PromotionType.PERCENTAGE).isActive(true)
                .usageLimitPerUser(3).build();
        ValidationContext ctx = ValidationContext.builder()
                .promotion(p).userId(42L)
                .orderSubtotal(BigDecimal.TEN).build();

        when(userPromotionRepository.findByUserIdAndPromotionId(42L, 1L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(ctx));
    }
}
