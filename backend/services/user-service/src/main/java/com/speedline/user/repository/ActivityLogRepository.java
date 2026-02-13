package com.speedline.user.repository;

import com.speedline.user.domain.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repository pour les logs d'activité
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    /**
     * Récupère tous les logs avec pagination (triés par date desc)
     */
    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * Récupère les logs d'un admin spécifique
     */
    Page<ActivityLog> findByAdminIdOrderByTimestampDesc(Long adminId, Pageable pageable);
    
    /**
     * Récupère les logs par type de ressource
     */
    Page<ActivityLog> findByResourceOrderByTimestampDesc(String resource, Pageable pageable);
    
    /**
     * Recherche avancée avec filtres
     */
    @Query("SELECT a FROM ActivityLog a WHERE " +
           "(:adminId IS NULL OR a.adminId = :adminId) AND " +
           "(:resource IS NULL OR a.resource = :resource) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<ActivityLog> findWithFilters(
        @Param("adminId") Long adminId,
        @Param("resource") String resource,
        @Param("action") String action,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}
