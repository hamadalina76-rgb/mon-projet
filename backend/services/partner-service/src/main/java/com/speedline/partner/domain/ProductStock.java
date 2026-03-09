package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stock d'un produit (OneToOne avec Product).
 * Si isTrackingEnabled = false, le stock est ignoré et le produit reste disponible.
 */
@Entity
@Table(name = "product_stock", indexes = {
    @Index(name = "idx_product_stock_product_id", columnList = "productId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isTrackingEnabled = true;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
