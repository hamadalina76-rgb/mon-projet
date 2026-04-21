package com.speedline.promotion.scheduler;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionStatus;
import com.speedline.promotion.domain.PromotionType;
import com.speedline.promotion.event.PromotionEventPublisher;
import com.speedline.promotion.repository.PromotionAuditLogRepository;
import com.speedline.promotion.repository.PromotionRepository;
import com.speedline.promotion.service.RedisPromotionCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionExpirationSchedulerTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionAuditLogRepository auditLogRepository;
    @Mock private RedisPromotionCacheService cacheService;
    @Mock private PromotionEventPublisher eventPublisher;

    private PromotionExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PromotionExpirationScheduler(promotionRepository, auditLogRepository, cacheService, eventPublisher);
    }

    @Test
    @DisplayName("Cas 7 — Promo expirée: status EXPIRED, retiré de Redis, event publié")
    void expirePromotionsRemovesFromRedisAndPublishesEvent() {
        Promotion p = Promotion.builder()
                .id(1L).code("EXPIRED10").type(PromotionType.PERCENTAGE)
                .isActive(true).status(PromotionStatus.ACTIVE)
                .endDate(LocalDateTime.now().minusHours(1))
                .build();
        when(promotionRepository.findExpirablePromotions(any(LocalDateTime.class)))
                .thenReturn(List.of(p));
        when(promotionRepository.expirePromotions(any(LocalDateTime.class))).thenReturn(1);

        scheduler.expirePromotions();

        verify(cacheService).removeCode("EXPIRED10");
        verify(eventPublisher).publishPromotionExpired(1L, "EXPIRED10");
        verify(promotionRepository).expirePromotions(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Scheduler activates SCHEDULED promo and adds to Redis")
    void activateScheduledPromotionsAddsToRedis() {
        Promotion p = Promotion.builder()
                .id(2L).code("SCHED10").type(PromotionType.PERCENTAGE)
                .isActive(false).status(PromotionStatus.SCHEDULED)
                .startDate(LocalDateTime.now().minusMinutes(5))
                .build();
        when(promotionRepository.findScheduledToActivate(any(LocalDateTime.class)))
                .thenReturn(List.of(p));

        scheduler.activateScheduledPromotions();

        verify(cacheService).addCode("SCHED10");
        verify(promotionRepository).save(p);
        assert p.getStatus() == PromotionStatus.ACTIVE;
        assert p.getIsActive();
    }
}
