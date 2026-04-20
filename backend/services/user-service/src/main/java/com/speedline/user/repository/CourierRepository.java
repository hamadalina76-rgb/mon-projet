package com.speedline.user.repository;

import com.speedline.user.domain.Courier;
import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.CourierType;
import com.speedline.user.domain.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Courier
 * Fournit les opérations CRUD et requêtes personnalisées
 */
@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    /**
     * Trouver un livreur par son userId (auth-service)
     * @param userId ID de l'utilisateur dans auth-service
     * @return Optional contenant le livreur si trouvé
     */
    Optional<Courier> findByUserId(Long userId);

    /**
     * Vérifier si un livreur existe pour cet userId
     * @param userId ID de l'utilisateur
     * @return true si existe
     */
    boolean existsByUserId(Long userId);

    // ==================== RECHERCHE PAR DISPONIBILITÉ ====================

    /**
     * Trouver tous les livreurs disponibles et en ligne
     * @return Liste des livreurs disponibles
     */
    List<Courier> findByIsAvailableTrueAndIsOnlineTrue();

    /**
     * Trouver les livreurs disponibles avec pagination
     * @param pageable Pagination
     * @return Page de livreurs disponibles
     */
    Page<Courier> findByIsAvailableTrueAndIsOnlineTrue(Pageable pageable);

    /**
     * Trouver les livreurs en ligne
     * @return Liste des livreurs en ligne
     */
    List<Courier> findByIsOnlineTrue();

    /**
     * Compter les livreurs disponibles
     * @return Nombre de livreurs disponibles
     */
    long countByIsAvailableTrueAndIsOnlineTrue();

    // ==================== RECHERCHE PAR STATUT ====================

    /**
     * Trouver les livreurs par statut
     * @param status Statut du compte
     * @param pageable Pagination
     * @return Page de livreurs
     */
    Page<Courier> findByStatus(CourierStatus status, Pageable pageable);

    /**
     * Trouver les livreurs en attente de validation
     * @return Liste des livreurs à valider
     */
    List<Courier> findByStatusAndDocumentsVerifiedFalse(CourierStatus status);

    /**
     * Compter les livreurs par statut
     * @param status Statut à compter
     * @return Nombre de livreurs
     */
    long countByStatus(CourierStatus status);

    // ==================== RECHERCHE PAR VÉHICULE ====================

    /**
     * Trouver les livreurs par type de véhicule
     * @param vehicleType Type de véhicule
     * @param pageable Pagination
     * @return Page de livreurs
     */
    Page<Courier> findByVehicleType(VehicleType vehicleType, Pageable pageable);

    /**
     * Trouver les livreurs disponibles par type de véhicule
     * @param vehicleType Type de véhicule
     * @return Liste des livreurs disponibles avec ce véhicule
     */
    List<Courier> findByVehicleTypeAndIsAvailableTrueAndIsOnlineTrue(VehicleType vehicleType);

    // ==================== RECHERCHE GÉOGRAPHIQUE ====================

    /**
     * Trouver les livreurs disponibles dans un rayon (en degrés)
     * Approximation: 0.01 degré ≈ 1.1 km
     * @param latitude Latitude du point central
     * @param longitude Longitude du point central
     * @param radiusDegrees Rayon en degrés
     * @return Liste des livreurs proches et disponibles
     */
    @Query("SELECT c FROM Courier c WHERE c.isAvailable = true AND c.isOnline = true " +
           "AND c.currentLatitude IS NOT NULL AND c.currentLongitude IS NOT NULL " +
           "AND c.currentLatitude BETWEEN :latitude - :radiusDegrees AND :latitude + :radiusDegrees " +
           "AND c.currentLongitude BETWEEN :longitude - :radiusDegrees AND :longitude + :radiusDegrees " +
           "ORDER BY ABS(c.currentLatitude - :latitude) + ABS(c.currentLongitude - :longitude)")
    List<Courier> findAvailableCouriersNearby(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusDegrees") BigDecimal radiusDegrees);

    /**
     * Trouver les livreurs dans une zone spécifique
     * @param zone Nom de la zone
     * @param pageable Pagination
     * @return Page de livreurs dans cette zone
     */
    Page<Courier> findByPreferredDeliveryZone(String zone, Pageable pageable);

    // ==================== RECHERCHE PAR PERFORMANCE ====================

    /**
     * Trouver les meilleurs livreurs (par note)
     * @param minRating Note minimale
     * @param pageable Pagination
     * @return Page des meilleurs livreurs
     */
    @Query("SELECT c FROM Courier c WHERE c.rating >= :minRating AND c.status = 'ACTIVE' ORDER BY c.rating DESC")
    Page<Courier> findTopRatedCouriers(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * Trouver les livreurs avec le plus de livraisons
     * @param pageable Pagination
     * @return Page de livreurs
     */
    @Query("SELECT c FROM Courier c WHERE c.status = 'ACTIVE' ORDER BY c.totalDeliveries DESC")
    Page<Courier> findMostActiveCouriers(Pageable pageable);

    // ==================== MISE À JOUR ====================

    /**
     * Mettre à jour la position d'un livreur
     * @param courierId ID du livreur
     * @param latitude Nouvelle latitude
     * @param longitude Nouvelle longitude
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.currentLatitude = :latitude, c.currentLongitude = :longitude, " +
           "c.lastLocationUpdate = CURRENT_TIMESTAMP WHERE c.id = :courierId")
    int updateLocation(@Param("courierId") Long courierId,
                       @Param("latitude") BigDecimal latitude,
                       @Param("longitude") BigDecimal longitude);

    /**
     * Mettre à jour la disponibilité
     * @param courierId ID du livreur
     * @param isAvailable Disponibilité
     * @param isOnline En ligne
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.isAvailable = :isAvailable, c.isOnline = :isOnline WHERE c.id = :courierId")
    int updateAvailability(@Param("courierId") Long courierId,
                           @Param("isAvailable") Boolean isAvailable,
                           @Param("isOnline") Boolean isOnline);

    /**
     * Mettre à jour le statut
     * @param courierId ID du livreur
     * @param status Nouveau statut
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.status = :status WHERE c.id = :courierId")
    int updateStatus(@Param("courierId") Long courierId, @Param("status") CourierStatus status);

    /**
     * Mettre à jour la note moyenne
     * @param courierId ID du livreur
     * @param newRating Nouvelle note moyenne
     * @param totalRatings Nouveau total d'évaluations
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.rating = :newRating, c.totalRatings = :totalRatings WHERE c.id = :courierId")
    int updateRating(@Param("courierId") Long courierId,
                     @Param("newRating") BigDecimal newRating,
                     @Param("totalRatings") Integer totalRatings);

    /**
     * Marquer les documents comme vérifiés
     * @param courierId ID du livreur
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.documentsVerified = true, c.status = 'ACTIVE' WHERE c.id = :courierId")
    int verifyDocuments(@Param("courierId") Long courierId);

    /**
     * Incrémenter les statistiques après une livraison
     * @param courierId ID du livreur
     * @param earnings Gains de la livraison
     * @param distance Distance parcourue
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.totalDeliveries = c.totalDeliveries + 1, " +
           "c.successfulDeliveries = c.successfulDeliveries + 1, " +
           "c.totalEarnings = c.totalEarnings + :earnings, " +
           "c.weeklyEarnings = c.weeklyEarnings + :earnings, " +
           "c.availableBalance = c.availableBalance + :earnings, " +
           "c.totalDistanceTravelled = c.totalDistanceTravelled + :distance " +
           "WHERE c.id = :courierId")
    int incrementDeliveryStats(@Param("courierId") Long courierId,
                               @Param("earnings") BigDecimal earnings,
                               @Param("distance") BigDecimal distance);

    /**
     * Réinitialiser les gains hebdomadaires (appelé par scheduler)
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Courier c SET c.weeklyEarnings = 0")
    int resetWeeklyEarnings();

    // ==================== RECHERCHE AVANCÉE ====================

    /**
     * Rechercher des livreurs par IDs
     * @param userIds Liste des userIds à rechercher
     * @return Liste des livreurs correspondants
     */
    @Query("SELECT c FROM Courier c WHERE c.userId IN :userIds")
    List<Courier> findByUserIdIn(@Param("userIds") List<Long> userIds);

    /**
     * Trouver les livreurs dont le permis expire bientôt
     * @param expiryDate Date limite d'expiration
     * @return Liste des livreurs concernés
     */
    @Query("SELECT c FROM Courier c WHERE c.drivingLicenseExpiry IS NOT NULL AND c.drivingLicenseExpiry <= :expiryDate")
    List<Courier> findCouriersWithExpiringLicense(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Recherche admin : par statut et optionnellement par terme (immatriculation, CIN, permis).
     * Si search est vide, les conditions LIKE sont ignorées (termes vrais).
     */
    @Query("SELECT c FROM Courier c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:courierType IS NULL OR c.courierType = :courierType) AND " +
           "(:zoneId IS NULL OR EXISTS (" +
           "  SELECT 1 FROM Courier c2 JOIN c2.assignedZoneIds z " +
           "  WHERE c2.id = c.id AND z = :zoneId" +
           ")) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(COALESCE(c.vehicleNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.identityNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.drivingLicenseNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Courier> searchCouriers(@Param("status") CourierStatus status,
                                 @Param("courierType") CourierType courierType,
                                 @Param("zoneId") Long zoneId,
                                 @Param("search") String search,
                                 Pageable pageable);

    @Query(value = "SELECT DISTINCT c.* FROM couriers c " +
           "JOIN courier_assigned_zones caz ON caz.courier_id = c.id " +
           "WHERE caz.zone_id = :zoneId " +
           "AND (:status IS NULL OR CAST(c.status AS text) = :status) " +
           "AND (:courierType IS NULL OR CAST(c.courier_type AS text) = :courierType) " +
           "AND (:search IS NULL OR :search = '' " +
           "OR LOWER(COALESCE(c.vehicle_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.identity_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.driving_license_number, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.created_at DESC",
           countQuery = "SELECT COUNT(DISTINCT c.id) FROM couriers c " +
                  "JOIN courier_assigned_zones caz ON caz.courier_id = c.id " +
                  "WHERE caz.zone_id = :zoneId " +
                  "AND (:status IS NULL OR CAST(c.status AS text) = :status) " +
                  "AND (:courierType IS NULL OR CAST(c.courier_type AS text) = :courierType) " +
                  "AND (:search IS NULL OR :search = '' " +
                  "OR LOWER(COALESCE(c.vehicle_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                  "OR LOWER(COALESCE(c.identity_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                  "OR LOWER(COALESCE(c.driving_license_number, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Courier> searchCouriersByZoneNative(@Param("status") String status,
                                        @Param("courierType") String courierType,
                                        @Param("zoneId") Long zoneId,
                                        @Param("search") String search,
                                        Pageable pageable);

    @Query(value = "SELECT DISTINCT c.* FROM couriers c " +
           "JOIN courier_assigned_zones caz ON caz.courier_id = c.id " +
           "WHERE caz.zone_id = :zoneId " +
           "AND (:search IS NULL OR :search = '' " +
           "OR LOWER(COALESCE(c.vehicle_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.identity_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.driving_license_number, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.created_at DESC",
           countQuery = "SELECT COUNT(DISTINCT c.id) FROM couriers c " +
                  "JOIN courier_assigned_zones caz ON caz.courier_id = c.id " +
                  "WHERE caz.zone_id = :zoneId " +
                  "AND (:search IS NULL OR :search = '' " +
                  "OR LOWER(COALESCE(c.vehicle_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                  "OR LOWER(COALESCE(c.identity_number, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
                  "OR LOWER(COALESCE(c.driving_license_number, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<Courier> searchCouriersByZoneNativeNoEnum(@Param("zoneId") Long zoneId,
                                              @Param("search") String search,
                                              Pageable pageable);
}
