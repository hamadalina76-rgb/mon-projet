package com.speedline.promotion.service.impl;

import com.speedline.promotion.domain.Promotion.PromotionType;
import com.speedline.promotion.repository.PromotionRepository;
import com.speedline.promotion.repository.UserPromotionRepository;
import com.speedline.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implémentation du service de gestion des promotions
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository userPromotionRepository;

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validatePromoCode(String code, Long userId, Long partnerId, BigDecimal orderAmount) {
        // TODO: Implémenter la validation du code promo
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        // TODO: Implémenter le calcul de la réduction
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void incrementUsage(String code, Long userId) {
        // TODO: Implémenter l'incrémentation de l'utilisation
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("promotions:active")
    public List<PromotionDTO> getActivePromotions() {
        // TODO: Implémenter la récupération des promotions actives
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionDTO> getAvailablePromotions(Long userId, Long partnerId) {
        // TODO: Implémenter la récupération des promotions disponibles
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    @CacheEvict(value = "promotions:active", allEntries = true)
    public PromotionDTO createPromotion(CreatePromotionRequest request) {
        // TODO: Implémenter la création d'une promotion
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void deactivatePromotion(Long promotionId) {
        // TODO: Implémenter la désactivation
        throw new UnsupportedOperationException("À implémenter");
    }
}
