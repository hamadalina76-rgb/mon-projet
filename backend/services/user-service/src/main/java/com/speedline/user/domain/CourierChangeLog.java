package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Journal d'audit des modifications admin sur un livreur.
 * Actions : APPROVE | REJECT | SUSPEND | ACTIVATE | DEACTIVATE | CHANGE_TYPE | ASSIGN_ZONES | REQUEST_MORE_INFO
 *           | SCHEDULE_UPDATED | SCHEDULE_TEMPLATE_APPLIED | SCHEDULE_DAY_COPIED
 */
@Entity
@Table(name = "courier_change_logs",
       indexes = @Index(name = "idx_ccl_courier_id", columnList = "courier_id, changed_at DESC"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_name", length = 150)
    private String adminName;

    /** APPROVE | REJECT | SUSPEND | ACTIVATE | DEACTIVATE | CHANGE_TYPE | ASSIGN_ZONES | REQUEST_MORE_INFO */
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    // ── Statut ───────────────────────────────────────────────────────────────

    @Column(name = "status_before", length = 50)
    private String statusBefore;

    @Column(name = "status_after", length = 50)
    private String statusAfter;

    // ── Type de livreur ──────────────────────────────────────────────────────

    @Column(name = "courier_type_before", length = 50)
    private String courierTypeBefore;

    @Column(name = "courier_type_after", length = 50)
    private String courierTypeAfter;

    // ── Zones assignées ──────────────────────────────────────────────────────

    /** IDs séparés par virgule, ex : "1,2,5" */
    @Column(name = "zone_ids_before", length = 1000)
    private String zoneIdsBefore;

    @Column(name = "zone_ids_after", length = 1000)
    private String zoneIdsAfter;

    // ── Description générique ─────────────────────────────────────────────────

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ── Motif ────────────────────────────────────────────────────────────────

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
