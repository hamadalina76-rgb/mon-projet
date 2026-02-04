package com.speedline.order.repository;

import com.speedline.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour OrderItem
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Trouver les articles d'une commande
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Supprimer les articles d'une commande
     */
    void deleteByOrderId(Long orderId);

    /**
     * Compter les articles d'une commande
     */
    long countByOrderId(Long orderId);

    /**
     * Compter combien de fois un produit a été commandé
     */
    long countByProductId(Long productId);

    /**
     * Trouver les produits les plus commandés
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQty FROM OrderItem oi " +
           "GROUP BY oi.productId, oi.productName ORDER BY totalQty DESC")
    List<Object[]> findMostOrderedProducts();

    /**
     * Trouver les produits les plus commandés pour un partenaire
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQty FROM OrderItem oi " +
           "JOIN Order o ON oi.orderId = o.id WHERE o.partnerId = :partnerId " +
           "GROUP BY oi.productId, oi.productName ORDER BY totalQty DESC")
    List<Object[]> findMostOrderedProductsByPartner(@Param("partnerId") Long partnerId);
}
