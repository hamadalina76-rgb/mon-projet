package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchExclusivityCellRepository extends JpaRepository<DispatchExclusivityCellEntity, DispatchExclusivityCellEntity.Pk> {

    List<DispatchExclusivityCellEntity> findByZoneId(Long zoneId);
}
