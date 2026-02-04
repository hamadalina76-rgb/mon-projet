package com.speedline.location.service;

import com.speedline.location.domain.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des zones de livraison
 */
public interface ZoneService {

    /**
     * Créer une nouvelle zone
     * 
     * @param name Nom de la zone
     * @param description Description
     * @param type Type de zone (DELIVERY, RESTRICTED, PREMIUM, EXPRESS)
     * @param boundaryJson Polygone de la zone (JSON array de coordonnées)
     * @param deliveryFee Frais de livraison pour cette zone
     * @param minDeliveryTime Temps minimum de livraison (minutes)
     * @param maxDeliveryTime Temps maximum de livraison (minutes)
     * @return ZoneDTO avec id généré
     * @throws InvalidBoundaryException si le polygone est invalide
     */
    ZoneDTO createZone(String name, String description, Zone.ZoneType type,
                       String boundaryJson, BigDecimal deliveryFee,
                       Integer minDeliveryTime, Integer maxDeliveryTime);

    /**
     * Récupérer une zone par ID
     * 
     * @param zoneId ID de la zone
     * @return ZoneDTO complet
     * @throws ZoneNotFoundException si la zone n'existe pas
     */
    ZoneDTO getZoneById(Long zoneId);

    /**
     * Mettre à jour une zone
     * 
     * @param zoneId ID de la zone
     * @param name Nouveau nom (null = pas de changement)
     * @param description Nouvelle description
     * @param deliveryFee Nouveaux frais de livraison
     * @param boundaryJson Nouveau polygone (JSON)
     * @return ZoneDTO mis à jour
     * @throws ZoneNotFoundException si la zone n'existe pas
     */
    ZoneDTO updateZone(Long zoneId, String name, String description,
                       BigDecimal deliveryFee, String boundaryJson);

    /**
     * Activer/Désactiver une zone
     * 
     * @param zoneId ID de la zone
     * @param isActive true = active
     * @return ZoneDTO mis à jour
     * @throws ZoneNotFoundException si la zone n'existe pas
     */
    ZoneDTO setActiveStatus(Long zoneId, boolean isActive);

    /**
     * Supprimer une zone
     * 
     * @param zoneId ID de la zone
     * @throws ZoneNotFoundException si la zone n'existe pas
     */
    void deleteZone(Long zoneId);

    /**
     * Obtenir toutes les zones actives
     * 
     * @return List<ZoneDTO> zones actives
     */
    List<ZoneDTO> getActiveZones();

    /**
     * Obtenir les zones avec pagination
     * 
     * @param pageable Pagination
     * @return Page<ZoneDTO>
     */
    Page<ZoneDTO> getAllZones(Pageable pageable);

    /**
     * Obtenir les zones par type
     * 
     * @param type Type de zone
     * @return List<ZoneDTO>
     */
    List<ZoneDTO> getZonesByType(Zone.ZoneType type);

    /**
     * Vérifier si un point est dans une zone
     * 
     * @param latitude Latitude du point
     * @param longitude Longitude du point
     * @return ZoneDTO zone contenant le point, ou null
     */
    ZoneDTO findZoneForPoint(BigDecimal latitude, BigDecimal longitude);

    /**
     * Vérifier si un point est dans une zone spécifique
     * 
     * @param zoneId ID de la zone
     * @param latitude Latitude du point
     * @param longitude Longitude du point
     * @return boolean true si le point est dans la zone
     * @throws ZoneNotFoundException si la zone n'existe pas
     */
    boolean isPointInZone(Long zoneId, BigDecimal latitude, BigDecimal longitude);

    /**
     * Obtenir les frais de livraison pour un point
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @return BigDecimal frais de livraison, ou null si point non livrable
     */
    BigDecimal getDeliveryFeeForPoint(BigDecimal latitude, BigDecimal longitude);

    /**
     * DTO pour les zones
     */
    record ZoneDTO(
            Long id,
            String name,
            String description,
            Zone.ZoneType type,
            BigDecimal deliveryFee,
            Integer minDeliveryTime,
            Integer maxDeliveryTime,
            Boolean isActive
    ) {}
}
