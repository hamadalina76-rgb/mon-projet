package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_zones",
    uniqueConstraints = @UniqueConstraint(columnNames = {"partner_id", "zone_id"}),
    indexes = {
        @Index(name = "idx_partner_zones_partner", columnList = "partner_id"),
        @Index(name = "idx_partner_zones_zone",    columnList = "zone_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    /** ID de zone dans location-service (pas de FK cross-service) */
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    void prePersist() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }
}
