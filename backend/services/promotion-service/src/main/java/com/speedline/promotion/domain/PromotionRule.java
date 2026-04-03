package com.speedline.promotion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_rules", indexes = {
    @Index(name = "idx_promotion_rules_promo", columnList = "promotion_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    /** e.g. "GTE", "LTE", "EQ" */
    @Column(length = 10)
    private String operator;

    /** The rule value (e.g. "20.00" for min order, "1" for user quota) */
    @Column(name = "target_value", length = 255)
    private String targetValue;
}
