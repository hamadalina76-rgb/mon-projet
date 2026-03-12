package com.speedline.partner.repository;

import com.speedline.partner.domain.PromotionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionLogRepository extends JpaRepository<PromotionLog, Long>, JpaSpecificationExecutor<PromotionLog> {

    Page<PromotionLog> findByPartnerIdOrderByAppliedAtDesc(Long partnerId, Pageable pageable);
}
