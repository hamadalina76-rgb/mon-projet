package com.speedline.auth.repository;

import com.speedline.auth.domain.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    /**
     * Find the latest unused OTP for an email
     */
    Optional<Otp> findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    /**
     * Delete expired OTPs (cleanup)
     */
    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(LocalDateTime now);

    /**
     * Delete all OTPs for an email
     */
    void deleteByEmail(String email);
}
