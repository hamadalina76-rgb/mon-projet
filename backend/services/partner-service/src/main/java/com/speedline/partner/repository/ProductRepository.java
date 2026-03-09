package com.speedline.partner.repository;

import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository pour Product
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ==================== RECHERCHE PAR PARTENAIRE ====================

    List<Product> findByPartnerIdAndStatusNot(Long partnerId, ProductStatus status);
    
    Page<Product> findByPartnerIdAndStatusNot(Long partnerId, ProductStatus status, Pageable pageable);
    
    List<Product> findByPartnerIdAndIsAvailableTrue(Long partnerId);

    // ==================== RECHERCHE PAR CATÉGORIE ====================

    List<Product> findByCategoryIdAndStatusNot(Long categoryId, ProductStatus status);
    
    Page<Product> findByCategoryIdAndStatusNot(Long categoryId, ProductStatus status, Pageable pageable);
    
    List<Product> findByPartnerIdAndCategoryIdAndStatusNot(Long partnerId, Long categoryId, ProductStatus status);

    /**
     * Produits d'une catégorie triés par displayOrder — utilisé par le menu API.
     */
    List<Product> findByPartnerIdAndCategoryIdAndStatusNotOrderByDisplayOrderAsc(
            Long partnerId, Long categoryId, ProductStatus status);

    /**
     * Compte les produits actifs d'une catégorie — utilisé pour le soft-delete de catégorie (TC-11).
     */
    long countByPartnerIdAndCategoryIdAndStatusNot(Long partnerId, Long categoryId, ProductStatus status);

    /**
     * Dernier produit d'un partenaire (displayOrder le plus élevé) — pour l'auto-position (TC-18 pattern).
     */
    java.util.Optional<Product> findTopByPartnerIdAndStatusNotOrderByDisplayOrderDesc(
            Long partnerId, ProductStatus status);

    // ==================== RECHERCHE PAR CARACTÉRISTIQUES ====================

    List<Product> findByPartnerIdAndIsPopularTrue(Long partnerId);
    
    List<Product> findByPartnerIdAndIsNewTrue(Long partnerId);
    
    List<Product> findByPartnerIdAndIsFeaturedTrue(Long partnerId);
    
    @Query("SELECT p FROM Product p WHERE p.partnerId = :partnerId AND p.originalPrice IS NOT NULL AND p.originalPrice > p.price")
    List<Product> findPromotionalProducts(@Param("partnerId") Long partnerId);

    // ==================== RECHERCHE PAR RÉGIME ALIMENTAIRE ====================

    List<Product> findByPartnerIdAndIsVegetarianTrue(Long partnerId);
    
    List<Product> findByPartnerIdAndIsVeganTrue(Long partnerId);
    
    List<Product> findByPartnerIdAndIsHalalTrue(Long partnerId);
    
    List<Product> findByPartnerIdAndIsGlutenFreeTrue(Long partnerId);

    // ==================== RECHERCHE TEXTUELLE ====================

    @Query("SELECT p FROM Product p WHERE p.partnerId = :partnerId AND p.status != 'DELETED' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchProducts(@Param("partnerId") Long partnerId, @Param("query") String query);

    @Query("SELECT p.id FROM Product p WHERE p.partnerId = :partnerId AND p.status != 'DELETED'")
    List<Long> findProductIdsByPartnerId(@Param("partnerId") Long partnerId);

    /**
     * Page de produits avec filtres optionnels (recherche, catégorie, disponibilité, stock faible).
     * lowStockIds : si non null et non vide, ne garde que les produits dont l'id est dans la liste.
     */
    @Query("SELECT p FROM Product p WHERE p.partnerId = :partnerId AND p.status <> com.speedline.partner.domain.ProductStatus.DELETED " +
           "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:isAvailable IS NULL OR p.isAvailable = :isAvailable) " +
           "AND (:lowStockIds IS NULL OR p.id IN :lowStockIds) " +
           "ORDER BY p.displayOrder ASC")
    Page<Product> findProductsPage(
            @Param("partnerId") Long partnerId,
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("isAvailable") Boolean isAvailable,
            @Param("lowStockIds") List<Long> lowStockIds,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status != 'DELETED' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchAllProducts(@Param("query") String query, Pageable pageable);

    // ==================== MISE À JOUR ====================

    @Modifying
    @Query("UPDATE Product p SET p.isAvailable = :available WHERE p.id = :productId")
    int updateAvailability(@Param("productId") Long productId, @Param("available") Boolean available);

    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.id = :productId")
    int updateStatus(@Param("productId") Long productId, @Param("status") ProductStatus status);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int decrementStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.orderCount = p.orderCount + 1 WHERE p.id = :productId")
    int incrementOrderCount(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Product p SET p.rating = :rating, p.totalRatings = :totalRatings WHERE p.id = :productId")
    int updateRating(@Param("productId") Long productId, @Param("rating") BigDecimal rating, @Param("totalRatings") Integer totalRatings);

    // ==================== STATISTIQUES ====================

    long countByPartnerId(Long partnerId);
    
    long countByPartnerIdAndIsAvailableTrue(Long partnerId);
    
    long countByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.partnerId = :partnerId ORDER BY p.orderCount DESC")
    List<Product> findTopSellingProducts(@Param("partnerId") Long partnerId, Pageable pageable);
}
