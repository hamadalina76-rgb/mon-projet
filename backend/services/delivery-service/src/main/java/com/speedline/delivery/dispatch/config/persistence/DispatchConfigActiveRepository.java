package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchConfigActiveRepository extends JpaRepository<DispatchConfigActiveEntity, Integer> {
}
