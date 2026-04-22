package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchSimulationRunRepository extends JpaRepository<DispatchSimulationRunEntity, Long> {
}
