package com.speedline.promotion.dto;

import com.speedline.promotion.domain.PromotionAuditLog;

import java.time.LocalDateTime;
import java.util.List;

public record PromotionAuditLogDto(
    Long id,
    Long promotionId,
    String promotionCode,
    String action,
    String details,
    String performedBy,
    LocalDateTime createdAt
) {
    public static PromotionAuditLogDto from(PromotionAuditLog log) {
        return new PromotionAuditLogDto(
            log.getId(),
            log.getPromotionId(),
            log.getPromotionCode(),
            log.getAction().name(),
            log.getDetails(),
            log.getPerformedBy(),
            log.getCreatedAt()
        );
    }
}
