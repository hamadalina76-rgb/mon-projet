package com.speedline.delivery.dispatch.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dispatch_config_snapshot")
@Getter
@Setter
public class DispatchConfigSnapshotEntity {

    @Id
    @Column(name = "version")
    private Long version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "general_json", nullable = false, columnDefinition = "jsonb")
    private String generalJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scoring_json", nullable = false, columnDefinition = "jsonb")
    private String scoringJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_external_json", nullable = false, columnDefinition = "jsonb")
    private String internalExternalJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bundling_json", nullable = false, columnDefinition = "jsonb")
    private String bundlingJson = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exclusivity_json", nullable = false, columnDefinition = "jsonb")
    private String exclusivityJson = "{}";
}
