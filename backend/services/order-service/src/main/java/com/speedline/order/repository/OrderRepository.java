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

    long countByPartnerIdAndStatus(Long partnerId, OrderStatus status);

    /** Compte sur {@code orderTime} inclusif (même sémantique que la liste partenaire). */
    long countByPartnerIdAndOrderTimeBetween(
            Long partnerId,
            LocalDateTime from,
            LocalDateTime to);

    long countByPartnerIdAndStatusAndOrderTimeBetween(
            Long partnerId,
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to);

    /** Recherche libre sur numéro de commande OU nom client */
    @Query("SELECT o FROM Order o WHERE o.partnerId = :partnerId AND (" +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findByPartnerIdAndSearch(
            @Param("partnerId") Long partnerId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Liste « Toutes » (tri urgence) : pipeline cuisine d’abord, puis livrées / annulées ;
     * dans chaque statut, commande la plus récente en premier (comme Glovo).
     */
    @Query("SELECT o FROM Order o WHERE o.partnerId = :partnerId ORDER BY "
            + "CASE "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PENDING THEN 0 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.CONFIRMED THEN 1 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PREPARING THEN 2 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.READY_FOR_PICKUP THEN 3 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PICKED_UP THEN 4 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.IN_DELIVERY THEN 5 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.DELIVERED THEN 6 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.CANCELLED THEN 7 "
            + "ELSE 8 END ASC, o.orderTime DESC")
    Page<Order> findByPartnerIdOrderByKitchenPriority(@Param("partnerId") Long partnerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.partnerId = :partnerId AND ("
            + "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "ORDER BY "
            + "CASE "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PENDING THEN 0 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.CONFIRMED THEN 1 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PREPARING THEN 2 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.READY_FOR_PICKUP THEN 3 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PICKED_UP THEN 4 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.IN_DELIVERY THEN 5 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.DELIVERED THEN 6 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.CANCELLED THEN 7 "
            + "ELSE 8 END ASC, o.orderTime DESC")
    Page<Order> findByPartnerIdAndSearchOrderByKitchenPriority(
            @Param("partnerId") Long partnerId,
            @Param("search") String search,
            Pageable pageable);

    /** Recherche libre + filtre statut */
    @Query("SELECT o FROM Order o WHERE o.partnerId = :partnerId AND o.status = :status AND (" +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findByPartnerIdAndStatusAndSearch(
            @Param("partnerId") Long partnerId,
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable);

    // ==================== REQUÊTES UNIFIÉES (status + date + search) ====================

    /**
     * Tri par urgence — statuts actifs d'abord, avec filtrage optionnel par date et recherche.
     * Utilisé pour l'onglet « Toutes » avec sortBy=priority.
     */
    /**
     * Tri par urgence — always provide non-null from/to (use sentinel dates for "no filter").
     * PostgreSQL cannot infer the type of a NULL-bound parameter in IS NULL checks.
     */
    @Query(value = "SELECT o FROM Order o WHERE o.partnerId = :partnerId "
            + "AND o.orderTime >= :from AND o.orderTime <= :to "
            + "AND (:search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "ORDER BY CASE "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PENDING THEN 0 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.CONFIRMED THEN 1 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PREPARING THEN 2 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.READY_FOR_PICKUP THEN 3 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.PICKED_UP THEN 4 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.IN_DELIVERY THEN 5 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.DELIVERED THEN 6 "
            + "WHEN o.status = com.speedline.order.domain.OrderStatus.CANCELLED THEN 7 "
            + "ELSE 8 END ASC, o.orderTime DESC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.partnerId = :partnerId "
            + "AND o.orderTime >= :from AND o.orderTime <= :to "
            + "AND (:search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findByPartnerIdPriority(
            @Param("partnerId") Long partnerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("search") String search,
            Pageable pageable);

    /** Tous statuts — always provide non-null from/to. */
    @Query(value = "SELECT o FROM Order o WHERE o.partnerId = :partnerId "
            + "AND o.orderTime >= :from AND o.orderTime <= :to "
            + "AND (:search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.partnerId = :partnerId "
            + "AND o.orderTime >= :from AND o.orderTime <= :to "
            + "AND (:search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findByPartnerIdAllStatuses(
            @Param("partnerId") Long partnerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("search") String search,
            Pageable pageable);

    /** Filtre statut précis — always provide non-null from/to. */
    @Query(value = "SELECT o FROM Order o WHERE o.partnerId = :partnerId AND o.status = :status "
            + "AND o.orderTime >= :from AND o.orderTime <= :to "
            + "AND (:search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.partnerId = :partnerId AND o.status = :status "
            + "AND o.orderTime >= :from AND o.orderTime <= :to "
            + "AND (:search = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "     OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findByPartnerIdAndStatusFiltered(
            @Param("partnerId") Long partnerId,
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("search") String search,
            Pageable pageable);

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

    @Modifying
    @Query(value = "UPDATE orders "
            + "SET delivery_address = CASE "
            + "        WHEN :deliveryAddressJson IS NULL OR :deliveryAddressJson = '' THEN NULL "
            + "        ELSE CAST(:deliveryAddressJson AS jsonb) "
            + "    END, "
            + "    delivery_location = CASE "
            + "        WHEN :deliveryLatitude IS NULL OR :deliveryLongitude IS NULL THEN NULL "
            + "        ELSE CAST(ST_SetSRID(ST_MakePoint(CAST(:deliveryLongitude AS DOUBLE PRECISION), CAST(:deliveryLatitude AS DOUBLE PRECISION)), 4326) AS geography) "
            + "    END "
            + "WHERE id = :orderId",
            nativeQuery = true)
    int syncLegacyDeliveryFields(
            @Param("orderId") Long orderId,
            @Param("deliveryAddressJson") String deliveryAddressJson,
            @Param("deliveryLatitude") BigDecimal deliveryLatitude,
            @Param("deliveryLongitude") BigDecimal deliveryLongitude
    );

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

    // ==================== STATS INTERNES (usage: partner-service stats catégories) ====================

    /**
     * Compte les commandes par jour pour une liste de partenaires depuis une date donnée.
     * Retourne des lignes [day (Date), count (Long)].
     */
    @Query(value = "SELECT DATE(o.created_at) AS day, COUNT(o.id) AS cnt " +
                   "FROM orders o " +
                   "WHERE o.partner_id IN :partnerIds " +
                   "AND o.created_at >= :since " +
                   "GROUP BY DATE(o.created_at) " +
                   "ORDER BY DATE(o.created_at)", nativeQuery = true)
    List<Object[]> countDailyByPartnerIdsSince(@Param("partnerIds") List<Long> partnerIds,
                                               @Param("since") LocalDateTime since);
}
