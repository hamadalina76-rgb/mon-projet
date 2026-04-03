package com.speedline.promotion.repository;

import com.speedline.promotion.domain.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRuleRepository extends JpaRepository<PromotionRule, Long> {

    List<PromotionRule> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);
}
