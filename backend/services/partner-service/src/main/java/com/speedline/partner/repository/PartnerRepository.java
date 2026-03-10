package com.speedline.partner.repository;

import com.speedline.partner.domain.Partner;
import com.speedline.partner.domain.PartnerStatus;
import com.speedline.partner.domain.PartnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Partner
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    Optional<Partner> findBySlug(String slug);
    
    Optional<Partner> findByUserId(Long userId);
    
    boolean existsBySlug(String slug);
    
    boolean existsByUserId(Long userId);

    // ==================== RECHERCHE PAR STATUT ====================

    Page<Partner> findByStatus(PartnerStatus status, Pageable pageable);

    /**
     * Recherche admin : par statut (optionnel) et par nom/ville (businessName, brandName, city).
     * search ne doit pas être null ni vide (à gérer en service).
     */
    @Query("SELECT p FROM Partner p WHERE (:status IS NULL OR p.status = :status) AND " +
           "(LOWER(p.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(p.brandName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(p.city, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> findByStatusAndSearch(@Param("status") PartnerStatus status, @Param("search") String search, Pageable pageable);
    
    Page<Partner> findByIsActiveTrueAndAcceptsOrdersTrue(Pageable pageable);
    
    List<Partner> findByStatusAndIsActiveTrue(PartnerStatus status);

    // ==================== RECHERCHE PAR TYPE ====================

    Page<Partner> findByTypeAndIsActiveTrue(PartnerType type, Pageable pageable);

    // ==================== RECHERCHE PAR LOCALISATION ====================

    @Query(value = """
            SELECT id,
                   ST_Distance(CAST(ST_MakePoint(CAST(longitude AS float8), CAST(latitude AS float8)) AS geography),
                               CAST(ST_MakePoint(:lng, :lat) AS geography)) / 1000.0 AS distance_km
            FROM partners
            WHERE is_active = true
              AND latitude IS NOT NULL AND longitude IS NOT NULL
              AND ST_DWithin(CAST(ST_MakePoint(CAST(longitude AS float8), CAST(latitude AS float8)) AS geography),
                             CAST(ST_MakePoint(:lng, :lat) AS geography), :radiusMeters)
            ORDER BY CASE WHEN accepts_orders THEN 0 ELSE 1 END ASC,
                     distance_km ASC,
                     rating DESC NULLS LAST
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findNearbyPartnersSorted(
            @Param("lat") double lat, @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("size") int size, @Param("offset") int offset);

    @Query(value = """
            SELECT COUNT(*) FROM partners
            WHERE is_active = true
              AND latitude IS NOT NULL AND longitude IS NOT NULL
              AND ST_DWithin(CAST(ST_MakePoint(CAST(longitude AS float8), CAST(latitude AS float8)) AS geography),
                             CAST(ST_MakePoint(:lng, :lat) AS geography), :radiusMeters)
            """, nativeQuery = true)
    long countNearbyPartners(
            @Param("lat") double lat, @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters);

    Page<Partner> findByCityAndIsActiveTrue(String city, Pageable pageable);

    // ==================== RECHERCHE PAR CATÉGORIE ====================

    @Query("SELECT p FROM Partner p WHERE p.isActive = true AND p.categoryIds LIKE %:categoryId%")
    Page<Partner> findByCategoryId(@Param("categoryId") String categoryId, Pageable pageable);

    // ==================== RECHERCHE ET TRI ====================

    @Query("SELECT p FROM Partner p WHERE p.isActive = true ORDER BY p.rating DESC")
    Page<Partner> findTopRated(Pageable pageable);

    @Query("SELECT p FROM Partner p WHERE p.isActive = true AND p.isFeatured = true")
    List<Partner> findFeaturedPartners();

    @Query("SELECT p FROM Partner p WHERE p.isActive = true AND p.isPremium = true")
    Page<Partner> findPremiumPartners(Pageable pageable);

    // ==================== RECHERCHE TEXTUELLE ====================

    @Query("SELECT p FROM Partner p WHERE p.isActive = true AND " +
           "(LOWER(p.businessName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Partner> searchPartners(@Param("query") String query, Pageable pageable);

    // ==================== MISE À JOUR ====================

    @Modifying
    @Query("UPDATE Partner p SET p.acceptsOrders = :accepts WHERE p.id = :partnerId")
    int updateAcceptsOrders(@Param("partnerId") Long partnerId, @Param("accepts") Boolean accepts);

    @Modifying
    @Query("UPDATE Partner p SET p.status = :status WHERE p.id = :partnerId")
    int updateStatus(@Param("partnerId") Long partnerId, @Param("status") PartnerStatus status);

    @Modifying
    @Query("UPDATE Partner p SET p.rating = :rating, p.totalRatings = :totalRatings WHERE p.id = :partnerId")
    int updateRating(@Param("partnerId") Long partnerId, @Param("rating") BigDecimal rating, @Param("totalRatings") Integer totalRatings);

    @Modifying
    @Query("UPDATE Partner p SET p.totalOrders = p.totalOrders + 1, p.totalRevenue = p.totalRevenue + :amount WHERE p.id = :partnerId")
    int incrementOrderCount(@Param("partnerId") Long partnerId, @Param("amount") BigDecimal amount);

    // ==================== STATISTIQUES ====================

    long countByStatus(PartnerStatus status);
    
    long countByIsActiveTrue();

    /** Nombre de partenaires liés à une catégorie créés entre deux dates (pour calcul de tendance). */
    @Query("SELECT COUNT(p) FROM Partner p WHERE p.categoryIds LIKE %:categoryId% " +
           "AND p.createdAt BETWEEN :start AND :end")
    long countByCategoryIdAndCreatedAtBetween(@Param("categoryId") String categoryId,
                                              @Param("start") java.time.LocalDateTime start,
                                              @Param("end") java.time.LocalDateTime end);

    @Query("SELECT DISTINCT p.city FROM Partner p WHERE p.isActive = true ORDER BY p.city")
    List<String> findDistinctCities();
}
