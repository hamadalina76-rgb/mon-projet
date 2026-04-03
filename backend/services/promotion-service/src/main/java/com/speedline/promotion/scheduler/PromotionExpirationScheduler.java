package com.speedline.promotion.scheduler;

import com.speedline.promotion.domain.PromotionStatus;
import com.speedline.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionExpirationScheduler {

    private final PromotionRepository promotionRepository;

    /**
     * Every hour: deactivate promotions whose end_date has passed.
     */
    @Scheduled(fixedRate = 300000)  // every 5 minutes
    @Transactional
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void expirePromotions() {
        LocalDateTime now = LocalDateTime.now();
        int expired = promotionRepository.expirePromotions(now);
        if (expired > 0) {
            log.info("[Scheduler] {} promotion(s) expired", expired);
        }
    }

    /**
     * Every hour: activate SCHEDULED promotions whose start_date has arrived.
     */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void activateScheduledPromotions() {
        LocalDateTime now = LocalDateTime.now();
        promotionRepository.findAll().stream()
            .filter(p -> p.getStatus() == PromotionStatus.SCHEDULED
                && p.getStartDate() != null
                && !now.isBefore(p.getStartDate()))
            .forEach(p -> {
                p.setStatus(PromotionStatus.ACTIVE);
                p.setIsActive(true);
                promotionRepository.save(p);
                log.info("[Scheduler] Promotion activated: {}", p.getCode());
            });
    }
}
