package com.speedline.promotion.scheduler;

import com.speedline.promotion.domain.Promotion;
import com.speedline.promotion.domain.PromotionStatus;
import com.speedline.promotion.event.PromotionEventPublisher;
import com.speedline.promotion.repository.PromotionRepository;
import com.speedline.promotion.service.RedisPromotionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionExpirationScheduler {

    private final PromotionRepository promotionRepository;
    private final RedisPromotionCacheService cacheService;
    private final PromotionEventPublisher eventPublisher;

    /**
     * Every hour: expire promotions whose end_date has passed.
     * Removes from Redis cache + publishes promotion-expired event.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void expirePromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> expiring = promotionRepository.findExpirablePromotions(now);
        if (!expiring.isEmpty()) {
            for (Promotion p : expiring) {
                cacheService.removeCode(p.getCode());
                eventPublisher.publishPromotionExpired(p.getId(), p.getCode());
            }
            int count = promotionRepository.expirePromotions(now);
            log.info("[Scheduler] {} promotion(s) expired", count);
        }
    }

    /**
     * Every hour: activate SCHEDULED promotions whose start_date has arrived.
     * Adds to Redis cache.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void activateScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> toActivate = promotionRepository.findScheduledToActivate(now);
        for (Promotion p : toActivate) {
            p.setStatus(PromotionStatus.ACTIVE);
            p.setIsActive(true);
            promotionRepository.save(p);
            cacheService.addCode(p.getCode());
            log.info("[Scheduler] Promotion activated: {}", p.getCode());
        }
    }
}
