package com.speedline.payment.repository;

import com.speedline.payment.domain.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour PaymentMethod
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

    List<PaymentMethodEntity> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<PaymentMethodEntity> findByUserIdAndIsDefaultTrue(Long userId);
    
    Optional<PaymentMethodEntity> findByStripePaymentMethodId(String stripePaymentMethodId);
    
    boolean existsByUserIdAndFingerprint(Long userId, String fingerprint);
    
    @Modifying
    @Query("UPDATE PaymentMethodEntity pm SET pm.isDefault = false WHERE pm.userId = :userId")
    int clearDefaultForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PaymentMethodEntity pm SET pm.isDefault = true WHERE pm.id = :id")
    int setAsDefault(@Param("id") Long id);
    
    long countByUserIdAndIsActiveTrue(Long userId);
}
