package com.speedline.partner.repository;

import com.speedline.partner.domain.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    Optional<ProductStock> findByProductId(Long productId);

    List<ProductStock> findByProductIdIn(List<Long> productIds);

    /**
     * IDs des produits en stock faible (isTrackingEnabled = true, quantity <= lowStockThreshold).
     */
    @Query("SELECT ps.productId FROM ProductStock ps WHERE ps.productId IN :productIds " +
           "AND ps.isTrackingEnabled = true AND ps.quantity <= ps.lowStockThreshold")
    List<Long> findProductIdsInLowStock(@Param("productIds") List<Long> productIds);
}
