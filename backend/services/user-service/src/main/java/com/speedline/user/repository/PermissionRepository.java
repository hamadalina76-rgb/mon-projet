package com.speedline.user.repository;

import com.speedline.user.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour la gestion des Permissions
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    List<Permission> findByRoleId(Long roleId);
    
    List<Permission> findByModule(String module);
    
    List<Permission> findByEnabledTrue();
    
    @Query("SELECT p FROM Permission p WHERE p.role.id = :roleId AND p.enabled = true")
    List<Permission> findEnabledByRoleId(@Param("roleId") Long roleId);
}
