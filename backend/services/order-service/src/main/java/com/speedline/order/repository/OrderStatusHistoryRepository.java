package com.speedline.order.repository;

import com.speedline.order.domain.OrderStatus;
import com.speedline.order.domain.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour OrderStatusHistory
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /**
     * Trouver l'historique d'une commande (trié par date)
     */
    List<OrderStatusHistory> findByOrderIdOrderByTimestampAsc(Long orderId);

    /**
     * Trouver le dernier changement de statut d'une commande
     */
    Optional<OrderStatusHistory> findFirstByOrderIdOrderByTimestampDesc(Long orderId);

    /**
     * Trouver quand une commande a atteint un certain statut
     */
    Optional<OrderStatusHistory> findFirstByOrderIdAndStatus(Long orderId, OrderStatus status);

    /**
     * Supprimer l'historique d'une commande
     */
    void deleteByOrderId(Long orderId);

    /**
     * Compter les entrées d'historique pour une commande
     */
    long countByOrderId(Long orderId);

    /**
     * Calculer le temps moyen entre deux statuts
     */
    @Query("SELECT AVG(TIMESTAMPDIFF(MINUTE, h1.timestamp, h2.timestamp)) " +
           "FROM OrderStatusHistory h1, OrderStatusHistory h2 " +
           "WHERE h1.orderId = h2.orderId AND h1.status = :fromStatus AND h2.status = :toStatus " +
           "AND h2.timestamp > h1.timestamp")
    Double getAverageTimeBetweenStatuses(@Param("fromStatus") OrderStatus fromStatus,
                                          @Param("toStatus") OrderStatus toStatus);

    /**
     * Trouver les changements de statut dans une période
     */
    List<OrderStatusHistory> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
