package com.speedline.promotion.service;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.domain.PromotionUsageLog;
import com.speedline.promotion.dto.ApplyPromotionRequest;
import com.speedline.promotion.dto.RevokePromotionRequest;
import com.speedline.promotion.dto.ValidatePromotionRequest;
import com.speedline.promotion.dto.ValidatePromotionResponse;
import com.speedline.promotion.event.PromotionEventPublisher;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.repository.PromotionRepository;
import com.speedline.promotion.repository.PromotionRuleRepository;
import com.speedline.promotion.repository.PromotionAuditLogRepository;
import com.speedline.promotion.repository.PromotionUsageLogRepository;
import com.speedline.promotion.repository.UserPromotionRepository;
import com.speedline.promotion.service.impl.PromotionServiceImpl;
import com.speedline.promotion.validation.ValidationChain;
import com.speedline.promotion.validation.ValidationContext;
import com.speedline.promotion.service.RedisPromotionCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionApplicationServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionRuleRepository ruleRepository;
    @Mock private PromotionUsageLogRepository usageLogRepository;
    @Mock private PromotionAuditLogRepository auditLogRepository;
    @Mock private UserPromotionRepository userPromotionRepository;
    @Mock private PromotionEventPublisher eventPublisher;
    @Mock private ValidationChain validationChain;
    @Mock private DiscountCalculator discountCalculator;
    @Mock private RedisQuotaService redisQuotaService;
    @Mock private RedisPromotionCacheService cacheService;

    private PromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromotionServiceImpl(
                promotionRepository,
                ruleRepository,
                usageLogRepository,
            auditLogRepository,
                userPromotionRepository,
                eventPublisher,
                new ObjectMapper(),
                validationChain,
                discountCalculator,
                redisQuotaService,
                cacheService
        );
    }

    // --------------------------------------------------------- helpers

    private Promotion validPromo() {
        return Promotion.builder()
                .id(1L).code("PROMO25").type(PromotionType.PERCENTAGE)
                .value(new BigDecimal("25")).isActive(true)
                .usageLimit(100).usageCount(10)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
    }

    // ----------------------------------------------------- Cas 1

    @Test
    @DisplayName("Cas 1 — Code valide, toutes conditions OK → is_valid: true, discount calculé")
    void validateAllConditionsOk() {
        Promotion p = validPromo();
        when(promotionRepository.findByCode("PROMO25")).thenReturn(Optional.of(p));
        doNothing().when(validationChain).validate(any(ValidationContext.class));
        when(discountCalculator.compute(eq(p), any(), any())).thenReturn(new BigDecimal("5.00"));

        ValidatePromotionResponse resp = service.validate(
                new ValidatePromotionRequest("PROMO25", 1L, new BigDecimal("20"), BigDecimal.ZERO, null, null, null, null));

        assertTrue(resp.isValid());
        assertEquals(new BigDecimal("5.00"), resp.discountAmount());
    }

    // ----------------------------------------------------- Cas 14

    @Test
    @DisplayName("Cas 14 — Apply concurrent, quota restant = 1 → un seul réussit")
    void applyConcurrentQuotaOne() {
        Promotion p = validPromo();
        p.setUsageLimit(1);
        p.setUsageCount(0);

        when(promotionRepository.findByCode("PROMO25")).thenReturn(Optional.of(p));
        doNothing().when(validationChain).validate(any(ValidationContext.class));

        // First call succeeds
        when(redisQuotaService.tryConsume(1L, 1, 0)).thenReturn(true).thenReturn(false);
        when(discountCalculator.compute(eq(p), any(), any())).thenReturn(new BigDecimal("5.00"));

        ApplyPromotionRequest req1 = new ApplyPromotionRequest("PROMO25", 1L, 100L,
                new BigDecimal("20"), BigDecimal.ZERO, null, null, null);

        ValidatePromotionResponse resp = service.apply(req1);
        assertTrue(resp.isValid());

        // Second call fails
        ApplyPromotionRequest req2 = new ApplyPromotionRequest("PROMO25", 2L, 101L,
                new BigDecimal("20"), BigDecimal.ZERO, null, null, null);

        assertThrows(PromotionException.class, () -> service.apply(req2));
    }

    // ----------------------------------------------------- Cas 15

    @Test
    @DisplayName("Cas 15 — Revoke après apply → compteurs décrémentés")
    void revokeDecrements() {
        Promotion p = validPromo();
        when(promotionRepository.findByCode("PROMO25")).thenReturn(Optional.of(p));
        when(usageLogRepository.revoke(eq(1L), eq(1L), eq(100L), any(LocalDateTime.class))).thenReturn(1);

        service.revoke(new RevokePromotionRequest("PROMO25", 1L, 100L));

        verify(promotionRepository).decrementUsageCount(1L);
        verify(redisQuotaService).release(1L);
        verify(eventPublisher).publishPromotionRevoked(1L, "PROMO25", 1L, 100L);
    }

    // ----------------------------------------------------- Cas 16

    @Test
    @DisplayName("Cas 16 — Validate ne change aucun compteur")
    void validateDoesNotChangeCounters() {
        Promotion p = validPromo();
        when(promotionRepository.findByCode("PROMO25")).thenReturn(Optional.of(p));
        doNothing().when(validationChain).validate(any(ValidationContext.class));
        when(discountCalculator.compute(eq(p), any(), any())).thenReturn(new BigDecimal("5.00"));

        service.validate(
                new ValidatePromotionRequest("PROMO25", 1L, new BigDecimal("20"), BigDecimal.ZERO, null, null, null, null));

        verify(promotionRepository, never()).incrementUsageCount(anyLong());
        verify(promotionRepository, never()).decrementUsageCount(anyLong());
        verify(userPromotionRepository, never()).upsertUsage(anyLong(), anyLong());
        verify(usageLogRepository, never()).save(any(PromotionUsageLog.class));
        verify(redisQuotaService, never()).tryConsume(anyLong(), anyInt(), anyInt());
        verify(redisQuotaService, never()).release(anyLong());
    }

    // ----------------------------------------------------- Apply happy path saves log

    @Test
    @DisplayName("Apply enregistre un usage log APPLIED")
    void applyCreatesUsageLog() {
        Promotion p = validPromo();
        when(promotionRepository.findByCode("PROMO25")).thenReturn(Optional.of(p));
        doNothing().when(validationChain).validate(any(ValidationContext.class));
        when(redisQuotaService.tryConsume(1L, 100, 10)).thenReturn(true);
        when(discountCalculator.compute(eq(p), any(), any())).thenReturn(new BigDecimal("5.00"));

        service.apply(new ApplyPromotionRequest("PROMO25", 1L, 100L,
                new BigDecimal("20"), BigDecimal.ZERO, null, null, null));

        ArgumentCaptor<PromotionUsageLog> captor = ArgumentCaptor.forClass(PromotionUsageLog.class);
        verify(usageLogRepository).save(captor.capture());

        PromotionUsageLog log = captor.getValue();
        assertEquals(1L, log.getPromotionId());
        assertEquals(1L, log.getUserId());
        assertEquals(100L, log.getOrderId());
        assertEquals(new BigDecimal("5.00"), log.getDiscountAmount());
        assertEquals(PromotionUsageLog.UsageStatus.APPLIED, log.getStatus());
    }
}
