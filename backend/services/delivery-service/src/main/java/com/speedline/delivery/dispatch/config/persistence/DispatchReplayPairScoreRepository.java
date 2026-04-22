package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchReplayPairScoreRepository extends JpaRepository<DispatchReplayPairScoreEntity, Long> {

    List<DispatchReplayPairScoreEntity> findByReplaySession_IdOrderByOrderIdAscCourierIdAsc(Long replaySessionId);
}
