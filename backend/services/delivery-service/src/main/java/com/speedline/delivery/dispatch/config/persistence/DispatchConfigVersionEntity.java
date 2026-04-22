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
@Table(name = "dispatch_config_version")
@Getter
@Setter
public class DispatchConfigVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long version;

    @Column(name = "optimistic_lock", nullable = false)
    private int optimisticLock = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by_user_id")
    private String createdByUserId;

    @Column(name = "created_by_email")
    private String createdByEmail;
}
