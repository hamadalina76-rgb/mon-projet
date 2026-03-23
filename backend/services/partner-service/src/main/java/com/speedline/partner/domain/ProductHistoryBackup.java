package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_history_backups", indexes = {
        @Index(name = "idx_phb_partner_created", columnList = "partner_id, created_at"),
        @Index(name = "idx_phb_product_created", columnList = "product_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductHistoryBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "actor_type", length = 30)
    private String actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "changes_before", columnDefinition = "TEXT")
    private String changesBefore;

    @Column(name = "changes_after", columnDefinition = "TEXT")
    private String changesAfter;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
