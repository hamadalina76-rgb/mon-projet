package com.speedline.user.repository;

import com.speedline.user.domain.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des AdminRoles
 */
@Repository
public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {
    
    @Query("SELECT r FROM AdminRole r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    Optional<AdminRole> findByCode(String code);
    
    boolean existsByCode(String code);
    
    @Query("SELECT DISTINCT r FROM AdminRole r LEFT JOIN FETCH r.permissions WHERE r.active = true")
    List<AdminRole> findByActiveTrue();
    
    @Query("SELECT DISTINCT r FROM AdminRole r LEFT JOIN FETCH r.permissions ORDER BY r.trustLevel DESC")
    List<AdminRole> findAllOrderByTrustLevelDesc();
    
    @Query("SELECT r FROM AdminRole r WHERE r.trustLevel >= :minTrustLevel")
    List<AdminRole> findByMinTrustLevel(Integer minTrustLevel);
}
