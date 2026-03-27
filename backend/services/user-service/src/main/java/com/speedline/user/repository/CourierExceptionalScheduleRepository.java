package com.speedline.user.repository;

import com.speedline.user.domain.CourierExceptionalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CourierExceptionalScheduleRepository
        extends JpaRepository<CourierExceptionalSchedule, Long>,
                JpaSpecificationExecutor<CourierExceptionalSchedule> {

    /**
     * Détecte un chevauchement de périodes pour un même livreur.
     * Deux périodes [A,B] et [C,D] se chevauchent si A &lt;= D AND C &lt;= B.
     */
    @Query("""
            SELECT e FROM CourierExceptionalSchedule e
            WHERE e.courierId = :courierId
              AND e.isActive = true
              AND e.startDate <= :endDate
              AND e.endDate   >= :startDate
              AND (:excludeId IS NULL OR e.id <> :excludeId)
            ORDER BY e.startDate
            """)
    List<CourierExceptionalSchedule> findOverlapping(
            @Param("courierId")  Long      courierId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate,
            @Param("excludeId")  Long      excludeId);

    /**
     * Retourne les exceptions actives d'un livreur qui intersectent la plage
     * [fromDate, toDate] – utilisé pour les badges calendrier.
     */
    @Query("""
            SELECT e FROM CourierExceptionalSchedule e
            WHERE e.courierId = :courierId
              AND e.isActive = true
              AND e.startDate <= :toDate
              AND e.endDate   >= :fromDate
            ORDER BY e.startDate
            """)
    List<CourierExceptionalSchedule> findActiveInRange(
            @Param("courierId") Long      courierId,
            @Param("fromDate")  LocalDate fromDate,
            @Param("toDate")    LocalDate toDate);

    /**
     * Retourne la première exception active couvrant exactement une date donnée.
     */
    @Query("""
            SELECT e FROM CourierExceptionalSchedule e
            WHERE e.courierId = :courierId
              AND e.isActive = true
              AND e.startDate <= :date
              AND e.endDate   >= :date
            ORDER BY e.startDate
            """)
    List<CourierExceptionalSchedule> findActiveOnDate(
            @Param("courierId") Long      courierId,
            @Param("date")      LocalDate date);
}
