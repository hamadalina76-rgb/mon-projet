package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchReplaySessionRepository extends JpaRepository<DispatchReplaySessionEntity, Long> {

    List<DispatchReplaySessionEntity> findByCycleCapture_IdOrderByCreatedAtDesc(Long cycleCaptureId);
}
