package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(nullable = false)
    private String action;  // CREATE | UPDATE | DELETE | DEACTIVATE | ACTIVATE

    @Column(name = "entity_type", nullable = false)
    private String entityType;  // CATEGORY

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}