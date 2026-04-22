package com.speedline.delivery.dispatch.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "dispatch_config_audit")
@Getter
@Setter
public class DispatchConfigAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "actor_user_id")
    private String actorUserId;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "config_group", nullable = false)
    private String configGroup;

    @Column(name = "endpoint_path")
    private String endpointPath;

    @Column(name = "diff_summary")
    private String diffSummary;

    @Column(name = "config_version", nullable = false)
    private Long configVersion;
}
