package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DispatchConfigVersionRepository extends JpaRepository<DispatchConfigVersionEntity, Long> {

    Optional<DispatchConfigVersionEntity> findTopByOrderByVersionDesc();

    Optional<DispatchConfigVersionEntity> findByVersion(long version);

    @Query("select coalesce(max(v.version), 0) from DispatchConfigVersionEntity v")
    long maxVersion();
}
