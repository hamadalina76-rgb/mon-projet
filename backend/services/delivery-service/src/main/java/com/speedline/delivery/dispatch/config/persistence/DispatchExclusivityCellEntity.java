package com.speedline.delivery.dispatch.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "dispatch_exclusivity_cell")
@IdClass(DispatchExclusivityCellEntity.Pk.class)
@Getter
@Setter
public class DispatchExclusivityCellEntity {

    @Id
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Id
    @Column(name = "commerce_type", nullable = false, length = 64)
    private String commerceType;

    @Column(nullable = false)
    private boolean allowed = true;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long zoneId;
        private String commerceType;
    }
}
