package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface DispatchConfigAuditRepository extends JpaRepository<DispatchConfigAuditEntity, Long> {

    Page<DispatchConfigAuditEntity> findByOccurredAtBetweenOrderByOccurredAtAsc(
            Instant fromInclusive, Instant toExclusive, Pageable pageable);
}
