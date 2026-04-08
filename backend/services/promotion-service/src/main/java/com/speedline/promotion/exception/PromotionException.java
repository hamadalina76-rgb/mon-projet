package com.speedline.promotion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PromotionException extends RuntimeException {

    private final String errorCode;

    public PromotionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static PromotionException notFound(String code) {
        return new PromotionException("PROMOTION_NOT_FOUND", "Code promo introuvable : " + code);
    }

    public static PromotionException inactive(String code) {
        return new PromotionException("PROMOTION_INACTIVE", "Ce code promo n'est plus actif : " + code);
    }

    public static PromotionException expired(String code) {
        return new PromotionException("PROMOTION_EXPIRED", "Ce code promo a expiré : " + code);
    }

    public static PromotionException notStarted(String code) {
        return new PromotionException("PROMOTION_NOT_STARTED", "Ce code promo n'est pas encore valide : " + code);
    }

    public static PromotionException quotaExceeded(String code) {
        return new PromotionException("PROMOTION_QUOTA_EXCEEDED", "Ce code promo a atteint sa limite d'utilisation : " + code);
    }

    public static PromotionException userQuotaExceeded(String code) {
        return new PromotionException("USER_QUOTA_EXCEEDED", "Vous avez déjà utilisé ce code le nombre maximal de fois : " + code);
    }

    public static PromotionException minimumOrderNotMet(String code) {
        return new PromotionException("MINIMUM_ORDER_NOT_MET", "Le montant minimum de commande n'est pas atteint pour : " + code);
    }

    public static PromotionException partnerNotEligible(String code) {
        return new PromotionException("PARTNER_NOT_ELIGIBLE", "Ce code promo n'est pas valide pour ce partenaire : " + code);
    }

    public static PromotionException categoryNotEligible(String code) {
        return new PromotionException("CATEGORY_NOT_ELIGIBLE", "Ce code promo n'est pas valide pour cette catégorie : " + code);
    }

    public static PromotionException duplicateCode(String code) {
        return new PromotionException("DUPLICATE_CODE", "Un code promo avec ce code existe déjà : " + code);
    }

    public static PromotionException invalidDateRange() {
        return new PromotionException("INVALID_DATE_RANGE", "La date de fin doit être après la date de début");
    }

    public static PromotionException invalidPercentage(java.math.BigDecimal value) {
        return new PromotionException("INVALID_PERCENTAGE", "Un pourcentage ne peut pas dépasser 100% (valeur: " + value + ")");
    }

    public static PromotionException cannotActivateExpired(String code) {
        return new PromotionException("CANNOT_ACTIVATE_EXPIRED", "Impossible d'activer une promotion expirée : " + code);
    }

    public static PromotionException cannotActivateScheduled(String code) {
        return new PromotionException("CANNOT_ACTIVATE_SCHEDULED",
                "Impossible d'activer manuellement une promotion planifiée. Elle s'activera automatiquement à sa date de début : " + code);
    }
}
