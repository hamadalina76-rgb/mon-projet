package com.speedline.delivery.dispatch.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "dispatch_simulation_run")
@Getter
@Setter
public class DispatchSimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "actor_user_id")
    private String actorUserId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "zone_id")
    private Long zoneId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "overlay_json", nullable = false, columnDefinition = "jsonb")
    private String overlayJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;

    @Column(nullable = false)
    private String status;
}
