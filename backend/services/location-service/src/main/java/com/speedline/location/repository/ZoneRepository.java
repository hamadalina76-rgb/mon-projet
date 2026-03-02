package com.speedline.location.repository;

import com.speedline.location.domain.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Zone avec requêtes PostGIS
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /**
     * Trouver une zone par nom
     */
    Optional<Zone> findByName(String name);

    /**
     * Trouver une zone par nom et ville
     */
    Optional<Zone> findByNameAndCity(String name, String city);

    /**
     * Trouver les zones actives
     */
    List<Zone> findByIsActiveTrue();

    /**
     * Trouver les zones par type
     */
    List<Zone> findByTypeAndIsActiveTrue(Zone.ZoneType type);

    /**
     * Vérifier si un point est dans une zone
     * Utilise PostGIS ST_Contains
     */
    @Query(value = "SELECT z.* FROM adm_zones z WHERE " +
           "ST_Contains(ST_GeomFromGeoJSON(z.boundary_json), " +
           "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) " +
           "AND z.is_active = true", nativeQuery = true)
    List<Zone> findZonesContainingPoint(@Param("latitude") BigDecimal latitude,
                                         @Param("longitude") BigDecimal longitude);

    /**
     * Trouver la zone pour un point (première zone trouvée)
     */
    @Query(value = "SELECT z.* FROM adm_zones z WHERE " +
           "ST_Contains(ST_GeomFromGeoJSON(z.boundary_json), " +
           "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) " +
           "AND z.is_active = true " +
           "ORDER BY z.delivery_fee ASC LIMIT 1", nativeQuery = true)
    Optional<Zone> findZoneForPoint(@Param("latitude") BigDecimal latitude,
                                     @Param("longitude") BigDecimal longitude);

    /**
     * Trouver les zones actives avec pagination
     */
    Page<Zone> findByIsActiveTrue(Pageable pageable);

    /**
     * Trouver les zones inactives avec pagination
     */
    Page<Zone> findByIsActiveFalse(Pageable pageable);

    /**
     * Rechercher les zones par nom ou ville (filtrage backend)
     */
    @Query("SELECT z FROM Zone z WHERE (LOWER(z.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(z.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Zone> searchByNameOrCity(@Param("search") String search, Pageable pageable);

    /**
     * Rechercher les zones par nom ou ville ET statut actif/inactif
     */
    @Query("SELECT z FROM Zone z WHERE (LOWER(z.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(z.city) LIKE LOWER(CONCAT('%', :search, '%'))) AND z.isActive = :isActive")
    Page<Zone> searchByNameOrCityAndIsActive(@Param("search") String search, @Param("isActive") boolean isActive, Pageable pageable);

    /**
     * Trouver les zones par ville
     */
    List<Zone> findByCityAndIsActiveTrue(String city);

    /**
     * Vérifier si une zone chevauche d'autres zones actives
     * Utilise PostGIS ST_Overlaps ou ST_Intersects
     */
    @Query(value = "SELECT z.* FROM adm_zones z WHERE " +
           "z.id != :excludeZoneId AND z.is_active = true AND " +
           "ST_Intersects(ST_GeomFromGeoJSON(:boundaryJson), ST_GeomFromGeoJSON(z.boundary_json))",
           nativeQuery = true)
    List<Zone> findOverlappingZones(@Param("boundaryJson") String boundaryJson,
                                    @Param("excludeZoneId") Long excludeZoneId);

    /**
     * Calculer la surface d'une zone en km²
     */
    @Query(value = "SELECT ST_Area(CAST(ST_GeomFromGeoJSON(:boundaryJson) AS geography)) / 1000000.0",
           nativeQuery = true)
    BigDecimal calculateAreaKm2(@Param("boundaryJson") String boundaryJson);

    /**
     * Calculer le centre géométrique d'une zone
     */
    @Query(value = "SELECT ARRAY[ST_Y(ST_Centroid(ST_GeomFromGeoJSON(:boundaryJson))), " +
           "ST_X(ST_Centroid(ST_GeomFromGeoJSON(:boundaryJson)))]",
           nativeQuery = true)
    Double[] calculateCenter(@Param("boundaryJson") String boundaryJson);

    /**
     * Calculer le périmètre d'une zone en km
     */
    @Query(value = "SELECT ST_Perimeter(CAST(ST_GeomFromGeoJSON(:boundaryJson) AS geography)) / 1000.0",
           nativeQuery = true)
    BigDecimal calculatePerimeterKm(@Param("boundaryJson") String boundaryJson);

    /**
     * Vérifier si un polygone est valide (au moins 3 points, pas d'auto-intersection)
     */
    @Query(value = "SELECT ST_IsValid(ST_GeomFromGeoJSON(:boundaryJson))",
           nativeQuery = true)
    Boolean isValidPolygon(@Param("boundaryJson") String boundaryJson);
}
