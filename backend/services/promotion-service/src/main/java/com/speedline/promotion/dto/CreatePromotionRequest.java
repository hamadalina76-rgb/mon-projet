package com.speedline.promotion.dto;

import com.speedline.promotion.domain.PromotionStatus;
import com.speedline.promotion.domain.PromotionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.speedline.promotion.domain.RuleType;

public record CreatePromotionRequest(

    @NotBlank(message = "Le code est obligatoire")
    @Size(min = 3, max = 50, message = "Le code doit contenir entre 3 et 50 caractères")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Le code ne doit contenir que des lettres majuscules, chiffres, - ou _")
    String code,

    @NotBlank(message = "Le nom est obligatoire")
    String name,

    String description,

    @NotNull(message = "Le type est obligatoire")
    PromotionType type,

    BigDecimal value,

    @DecimalMin(value = "0", message = "La réduction max ne peut pas être négative")
    BigDecimal maximumDiscount,

    @DecimalMin(value = "0", message = "Le montant minimum ne peut pas être négatif")
    BigDecimal minimumOrder,

    @Min(value = 1, message = "La limite d'utilisation totale doit être au moins 1")
    Integer usageLimitTotal,

    @Min(value = 1, message = "La limite par utilisateur doit être au moins 1")
    Integer usageLimitPerUser,

    LocalDateTime startDate,

    LocalDateTime endDate,

    List<Long> applicablePartnerIds,

    List<Long> applicableCategoryIds,

    List<Long> applicableZoneIds,

    Boolean firstOrderOnly,

    PromotionStatus status,

    List<RuleRequest> rules
) {
    public record RuleRequest(
        @NotNull(message = "Le type de règle est obligatoire")
        RuleType ruleType,
        String operator,
        String targetValue
    ) {}
}
