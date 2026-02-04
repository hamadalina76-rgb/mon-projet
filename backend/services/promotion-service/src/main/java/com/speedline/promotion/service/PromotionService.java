package com.speedline.promotion.service;

import com.speedline.promotion.domain.Promotion.PromotionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des promotions
 */
public interface PromotionService {

    /**
     * Valider un code promo
     */
    ValidationResult validatePromoCode(String code, Long userId, Long partnerId, BigDecimal orderAmount);

    /**
     * Appliquer un code promo et calculer la réduction
     */
    BigDecimal calculateDiscount(String code, BigDecimal orderAmount);

    /**
     * Incrémenter l'utilisation d'un code promo
     */
    void incrementUsage(String code, Long userId);

    /**
     * Obtenir les promotions actives
     */
    List<PromotionDTO> getActivePromotions();

    /**
     * Obtenir les promotions disponibles pour un utilisateur
     */
    List<PromotionDTO> getAvailablePromotions(Long userId, Long partnerId);

    /**
     * Créer une promotion (admin)
     */
    PromotionDTO createPromotion(CreatePromotionRequest request);

    /**
     * Désactiver une promotion
     */
    void deactivatePromotion(Long promotionId);

    /**
     * DTO de résultat de validation
     */
    record ValidationResult(boolean isValid, String message, BigDecimal discount, PromotionType type) {}

    /**
     * DTO de promotion
     */
    record PromotionDTO(
            Long id,
            String code,
            String name,
            String description,
            PromotionType type,
            BigDecimal value,
            BigDecimal minimumOrder,
            BigDecimal maximumDiscount,
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            Boolean isActive
    ) {}

    /**
     * DTO de création de promotion
     */
    record CreatePromotionRequest(
            String code,
            String name,
            String description,
            PromotionType type,
            BigDecimal value,
            BigDecimal minimumOrder,
            BigDecimal maximumDiscount,
            Integer usageLimit,
            Integer usageLimitPerUser,
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            List<Long> applicablePartnerIds,
            Boolean firstOrderOnly
    ) {}
}
