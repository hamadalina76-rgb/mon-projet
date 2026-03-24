package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entrée d'audit pour chaque modification d'un planning de livreur.
 */
@Entity
@Table(name = "courier_schedule_audit_logs", indexes = {
        @Index(name = "idx_csal_courier_created", columnList = "courierId, createdAt DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierScheduleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courierId;

    /** Identifiant du planning concerné (null pour DELETED si déjà effacé) */
    private Long scheduleId;

    /** Type d'action : CREATED, UPDATED, TEMPLATE_APPLIED, DAY_COPIED, DELETED */
    @Column(nullable = false, length = 50)
    private String action;

    /** Template éventuellement impliqué */
    private Long templateId;

    @Column(length = 100)
    private String templateName;

    /** Description lisible de la modification */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** userId (auth-service) de l'admin ayant effectué l'action */
    private Long adminId;

    @Column(length = 150)
    private String adminName;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
