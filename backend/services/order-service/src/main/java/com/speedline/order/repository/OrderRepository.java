package com.speedline.order.repository;

import com.speedline.order.domain.Order;
import com.speedline.order.domain.Order.PaymentStatus;
import com.speedline.order.domain.OrderStatus;
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
 * Repository pour Order
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    Optional<Order> findByOrderNumber(String orderNumber);
    
    boolean existsByOrderNumber(String orderNumber);

    // ==================== RECHERCHE PAR CLIENT ====================

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    
    List<Order> findByCustomerIdAndStatusIn(Long customerId, List<OrderStatus> statuses);
    
    long countByCustomerId(Long customerId);

    // ==================== RECHERCHE PAR PARTENAIRE ====================

    Page<Order> findByPartnerId(Long partnerId, Pageable pageable);
    
    List<Order> findByPartnerIdAndStatusIn(Long partnerId, List<OrderStatus> statuses);
    
    Page<Order> findByPartnerIdAndStatus(Long partnerId, OrderStatus status, Pageable pageable);
    
    long countByPartnerId(Long partnerId);

    // ==================== RECHERCHE PAR LIVREUR ====================

    Page<Order> findByCourierId(Long courierId, Pageable pageable);
    
    List<Order> findByCourierIdAndStatusIn(Long courierId, List<OrderStatus> statuses);
    
    Optional<Order> findByCourierIdAndStatus(Long courierId, OrderStatus status);
    
    long countByCourierId(Long courierId);

    // ==================== RECHERCHE PAR STATUT ====================

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    List<Order> findByStatusIn(List<OrderStatus> statuses);
    
    long countByStatus(OrderStatus status);

    // ==================== RECHERCHE PAR DATE ====================

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Page<Order> findByDateRange(@Param("start") LocalDateTime start, 
                                 @Param("end") LocalDateTime end, 
                                 Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.partnerId = :partnerId AND o.createdAt BETWEEN :start AND :end")
    List<Order> findByPartnerAndDateRange(@Param("partnerId") Long partnerId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    // ==================== COMMANDES ACTIVES ====================

    @Query("SELECT o FROM Order o WHERE o.status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrders();

    @Query("SELECT o FROM Order o WHERE o.partnerId = :partnerId AND o.status NOT IN ('DELIVERED', 'CANCELLED') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByPartner(@Param("partnerId") Long partnerId);

    @Query("SELECT o FROM Order o WHERE o.courierId = :courierId AND o.status NOT IN ('DELIVERED', 'CANCELLED')")
    List<Order> findActiveOrdersByCourier(@Param("courierId") Long courierId);

    // ==================== COMMANDES EN ATTENTE D'ASSIGNATION ====================

    @Query("SELECT o FROM Order o WHERE o.status = 'READY_FOR_PICKUP' AND o.courierId IS NULL ORDER BY o.createdAt ASC")
    List<Order> findOrdersAwaitingCourier();

    // ==================== MISE À JOUR ====================

    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    int updateStatus(@Param("orderId") Long orderId, @Param("status") OrderStatus status);

    @Modifying
    @Query("UPDATE Order o SET o.courierId = :courierId, o.courierName = :courierName, o.courierPhone = :courierPhone WHERE o.id = :orderId")
    int assignCourier(@Param("orderId") Long orderId, 
                      @Param("courierId") Long courierId,
                      @Param("courierName") String courierName,
                      @Param("courierPhone") String courierPhone);

    @Modifying
    @Query("UPDATE Order o SET o.paymentStatus = :paymentStatus, o.paymentId = :paymentId WHERE o.id = :orderId")
    int updatePaymentStatus(@Param("orderId") Long orderId, 
                            @Param("paymentStatus") PaymentStatus paymentStatus,
                            @Param("paymentId") Long paymentId);

    @Modifying
    @Query("UPDATE Order o SET o.estimatedDeliveryTime = :eta WHERE o.id = :orderId")
    int updateEstimatedDeliveryTime(@Param("orderId") Long orderId, 
                                     @Param("eta") LocalDateTime eta);

    @Modifying
    @Query("UPDATE Order o SET o.actualDeliveryTime = :time WHERE o.id = :orderId")
    int updateActualDeliveryTime(@Param("orderId") Long orderId, 
                                  @Param("time") LocalDateTime time);

    // ==================== STATISTIQUES ====================

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.partnerId = :partnerId AND o.status = 'DELIVERED'")
    BigDecimal getTotalRevenueByPartner(@Param("partnerId") Long partnerId);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.partnerId = :partnerId AND o.status = 'DELIVERED' AND o.createdAt BETWEEN :start AND :end")
    BigDecimal getRevenueByPartnerAndDateRange(@Param("partnerId") Long partnerId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, o.orderTime, o.actualDeliveryTime)) FROM Order o WHERE o.status = 'DELIVERED' AND o.actualDeliveryTime IS NOT NULL")
    Double getAverageDeliveryTime();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt >= :since")
    long countCompletedOrdersSince(@Param("since") LocalDateTime since);
}
