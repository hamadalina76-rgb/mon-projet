package com.speedline.delivery.repository;

import com.speedline.delivery.domain.Delivery;
import com.speedline.delivery.domain.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Delivery
 */
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    Optional<Delivery> findByOrderId(Long orderId);
    
    boolean existsByOrderId(Long orderId);

    // ==================== RECHERCHE PAR LIVREUR ====================

    Page<Delivery> findByCourierId(Long courierId, Pageable pageable);
    
    List<Delivery> findByCourierIdAndStatusIn(Long courierId, List<DeliveryStatus> statuses);
    
    Optional<Delivery> findByCourierIdAndStatus(Long courierId, DeliveryStatus status);
    
    long countByCourierIdAndStatus(Long courierId, DeliveryStatus status);

    // ==================== RECHERCHE PAR STATUT ====================

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);
    
    List<Delivery> findByStatusIn(List<DeliveryStatus> statuses);

    // ==================== LIVRAISONS ACTIVES ====================

    @Query("SELECT d FROM Delivery d WHERE d.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED') ORDER BY d.createdAt DESC")
    List<Delivery> findActiveDeliveries();

    @Query("SELECT d FROM Delivery d WHERE d.courierId = :courierId AND d.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED')")
    List<Delivery> findActiveDeliveriesByCourier(@Param("courierId") Long courierId);

        @Query("""
                        SELECT DISTINCT d.courierId FROM Delivery d
                        WHERE d.courierId IS NOT NULL
                            AND d.courierId <> :excludedCourierId
                            AND d.status = 'DELIVERED'
                        ORDER BY d.courierId ASC
                        """)
        List<Long> findReassignmentCandidateCourierIds(@Param("excludedCourierId") Long excludedCourierId);

    // ==================== LIVRAISONS EN ATTENTE ====================

    @Query("SELECT d FROM Delivery d WHERE d.status = 'PENDING' ORDER BY d.createdAt ASC")
    List<Delivery> findPendingDeliveries();

    // ==================== MISE À JOUR ====================

    @Modifying
    @Query("UPDATE Delivery d SET d.status = :status WHERE d.id = :deliveryId")
    int updateStatus(@Param("deliveryId") Long deliveryId, @Param("status") DeliveryStatus status);

    @Modifying
    @Query("UPDATE Delivery d SET d.courierId = :courierId, d.courierName = :courierName, d.courierPhone = :courierPhone, d.status = 'ASSIGNED', d.assignedAt = CURRENT_TIMESTAMP WHERE d.id = :deliveryId")
    int assignCourier(@Param("deliveryId") Long deliveryId, 
                      @Param("courierId") Long courierId,
                      @Param("courierName") String courierName,
                      @Param("courierPhone") String courierPhone);

    // ==================== STATISTIQUES ====================

    long countByCourierId(Long courierId);
    
    long countByCourierIdAndStatusAndDeliveredAtBetween(Long courierId, DeliveryStatus status, 
                                                         LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(d.actualDuration) FROM Delivery d WHERE d.status = 'DELIVERED' AND d.actualDuration IS NOT NULL")
    Double getAverageDeliveryTime();

    @Query("SELECT SUM(d.actualDistance) FROM Delivery d WHERE d.courierId = :courierId AND d.status = 'DELIVERED'")
    BigDecimal getTotalDistanceByCourier(@Param("courierId") Long courierId);

    Long countByStatusAndDeliveredAtAfter(DeliveryStatus status, LocalDateTime deliveredAfter);

    Long countByDeliveredAtIsNotNull();

    Long countByStatus(DeliveryStatus status);

    @Query(
            value = "SELECT AVG(EXTRACT(EPOCH FROM (assigned_at - created_at))) FROM deliveries WHERE assigned_at IS NOT NULL",
            nativeQuery = true
    )
    Double averageAssignmentDelaySeconds();
}
