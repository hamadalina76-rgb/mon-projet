package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entrée d'audit pour chaque modification des paramètres globaux :
 * horaires d'ouverture, activation / désactivation de l'application.
 */
@Entity
@Table(name = "settings_audit_logs", indexes = {
        @Index(name = "idx_sal_created_at", columnList = "createdAt DESC"),
        @Index(name = "idx_sal_action",     columnList = "action")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** WORKING_HOURS_UPDATED | APP_ENABLED | APP_DISABLED | GENERAL_SETTINGS_UPDATED */
    @Column(nullable = false, length = 60)
    private String action;

    /** userId (auth-service) de l'admin ayant effectué l'action */
    private Long adminId;

    /** Nom résolu au moment de l'action */
    @Column(length = 100)
    private String adminName;

    /** Description lisible du changement */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
