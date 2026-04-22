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
import java.util.UUID;

@Entity
@Table(name = "dispatch_cycle_capture")
@Getter
@Setter
public class DispatchCycleCaptureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_cycle_id", nullable = false, unique = true)
    private UUID externalCycleId = UUID.randomUUID();

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pending_orders_json", columnDefinition = "jsonb")
    private String pendingOrdersJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "couriers_json", columnDefinition = "jsonb")
    private String couriersJson;

    @Column(name = "pool_internal_only")
    private Boolean poolInternalOnly;

    @Column(name = "config_version")
    private Long configVersion;
}
