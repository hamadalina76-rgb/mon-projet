package com.speedline.promotion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_audit_log", indexes = {
    @Index(name = "idx_audit_promotion", columnList = "promotion_id"),
    @Index(name = "idx_audit_created",   columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "performed_by")
    private String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AuditAction {
        CREATED,
        UPDATED,
        DELETED,
        ACTIVATED,
        DEACTIVATED,
        TOGGLED,
        APPLIED,
        REVOKED,
        EXPIRED
    }
}
