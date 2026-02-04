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
    @Query(value = "SELECT z.* FROM zones z WHERE " +
           "ST_Contains(ST_GeomFromGeoJSON(z.boundary_json), " +
           "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) " +
           "AND z.is_active = true", nativeQuery = true)
    List<Zone> findZonesContainingPoint(@Param("latitude") BigDecimal latitude,
                                         @Param("longitude") BigDecimal longitude);

    /**
     * Trouver la zone pour un point (première zone trouvée)
     */
    @Query(value = "SELECT z.* FROM zones z WHERE " +
           "ST_Contains(ST_GeomFromGeoJSON(z.boundary_json), " +
           "ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) " +
           "AND z.is_active = true " +
           "ORDER BY z.delivery_fee ASC LIMIT 1", nativeQuery = true)
    Optional<Zone> findZoneForPoint(@Param("latitude") BigDecimal latitude,
                                     @Param("longitude") BigDecimal longitude);

    /**
     * Trouver toutes les zones avec pagination
     */
    Page<Zone> findByIsActiveTrue(Pageable pageable);
}
