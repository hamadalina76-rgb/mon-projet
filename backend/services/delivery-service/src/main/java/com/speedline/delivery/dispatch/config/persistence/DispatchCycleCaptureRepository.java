package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DispatchCycleCaptureRepository extends JpaRepository<DispatchCycleCaptureEntity, Long> {

    Optional<DispatchCycleCaptureEntity> findByExternalCycleId(UUID externalCycleId);
}
