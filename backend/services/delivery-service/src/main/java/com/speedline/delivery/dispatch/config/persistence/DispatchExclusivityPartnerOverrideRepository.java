package com.speedline.delivery.dispatch.config.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchExclusivityPartnerOverrideRepository extends JpaRepository<DispatchExclusivityPartnerOverrideEntity, Long> {

    List<DispatchExclusivityPartnerOverrideEntity> findByPartnerId(Long partnerId);
}
