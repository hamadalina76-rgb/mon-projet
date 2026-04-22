package com.speedline.delivery.dispatch.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dispatch_exclusivity_partner_override")
@Getter
@Setter
public class DispatchExclusivityPartnerOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "commerce_type", nullable = false, length = 64)
    private String commerceType;

    @Column(nullable = false)
    private boolean allowed;
}
