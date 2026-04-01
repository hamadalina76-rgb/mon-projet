package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Planning exceptionnel (ponctuel) d'un livreur qui écrase le planning normal
 * sur une période définie (jours fériés, événements spéciaux, congé, etc.)
 *
 * Types : JOUR_FERIE | EVENEMENT_SPECIAL | CONGE | FERMETURE | FORMATION
 */
@Entity
@Table(
    name = "courier_exceptional_schedules",
    indexes = {
        @Index(name = "idx_ces_courier_id",  columnList = "courier_id"),
        @Index(name = "idx_ces_dates",       columnList = "start_date, end_date"),
        @Index(name = "idx_ces_active",      columnList = "is_active")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierExceptionalSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    /** JOUR_FERIE | EVENEMENT_SPECIAL | CONGE | FERMETURE | FORMATION */
    @Column(name = "exception_type", nullable = false, length = 30)
    private String exceptionType;

    /** Libellé humain : "Aïd El Fitr", "Fête du Travail", "Congé annuel" */
    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Motif / raison de l'exception */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "unavailability_reason", length = 40)
    private UnavailabilityReason unavailabilityReason;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 40)
    private UnavailabilityValidationStatus validationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_type", length = 20)
    private CourierType courierType;

    @Column(name = "validator_admin_id")
    private Long validatorAdminId;

    @Column(name = "validator_admin_name", length = 150)
    private String validatorAdminName;

    @Column(name = "validation_comment", columnDefinition = "TEXT")
    private String validationComment;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    /**
     * true  = le livreur ne travaille PAS pendant cette période (on affiche REST)
     * false = des shifts spéciaux s'appliquent (version future)
     */
    @Column(name = "is_rest_period", nullable = false)
    @Builder.Default
    private Boolean isRestPeriod = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Prénom + Nom du livreur (dénormalisé pour l'affichage) */
    @Column(name = "courier_name", length = 150)
    private String courierName;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_name", length = 150)
    private String adminName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
