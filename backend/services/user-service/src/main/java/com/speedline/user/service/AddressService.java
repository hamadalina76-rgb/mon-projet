package com.speedline.user.service;

import com.speedline.user.domain.AddressType;
import com.speedline.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service pour la gestion des adresses de livraison
 * 
 * Ce service gère toutes les opérations liées aux adresses :
 * - Création, modification, suppression d'adresses
 * - Gestion de l'adresse par défaut
 * - Recherche et validation d'adresses
 */
public interface AddressService {

    // ==================== OPÉRATIONS CRUD ====================

    /**
     * Créer une nouvelle adresse pour un client
     * 
     * @param customerId ID du profil client
     * @param request AddressCreateRequest contenant:
     *                - type (AddressType): HOME, WORK, OTHER (défaut: HOME)
     *                - label (String, optionnel): Label personnalisé
     *                - street (String, obligatoire): Numéro et nom de rue
     *                - building, floor, apartment, accessCode (String, optionnel): Détails
     *                - city (String, obligatoire): Ville
     *                - postalCode, state, country (String, optionnel): Localisation
     *                - latitude, longitude (BigDecimal, optionnel): Coordonnées GPS
     *                - placeId (String, optionnel): ID Google/Mapbox
     *                - deliveryInstructions, landmark (String, optionnel): Instructions
     *                - contactPhone, contactName (String, optionnel): Contact alternatif
     *                - isDefault (Boolean): Si true, devient l'adresse par défaut
     * @return AddressDTO avec:
     *         - id: ID de l'adresse créée
     *         - formattedAddress: Adresse complète générée
     *         - isVerified: false (en attente de vérification GPS)
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws MaxAddressesReachedException si le client a atteint le max d'adresses (10)
     */
    AddressDTO createAddress(Long customerId, AddressCreateRequest request);

    /**
     * Récupérer une adresse par son ID
     * 
     * @param addressId ID de l'adresse
     * @return AddressDTO avec toutes les informations
     * @throws AddressNotFoundException si l'adresse n'existe pas
     */
    AddressDTO getAddressById(Long addressId);

    /**
     * Mettre à jour une adresse existante
     * 
     * @param addressId ID de l'adresse
     * @param request AddressUpdateRequest contenant les champs à modifier
     *                (tous les champs sont optionnels)
     * @return AddressDTO mis à jour
     * @throws AddressNotFoundException si l'adresse n'existe pas
     */
    AddressDTO updateAddress(Long addressId, AddressUpdateRequest request);

    /**
     * Supprimer une adresse (soft delete)
     * 
     * @param addressId ID de l'adresse
     * @throws AddressNotFoundException si l'adresse n'existe pas
     * @throws CannotDeleteDefaultAddressException si c'est l'adresse par défaut
     *         et qu'il existe d'autres adresses
     */
    void deleteAddress(Long addressId);

    // ==================== ADRESSES D'UN CLIENT ====================

    /**
     * Obtenir toutes les adresses d'un client
     * 
     * @param customerId ID du profil client
     * @return List<AddressDTO> liste des adresses actives du client
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<AddressDTO> getCustomerAddresses(Long customerId);

    /**
     * Obtenir les adresses d'un client avec pagination
     * 
     * @param customerId ID du profil client
     * @param pageable Pagination
     * @return Page<AddressDTO> page des adresses du client
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    Page<AddressDTO> getCustomerAddressesPaginated(Long customerId, Pageable pageable);

    /**
     * Obtenir les adresses d'un client par type
     * 
     * @param customerId ID du profil client
     * @param type Type d'adresse (HOME, WORK, OTHER)
     * @return List<AddressDTO> liste des adresses de ce type
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<AddressDTO> getCustomerAddressesByType(Long customerId, AddressType type);

    /**
     * Compter le nombre d'adresses d'un client
     * 
     * @param customerId ID du profil client
     * @return long nombre d'adresses actives
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    long countCustomerAddresses(Long customerId);

    // ==================== ADRESSE PAR DÉFAUT ====================

    /**
     * Obtenir l'adresse par défaut d'un client
     * 
     * @param customerId ID du profil client
     * @return AddressDTO adresse par défaut
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws NoDefaultAddressException si aucune adresse par défaut n'existe
     */
    AddressDTO getDefaultAddress(Long customerId);

    /**
     * Définir une adresse comme adresse par défaut
     * Retire le flag "par défaut" des autres adresses
     * 
     * @param addressId ID de l'adresse
     * @return AddressDTO adresse mise à jour avec isDefault=true
     * @throws AddressNotFoundException si l'adresse n'existe pas
     */
    AddressDTO setAsDefault(Long addressId);

    // ==================== VALIDATION ET GÉOCODAGE ====================

    /**
     * Vérifier et géocoder une adresse (obtenir les coordonnées GPS)
     * Utilise Mapbox/Google Geocoding API
     * 
     * @param addressId ID de l'adresse
     * @return AddressDTO avec latitude, longitude, placeId remplis
     * @throws AddressNotFoundException si l'adresse n'existe pas
     * @throws GeocodingFailedException si le géocodage échoue
     */
    AddressDTO geocodeAddress(Long addressId);

    /**
     * Vérifier si une adresse est dans une zone de livraison
     * Utilise location-service
     * 
     * @param addressId ID de l'adresse
     * @return boolean true si l'adresse est livrable
     * @throws AddressNotFoundException si l'adresse n'existe pas
     * @throws AddressNotGeocodedException si l'adresse n'a pas de coordonnées GPS
     */
    boolean isInDeliveryZone(Long addressId);

    /**
     * Marquer une adresse comme vérifiée
     * Appelé après confirmation GPS réussie
     * 
     * @param addressId ID de l'adresse
     * @return AddressDTO avec isVerified=true
     * @throws AddressNotFoundException si l'adresse n'existe pas
     */
    AddressDTO markAsVerified(Long addressId);

    // ==================== UTILISATION ====================

    /**
     * Marquer une adresse comme utilisée
     * Appelé automatiquement après une commande
     * Incrémente usageCount et met à jour lastUsedAt
     * 
     * @param addressId ID de l'adresse
     * @throws AddressNotFoundException si l'adresse n'existe pas
     */
    void markAsUsed(Long addressId);

    /**
     * Obtenir les adresses les plus utilisées d'un client
     * 
     * @param customerId ID du profil client
     * @param limit Nombre max d'adresses à retourner
     * @return List<AddressDTO> adresses triées par usage décroissant
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<AddressDTO> getMostUsedAddresses(Long customerId, int limit);

    // ==================== RECHERCHE ====================

    /**
     * Rechercher des adresses d'un client par texte
     * Recherche dans: rue, ville, label
     * 
     * @param customerId ID du profil client
     * @param searchTerm Terme de recherche
     * @return List<AddressDTO> adresses correspondantes
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<AddressDTO> searchCustomerAddresses(Long customerId, String searchTerm);

    /**
     * Lister les villes disponibles (pour autocomplétion)
     * 
     * @return List<String> liste des villes distinctes
     */
    List<String> getAvailableCities();

    // ==================== VALIDATION ====================

    /**
     * Vérifier si une adresse appartient à un client
     * 
     * @param addressId ID de l'adresse
     * @param customerId ID du profil client
     * @return boolean true si l'adresse appartient au client
     */
    boolean belongsToCustomer(Long addressId, Long customerId);

    /**
     * Vérifier si un client peut ajouter une nouvelle adresse
     * Max 10 adresses par client
     * 
     * @param customerId ID du profil client
     * @return boolean true si le client peut ajouter une adresse
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    boolean canAddAddress(Long customerId);
}
