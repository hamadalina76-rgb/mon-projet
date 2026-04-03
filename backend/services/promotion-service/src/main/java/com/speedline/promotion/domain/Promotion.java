package com.speedline.promotion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Promotion - Codes promo et réductions
 */
@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promotion_code", columnList = "code", unique = true),
    @Index(name = "idx_promotion_active", columnList = "is_active"),
    @Index(name = "idx_promotion_dates", columnList = "start_date, end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PromotionStatus status = PromotionStatus.ACTIVE;

    @Column(precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "minimum_order", precision = 10, scale = 2)
    private BigDecimal minimumOrder;

    @Column(name = "maximum_discount", precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * IDs des partenaires éligibles (JSON array, null = tous)
     */
    @Column(name = "applicable_partner_ids", columnDefinition = "TEXT")
    private String applicablePartnerIds;

    /**
     * IDs des catégories éligibles (JSON array, null = toutes)
     */
    @Column(name = "applicable_category_ids", columnDefinition = "TEXT")
    private String applicableCategoryIds;

    /**
     * IDs des zones éligibles pour FREE_DELIVERY (comma-separated, null = toutes)
     */
    @Column(name = "applicable_zone_ids", columnDefinition = "TEXT")
    private String applicableZoneIds;

    @Column(name = "first_order_only")
    @Builder.Default
    private Boolean firstOrderOnly = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
               (startDate == null || !now.isBefore(startDate)) &&
               (endDate == null || !now.isAfter(endDate)) &&
               (usageLimit == null || usageCount < usageLimit);
    }
}
