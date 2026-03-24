package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_change_logs",
       indexes = @Index(name = "idx_pcl_partner_id", columnList = "partner_id, changed_at DESC"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "admin_id")
    private Long adminId;

    /** APPROVE | APPROVE_WITH_COMMISSION | REJECT | SUSPEND | ACTIVATE | DEACTIVATE */
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    // ── Status ──────────────────────────────────────────────────────────────

    @Column(name = "status_before", length = 50)
    private String statusBefore;

    @Column(name = "status_after", length = 50)
    private String statusAfter;

    // ── Commission ───────────────────────────────────────────────────────────

    @Column(name = "commission_type_before", length = 50)
    private String commissionTypeBefore;

    @Column(name = "commission_type_after", length = 50)
    private String commissionTypeAfter;

    @Column(name = "commission_rate_before", precision = 10, scale = 2)
    private BigDecimal commissionRateBefore;

    @Column(name = "commission_rate_after", precision = 10, scale = 2)
    private BigDecimal commissionRateAfter;

    // ── Categories ───────────────────────────────────────────────────────────

    @Column(name = "category_ids_before", length = 500)
    private String categoryIdsBefore;

    @Column(name = "category_ids_after", length = 500)
    private String categoryIdsAfter;

    // ── Zones ────────────────────────────────────────────────────────────────

    @Column(name = "zone_ids_before", length = 1000)
    private String zoneIdsBefore;

    @Column(name = "zone_ids_after", length = 1000)
    private String zoneIdsAfter;

    // ── Product edit permission (menu products) ───────────────────────────────

    @Column(name = "product_edit_permission_before")
    private Boolean productEditPermissionBefore;

    @Column(name = "product_edit_permission_after")
    private Boolean productEditPermissionAfter;

    // ── Reason ───────────────────────────────────────────────────────────────

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }
}
