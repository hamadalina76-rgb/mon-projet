package com.speedline.user.repository;

import com.speedline.user.domain.Admin;
import com.speedline.user.domain.AdminStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour la gestion des Admins
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    @Query("SELECT a FROM Admin a LEFT JOIN FETCH a.role r LEFT JOIN FETCH r.permissions WHERE a.userId = :userId")
    Optional<Admin> findByUserId(@Param("userId") Long userId);
    
    Optional<Admin> findByEmail(String email);
    
    boolean existsByUserId(Long userId);
    
    boolean existsByEmail(String email);
    
    Page<Admin> findByStatus(AdminStatus status, Pageable pageable);
    
    @Query("SELECT a FROM Admin a WHERE a.role.id = :roleId")
    Page<Admin> findByRoleId(@Param("roleId") Long roleId, Pageable pageable);
    
    @Query("SELECT a FROM Admin a WHERE " +
           "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Admin> searchAdmins(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Admin a WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(CAST(a.fullName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(CAST(a.email AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:roleId IS NULL OR a.role.id = :roleId)")
    Page<Admin> findWithFilters(
        @Param("search") String search,
        @Param("status") AdminStatus status,
        @Param("roleId") Long roleId,
        Pageable pageable
    );
    
    long countByStatus(AdminStatus status);
    
    long countByRoleId(Long roleId);
}
