package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Log d'une application de promotion sur un ou plusieurs produits.
 * Chaque appel à setPromotion crée une entrée par produit concerné.
 */
@Entity
@Table(name = "promotion_logs", indexes = {
    @Index(name = "idx_promotion_logs_partner_applied", columnList = "partner_id, applied_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "promotion_label", length = 100)
    private String promotionLabel;

    @Column(name = "promotion_start_date")
    private LocalDate promotionStartDate;

    @Column(name = "promotion_end_date")
    private LocalDate promotionEndDate;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
}
