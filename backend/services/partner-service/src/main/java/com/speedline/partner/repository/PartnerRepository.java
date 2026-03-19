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
 * Repository pour Partner.
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

    @Query("SELECT p FROM Partner p WHERE (:status IS NULL OR p.status = :status) AND " +
           "(LOWER(p.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(p.brandName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(p.city, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Partner> findByStatusAndSearch(@Param("status") PartnerStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);

    Page<Partner> findByIsActiveTrueAndAcceptsOrdersTrue(Pageable pageable);

    List<Partner> findByStatusAndIsActiveTrue(PartnerStatus status);

    // ==================== RECHERCHE PAR TYPE ====================

    Page<Partner> findByTypeAndIsActiveTrue(PartnerType type, Pageable pageable);

    // ==================== RECHERCHE PAR LOCALISATION ====================

    /**
     * Nearby paginé avec filtres combinables (AND) et tri dynamique.
     * Retourne [id, distance_km] pour éviter tout recalcul de distance côté Java.
     */
       @Query(value = """
                     SELECT
                            p.id,
                            ST_Distance(
                                   CAST(p.location AS geography),
                                   CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)
                            ) / 1000.0 AS distance_km
                     FROM partners p
                     WHERE p.is_active = true
                       AND p.location IS NOT NULL
                       AND ST_DWithin(
                                   CAST(p.location AS geography),
                                   CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                                   :radiusMeters
                       )
                       AND (
                                   :isOpen IS NULL
                                   OR (
                                          :isOpen = true
                                          AND COALESCE(p.accepts_orders, false) = true
                                          AND p.opening_hours_json IS NOT NULL
                                          AND BTRIM(p.opening_hours_json) <> ''
                                          AND BTRIM(p.opening_hours_json) <> '[]'
                                          AND EXISTS (
                                                 SELECT 1
                                                 FROM jsonb_array_elements(CAST(p.opening_hours_json AS jsonb)) oh
                                                 WHERE CAST(NULLIF(oh->>'dayOfWeek', '') AS integer) = CAST(EXTRACT(ISODOW FROM CURRENT_TIMESTAMP) AS integer)
                                                   AND COALESCE(CAST(NULLIF(oh->>'isClosed', '') AS boolean), false) = false
                                                   AND (
                                                               COALESCE(CAST(NULLIF(oh->>'is24Hours', '') AS boolean), false) = true
                                                               OR (
                                                                      (oh->>'openTime') IS NOT NULL
                                                                      AND (oh->>'closeTime') IS NOT NULL
                                                                      AND (
                                                                             (
                                                                                    CAST(NULLIF(oh->>'openTime', '') AS time) <= CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    AND LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                    AND LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                             )
                                                                             OR (
                                                                                    CAST(NULLIF(oh->>'openTime', '') AS time) > CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    AND (
                                                                                           LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                           OR LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    )
                                                                             )
                                                                      )
                                                               )
                                                   )
                                          )
                                   )
                                   OR (
                                          :isOpen = false
                                          AND (
                                                 COALESCE(p.accepts_orders, false) = false
                                                 OR p.opening_hours_json IS NULL
                                                 OR BTRIM(p.opening_hours_json) = ''
                                                 OR BTRIM(p.opening_hours_json) = '[]'
                                                 OR NOT EXISTS (
                                                        SELECT 1
                                                        FROM jsonb_array_elements(CAST(p.opening_hours_json AS jsonb)) oh
                                                        WHERE CAST(NULLIF(oh->>'dayOfWeek', '') AS integer) = CAST(EXTRACT(ISODOW FROM CURRENT_TIMESTAMP) AS integer)
                                                          AND COALESCE(CAST(NULLIF(oh->>'isClosed', '') AS boolean), false) = false
                                                          AND (
                                                                      COALESCE(CAST(NULLIF(oh->>'is24Hours', '') AS boolean), false) = true
                                                                      OR (
                                                                             (oh->>'openTime') IS NOT NULL
                                                                             AND (oh->>'closeTime') IS NOT NULL
                                                                             AND (
                                                                                    (
                                                                                           CAST(NULLIF(oh->>'openTime', '') AS time) <= CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                           AND LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                           AND LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    )
                                                                                    OR (
                                                                                           CAST(NULLIF(oh->>'openTime', '') AS time) > CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                           AND (
                                                                                                  LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                                  OR LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                           )
                                                                                    )
                                                                             )
                                                                      )
                                                          )
                                                 )
                                          )
                                   )
                       )
                       AND (:categoryRegex IS NULL OR COALESCE(p.category_ids, '') ~ :categoryRegex)
                       AND (:minRating IS NULL OR p.rating >= :minRating)
                       AND (:maxDeliveryTime IS NULL OR p.preparation_time <= :maxDeliveryTime)
                       AND (:freeDelivery IS NULL OR :freeDelivery = false OR p.delivery_fee = 0)
                     ORDER BY
                            CASE
                                   WHEN :sortBy = 'distance' THEN ST_Distance(
                                          CAST(p.location AS geography),
                                          CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)
                                   ) / 1000.0
                            END ASC NULLS LAST,
                            CASE WHEN :sortBy = 'rating' THEN p.rating END DESC NULLS LAST,
                            CASE WHEN :sortBy = 'deliveryTime' THEN p.preparation_time END ASC NULLS LAST,
                            CASE WHEN :sortBy = 'popularity' THEN p.total_orders END DESC NULLS LAST,
                            p.id ASC
                     LIMIT :size OFFSET :offset
                     """, nativeQuery = true)
    List<Object[]> findNearbyWithFilters(@Param("lat") double lat,
                                         @Param("lng") double lng,
                                         @Param("radiusMeters") double radiusMeters,
                                         @Param("isOpen") Boolean isOpen,
                                         @Param("categoryRegex") String categoryRegex,
                                         @Param("minRating") BigDecimal minRating,
                                         @Param("maxDeliveryTime") Integer maxDeliveryTime,
                                         @Param("freeDelivery") Boolean freeDelivery,
                                         @Param("sortBy") String sortBy,
                                         @Param("size") int size,
                                         @Param("offset") int offset);

       @Query(value = """
                     SELECT COUNT(*)
                     FROM partners p
                     WHERE p.is_active = true
                       AND p.location IS NOT NULL
                       AND ST_DWithin(
                                   CAST(p.location AS geography),
                                   CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                                   :radiusMeters
                       )
                       AND (
                                   :isOpen IS NULL
                                   OR (
                                          :isOpen = true
                                          AND COALESCE(p.accepts_orders, false) = true
                                          AND p.opening_hours_json IS NOT NULL
                                          AND BTRIM(p.opening_hours_json) <> ''
                                          AND BTRIM(p.opening_hours_json) <> '[]'
                                          AND EXISTS (
                                                 SELECT 1
                                                 FROM jsonb_array_elements(CAST(p.opening_hours_json AS jsonb)) oh
                                                 WHERE CAST(NULLIF(oh->>'dayOfWeek', '') AS integer) = CAST(EXTRACT(ISODOW FROM CURRENT_TIMESTAMP) AS integer)
                                                   AND COALESCE(CAST(NULLIF(oh->>'isClosed', '') AS boolean), false) = false
                                                   AND (
                                                               COALESCE(CAST(NULLIF(oh->>'is24Hours', '') AS boolean), false) = true
                                                               OR (
                                                                      (oh->>'openTime') IS NOT NULL
                                                                      AND (oh->>'closeTime') IS NOT NULL
                                                                      AND (
                                                                             (
                                                                                    CAST(NULLIF(oh->>'openTime', '') AS time) <= CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    AND LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                    AND LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                             )
                                                                             OR (
                                                                                    CAST(NULLIF(oh->>'openTime', '') AS time) > CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    AND (
                                                                                           LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                           OR LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    )
                                                                             )
                                                                      )
                                                               )
                                                   )
                                          )
                                   )
                                   OR (
                                          :isOpen = false
                                          AND (
                                                 COALESCE(p.accepts_orders, false) = false
                                                 OR p.opening_hours_json IS NULL
                                                 OR BTRIM(p.opening_hours_json) = ''
                                                 OR BTRIM(p.opening_hours_json) = '[]'
                                                 OR NOT EXISTS (
                                                        SELECT 1
                                                        FROM jsonb_array_elements(CAST(p.opening_hours_json AS jsonb)) oh
                                                        WHERE CAST(NULLIF(oh->>'dayOfWeek', '') AS integer) = CAST(EXTRACT(ISODOW FROM CURRENT_TIMESTAMP) AS integer)
                                                          AND COALESCE(CAST(NULLIF(oh->>'isClosed', '') AS boolean), false) = false
                                                          AND (
                                                                      COALESCE(CAST(NULLIF(oh->>'is24Hours', '') AS boolean), false) = true
                                                                      OR (
                                                                             (oh->>'openTime') IS NOT NULL
                                                                             AND (oh->>'closeTime') IS NOT NULL
                                                                             AND (
                                                                                    (
                                                                                           CAST(NULLIF(oh->>'openTime', '') AS time) <= CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                           AND LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                           AND LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                    )
                                                                                    OR (
                                                                                           CAST(NULLIF(oh->>'openTime', '') AS time) > CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                           AND (
                                                                                                  LOCALTIME >= CAST(NULLIF(oh->>'openTime', '') AS time)
                                                                                                  OR LOCALTIME < CAST(NULLIF(oh->>'closeTime', '') AS time)
                                                                                           )
                                                                                    )
                                                                             )
                                                                      )
                                                          )
                                                 )
                                          )
                                   )
                       )
                       AND (:categoryRegex IS NULL OR COALESCE(p.category_ids, '') ~ :categoryRegex)
                       AND (:minRating IS NULL OR p.rating >= :minRating)
                       AND (:maxDeliveryTime IS NULL OR p.preparation_time <= :maxDeliveryTime)
                       AND (:freeDelivery IS NULL OR :freeDelivery = false OR p.delivery_fee = 0)
                     """, nativeQuery = true)
    long countNearbyWithFilters(@Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("radiusMeters") double radiusMeters,
                                @Param("isOpen") Boolean isOpen,
                                @Param("categoryRegex") String categoryRegex,
                                @Param("minRating") BigDecimal minRating,
                                @Param("maxDeliveryTime") Integer maxDeliveryTime,
                                @Param("freeDelivery") Boolean freeDelivery);

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
    int updateRating(@Param("partnerId") Long partnerId,
                     @Param("rating") BigDecimal rating,
                     @Param("totalRatings") Integer totalRatings);

    @Modifying
    @Query("UPDATE Partner p SET p.totalOrders = p.totalOrders + 1, p.totalRevenue = p.totalRevenue + :amount WHERE p.id = :partnerId")
    int incrementOrderCount(@Param("partnerId") Long partnerId, @Param("amount") BigDecimal amount);

    // ==================== STATISTIQUES ====================

    long countByStatus(PartnerStatus status);

    long countByIsActiveTrue();

    @Query("SELECT COUNT(p) FROM Partner p WHERE p.categoryIds LIKE %:categoryId% " +
           "AND p.createdAt BETWEEN :start AND :end")
    long countByCategoryIdAndCreatedAtBetween(@Param("categoryId") String categoryId,
                                              @Param("start") java.time.LocalDateTime start,
                                              @Param("end") java.time.LocalDateTime end);

    @Query("SELECT DISTINCT p.city FROM Partner p WHERE p.isActive = true ORDER BY p.city")
    List<String> findDistinctCities();
}
