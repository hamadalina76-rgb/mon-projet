package com.speedline.partner.repository;

import com.speedline.partner.domain.Category;
import com.speedline.partner.domain.Category.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    Optional<Category> findBySlug(String slug);
    
    boolean existsBySlug(String slug);

    // ==================== RECHERCHE PAR TYPE ====================

    List<Category> findByCategoryTypeAndIsActiveTrueOrderByDisplayOrderAsc(CategoryType categoryType);
    
    Page<Category> findByCategoryTypeAndIsActiveTrue(CategoryType categoryType, Pageable pageable);

    // ==================== RECHERCHE PAR PARENT ====================

    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(Long parentId);
    
    List<Category> findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    // ==================== CATÉGORIES FEATURED ====================

    List<Category> findByIsFeaturedTrueAndIsActiveTrueOrderByDisplayOrderAsc();

    // ==================== MISE À JOUR ====================

    @Modifying
    @Query("UPDATE Category c SET c.partnerCount = c.partnerCount + 1 WHERE c.id = :categoryId")
    int incrementPartnerCount(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Category c SET c.partnerCount = c.partnerCount - 1 WHERE c.id = :categoryId AND c.partnerCount > 0")
    int decrementPartnerCount(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Category c SET c.productCount = c.productCount + 1 WHERE c.id = :categoryId")
    int incrementProductCount(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Category c SET c.productCount = c.productCount - 1 WHERE c.id = :categoryId AND c.productCount > 0")
    int decrementProductCount(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Category c SET c.isActive = :active WHERE c.id = :categoryId")
    int updateActiveStatus(@Param("categoryId") Long categoryId, @Param("active") Boolean active);
}
