package com.speedline.payment.repository;

import com.speedline.payment.domain.Payment;
import com.speedline.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository pour Payment
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Payment p SET p.status = :status WHERE p.id = :paymentId")
    int updateStatus(@Param("paymentId") Long paymentId, @Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end")
    BigDecimal getTotalRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
