package com.speedline.partner.scheduler;

import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.event.PromotionEndingEventPublisher;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs daily and publishes events for products whose promotion ends in 3 days or in 1 day (tomorrow),
 * so the notification-service can alert the partner.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEndingScheduler {

    private final ProductRepository productRepository;
    private final PartnerRepository partnerRepository;
    private final PromotionEndingEventPublisher promotionEndingEventPublisher;

    /** Runs every day at 8:00 AM (server time). Notifies for promotions ending in 3 days and in 1 day. */
    @Scheduled(cron = "${app.promotion-ending.cron:0 0 8 * * *}")
    public void notifyPromotionEndingSoon() {
        LocalDate now = LocalDate.now();
        // 1) Promotions qui se terminent dans 3 jours
        LocalDate inThreeDays = now.plusDays(3);
        List<Product> inThreeDaysProducts = productRepository.findByPromotionEndDateAndStatusNot(inThreeDays, ProductStatus.DELETED);
        if (!inThreeDaysProducts.isEmpty()) {
            log.info("Found {} product(s) with promotion ending on {} (in 3 days), sending notifications.", inThreeDaysProducts.size(), inThreeDays);
            for (Product p : inThreeDaysProducts) {
                partnerRepository.findById(p.getPartnerId()).ifPresent(partner ->
                        promotionEndingEventPublisher.publishPromotionEnding(
                                p.getPartnerId(), partner.getUserId(), p.getId(), p.getName(),
                                p.getPromotionLabel(), p.getPromotionEndDate(), 3));
            }
        }
        // 2) Promotions qui se terminent demain (pour test et rappel la veille)
        LocalDate tomorrow = now.plusDays(1);
        List<Product> tomorrowProducts = productRepository.findByPromotionEndDateAndStatusNot(tomorrow, ProductStatus.DELETED);
        if (!tomorrowProducts.isEmpty()) {
            log.info("Found {} product(s) with promotion ending on {} (tomorrow), sending notifications.", tomorrowProducts.size(), tomorrow);
            for (Product p : tomorrowProducts) {
                partnerRepository.findById(p.getPartnerId()).ifPresent(partner ->
                        promotionEndingEventPublisher.publishPromotionEnding(
                                p.getPartnerId(), partner.getUserId(), p.getId(), p.getName(),
                                p.getPromotionLabel(), p.getPromotionEndDate(), 1));
            }
        }
    }

    /**
     * Déclenche manuellement la vérification pour un partenaire (pour test sans attendre 8h).
     * Appelé par POST /partners/{partnerId}/menu/trigger-promotion-ending-check
     */
    public int runNowForPartner(Long partnerId) {
        LocalDate now = LocalDate.now();
        int count = 0;
        for (int daysLeft : new int[] { 3, 1 }) {
            LocalDate targetDate = now.plusDays(daysLeft);
            List<Product> products = productRepository.findByPartnerIdAndPromotionEndDateAndStatusNot(partnerId, targetDate, ProductStatus.DELETED);
            for (Product p : products) {
                partnerRepository.findById(p.getPartnerId()).ifPresent(partner -> {
                    promotionEndingEventPublisher.publishPromotionEnding(
                            p.getPartnerId(), partner.getUserId(), p.getId(), p.getName(),
                            p.getPromotionLabel(), p.getPromotionEndDate(), daysLeft);
                });
                count++;
            }
        }
        return count;
    }
}
