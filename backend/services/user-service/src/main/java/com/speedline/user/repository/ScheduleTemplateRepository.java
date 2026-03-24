package com.speedline.user.repository;

import com.speedline.user.domain.ScheduleTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleTemplateRepository extends JpaRepository<ScheduleTemplate, Long> {
    List<ScheduleTemplate> findByIsActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);

    Page<ScheduleTemplate> findByIsActive(Boolean isActive, Pageable pageable);

    @Query("SELECT t FROM ScheduleTemplate t WHERE " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ScheduleTemplate> findBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM ScheduleTemplate t WHERE " +
           "(LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND t.isActive = :isActive")
    Page<ScheduleTemplate> findBySearchAndIsActive(@Param("search") String search, @Param("isActive") Boolean isActive, Pageable pageable);
}
