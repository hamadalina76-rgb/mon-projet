package com.speedline.user.service;

import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.VehicleType;
import com.speedline.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des profils livreurs
 * 
 * Ce service gère toutes les opérations liées aux livreurs :
 * - Création et mise à jour des profils
 * - Gestion de la disponibilité et localisation
 * - Validation des documents
 * - Statistiques et performances
 * - Gestion des gains
 */
public interface CourierService {

    // ==================== OPÉRATIONS CRUD ====================

    /**
     * Créer un nouveau profil livreur
     * Le statut initial est PENDING_APPROVAL (en attente de validation)
     * 
     * @param request CourierCreateRequest contenant:
     *                - userId (Long, obligatoire): ID de l'utilisateur dans auth-service
     *                - vehicleType (VehicleType, obligatoire): Type de véhicule
     *                - vehicleNumber (String, optionnel): Numéro d'immatriculation
     *                - vehicleModel (String, optionnel): Marque et modèle
     *                - vehicleColor (String, optionnel): Couleur du véhicule
     *                - drivingLicenseNumber (String, optionnel): Numéro de permis
     *                - preferredDeliveryZone (String, optionnel): Zone préférée
     *                - maxDeliveryRadius (Integer, optionnel): Rayon max en mètres
     * @return CourierDTO avec:
     *         - id: ID du profil livreur créé
     *         - status: PENDING_APPROVAL
     *         - documentsVerified: false
     *         - isAvailable: false
     *         - isOnline: false
     * @throws UserAlreadyExistsException si un profil existe déjà pour cet userId
     */
    CourierDTO createCourier(CourierCreateRequest request);

    /**
     * Récupérer un livreur par son ID
     * 
     * @param courierId ID du profil livreur
     * @return CourierDTO avec toutes les informations du profil
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO getCourierById(Long courierId);

    /**
     * Récupérer un livreur par son userId (auth-service)
     * 
     * @param userId ID de l'utilisateur dans auth-service
     * @return CourierDTO avec toutes les informations du profil
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO getCourierByUserId(Long userId);

    /**
     * Mettre à jour le profil d'un livreur
     * 
     * @param courierId ID du profil livreur
     * @param request CourierUpdateRequest contenant les champs à modifier:
     *                - vehicleType, vehicleNumber, vehicleModel, vehicleColor
     *                - preferredDeliveryZone, maxDeliveryRadius
     *                - bankIban, bankAccountHolder
     * @return CourierDTO mis à jour
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO updateCourier(Long courierId, CourierUpdateRequest request);

    /**
     * Mettre à jour le profil d'un livreur par userId (auth-service)
     * Utilisé par l'endpoint /couriers/current_user
     * 
     * @param userId ID de l'utilisateur dans auth-service
     * @param request CourierUpdateRequest contenant les champs à modifier
     * @return CourierDTO mis à jour
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO updateCourierByUserId(Long userId, CourierUpdateRequest request);

    /**
     * Mettre à jour la documentation complète d'un livreur par userId (avec upload de fichiers)
     * 
     * @param userId ID de l'utilisateur dans auth-service
     * @param vehicleType Type de véhicule
     * @param vehicleModel Modèle du véhicule
     * @param vehicleColor Couleur du véhicule
     * @param plateNumber Numéro de plaque
     * @param idNumber Numéro de carte d'identité
     * @param licenseNumber Numéro de permis
     * @param licenseExpiryDate Date d'expiration du permis
     * @param accountHolder Titulaire du compte bancaire
     * @param accountNumber Numéro de compte/IBAN
     * @param idCardFront Photo recto carte d'identité
     * @param idCardBack Photo verso carte d'identité
     * @param licenseFront Photo recto permis
     * @param licenseBack Photo verso permis
     * @return CourierDTO mis à jour
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO updateCourierDocumentationByUserId(
            Long userId, String vehicleType, String vehicleModel, String vehicleColor, String plateNumber,
            String idNumber, String licenseNumber, String licenseExpiryDate,
            String accountHolder, String accountNumber,
            org.springframework.web.multipart.MultipartFile idCardFront,
            org.springframework.web.multipart.MultipartFile idCardBack,
            org.springframework.web.multipart.MultipartFile licenseFront,
            org.springframework.web.multipart.MultipartFile licenseBack);

    /**
     * Supprimer (désactiver) un profil livreur
     * 
     * @param courierId ID du profil livreur
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws CourierBusyException si le livreur a une livraison en cours
     */
    void deleteCourier(Long courierId);

    // ==================== GESTION DE LA DISPONIBILITÉ ====================

    /**
     * Mettre à jour la disponibilité d'un livreur
     * 
     * @param courierId ID du profil livreur
     * @param request CourierAvailabilityRequest contenant:
     *                - isAvailable (Boolean, obligatoire): Disponible pour livraisons
     *                - isOnline (Boolean, obligatoire): Application active
     * @return CourierDTO mis à jour
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws CourierNotApprovedException si le compte n'est pas encore validé
     */
    CourierDTO updateAvailability(Long courierId, CourierAvailabilityRequest request);

    /**
     * Passer un livreur en ligne
     * 
     * @param courierId ID du profil livreur
     * @return CourierDTO mis à jour avec isOnline=true, isAvailable=true
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws CourierNotApprovedException si le compte n'est pas encore validé
     */
    CourierDTO goOnline(Long courierId);

    /**
     * Passer un livreur hors ligne
     * 
     * @param courierId ID du profil livreur
     * @return CourierDTO mis à jour avec isOnline=false, isAvailable=false
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO goOffline(Long courierId);

    /**
     * Marquer un livreur comme occupé (livraison en cours)
     * Appelé automatiquement par delivery-service
     * 
     * @param courierId ID du profil livreur
     * @param deliveryId ID de la livraison assignée
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void markAsBusy(Long courierId, Long deliveryId);

    /**
     * Marquer un livreur comme disponible (livraison terminée)
     * Appelé automatiquement par delivery-service
     * 
     * @param courierId ID du profil livreur
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void markAsAvailable(Long courierId);

    // ==================== GESTION DE LA LOCALISATION ====================

    /**
     * Mettre à jour la position GPS d'un livreur
     * Appelé régulièrement par l'application mobile
     * 
     * @param courierId ID du profil livreur
     * @param request CourierLocationUpdateRequest contenant:
     *                - latitude (BigDecimal, obligatoire): Latitude GPS
     *                - longitude (BigDecimal, obligatoire): Longitude GPS
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void updateLocation(Long courierId, CourierLocationUpdateRequest request);

    /**
     * Obtenir la position actuelle d'un livreur
     * 
     * @param courierId ID du profil livreur
     * @return CourierDTO avec currentLatitude, currentLongitude, lastLocationUpdate
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO getCurrentLocation(Long courierId);

    /**
     * Trouver les livreurs disponibles proches d'une position
     * Utilisé par delivery-service pour l'assignation
     * 
     * @param latitude Latitude du point de pickup
     * @param longitude Longitude du point de pickup
     * @param radiusKm Rayon de recherche en kilomètres
     * @return List<CourierDTO> livreurs disponibles triés par distance
     */
    List<CourierDTO> findAvailableCouriersNearby(BigDecimal latitude, BigDecimal longitude, double radiusKm);

    /**
     * Trouver les livreurs disponibles par type de véhicule
     * 
     * @param vehicleType Type de véhicule requis
     * @param latitude Latitude du point de pickup
     * @param longitude Longitude du point de pickup
     * @param radiusKm Rayon de recherche en kilomètres
     * @return List<CourierDTO> livreurs disponibles avec ce véhicule
     */
    List<CourierDTO> findAvailableCouriersByVehicle(VehicleType vehicleType, BigDecimal latitude, BigDecimal longitude, double radiusKm);

    // ==================== VALIDATION DES DOCUMENTS ====================

    /**
     * Soumettre les documents pour validation
     * 
     * @param courierId ID du profil livreur
     * @param drivingLicenseImage URL de l'image du permis de conduire
     * @param identityDocumentFrontImage URL de l'image recto de la CIN
     * @param identityDocumentBackImage URL de l'image verso de la CIN
     * @param profilePhoto URL de la photo de profil
     * @return CourierDTO mis à jour
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO submitDocuments(Long courierId, String drivingLicenseImage, String identityDocumentFrontImage, String identityDocumentBackImage, String profilePhoto);

    /**
     * Valider les documents d'un livreur (Admin only)
     * Passe le statut à ACTIVE.
     *
     * @param courierId ID du profil livreur
     * @return CourierDTO mis à jour avec status=ACTIVE, documentsVerified=true
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierDTO verifyDocuments(Long courierId, com.speedline.user.domain.CourierType courierType, java.util.List<Long> zoneIds);

    /**
     * Mettre à jour les zones assignées d'un livreur.
     */
    CourierDTO updateAssignedZones(Long courierId, java.util.List<Long> zoneIds);

    /**
     * Rejeter les documents d'un livreur (Admin only)
     * 
     * @param courierId ID du profil livreur
     * @param reason Raison du rejet
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void rejectDocuments(Long courierId, String reason);

    /**
     * Obtenir les livreurs en attente de validation
     * 
     * @param pageable Pagination
     * @return Page<CourierDTO> livreurs avec status=PENDING_APPROVAL
     */
    Page<CourierDTO> getCouriersAwaitingApproval(Pageable pageable);

    /**
     * Demander des informations complémentaires au livreur (Admin only).
     * Enregistre le message et notifie le livreur.
     * 
     * @param courierId ID du profil livreur
     * @param message Message à transmettre au livreur
     */
    void requestMoreInfo(Long courierId, String message);

    // ==================== STATISTIQUES ET PERFORMANCES ====================

    /**
     * Obtenir les statistiques d'un livreur
     * 
     * @param courierId ID du profil livreur
     * @return CourierStatisticsDTO contenant:
     *         - totalDeliveries, successfulDeliveries, cancelledDeliveries
     *         - successRate (%)
     *         - averageDeliveryTime (minutes)
     *         - rating, totalRatings
     *         - totalEarnings, weeklyEarnings, monthlyEarnings
     *         - totalDistanceTravelled
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    CourierStatisticsDTO getCourierStatistics(Long courierId);

    /**
     * Mettre à jour la note d'un livreur
     * Appelé automatiquement après une évaluation client
     * 
     * @param courierId ID du profil livreur
     * @param rating Note donnée (1-5)
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws InvalidRatingException si la note n'est pas entre 1 et 5
     */
    void updateRating(Long courierId, BigDecimal rating);

    /**
     * Enregistrer une livraison complétée
     * Met à jour toutes les statistiques
     * 
     * @param courierId ID du profil livreur
     * @param earnings Gains de la livraison
     * @param distanceKm Distance parcourue en km
     * @param deliveryTimeMinutes Temps de livraison en minutes
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void recordCompletedDelivery(Long courierId, BigDecimal earnings, BigDecimal distanceKm, int deliveryTimeMinutes);

    /**
     * Enregistrer une livraison annulée
     * 
     * @param courierId ID du profil livreur
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void recordCancelledDelivery(Long courierId);

    // ==================== GESTION DES GAINS ====================

    /**
     * Obtenir le solde disponible pour retrait
     * 
     * @param courierId ID du profil livreur
     * @return BigDecimal solde disponible
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    BigDecimal getAvailableBalance(Long courierId);

    /**
     * Demander un retrait des gains
     * 
     * @param courierId ID du profil livreur
     * @param amount Montant à retirer
     * @return BigDecimal nouveau solde après retrait
     * @throws CourierNotFoundException si le livreur n'existe pas
     * @throws InsufficientBalanceException si le solde est insuffisant
     * @throws BankDetailsNotSetException si les coordonnées bancaires ne sont pas renseignées
     */
    BigDecimal requestWithdrawal(Long courierId, BigDecimal amount);

    // ==================== RECHERCHE ET LISTE ====================

    /**
     * Obtenir tous les livreurs avec pagination
     * 
     * @param pageable Pagination (page, size, sort)
     * @return Page<CourierDTO> page de livreurs
     */
    Page<CourierDTO> getAllCouriers(Pageable pageable);

    /**
     * Obtenir les livreurs par statut
     * 
     * @param status Statut recherché
     * @param pageable Pagination
     * @return Page<CourierDTO> page de livreurs
     */
    Page<CourierDTO> getCouriersByStatus(CourierStatus status, Pageable pageable);

    /**
     * Recherche admin : liste paginée avec filtre statut et recherche (immat, CIN, permis).
     */
    Page<CourierDTO> searchCouriers(String search, CourierStatus status, com.speedline.user.domain.CourierType courierType, Pageable pageable);

    /**
     * Obtenir les livreurs actuellement en ligne
     * 
     * @param pageable Pagination
     * @return Page<CourierDTO> page de livreurs en ligne
     */
    Page<CourierDTO> getOnlineCouriers(Pageable pageable);

    /**
     * Obtenir les meilleurs livreurs (par note)
     * 
     * @param minRating Note minimale
     * @param pageable Pagination
     * @return Page<CourierDTO> page des meilleurs livreurs
     */
    Page<CourierDTO> getTopRatedCouriers(BigDecimal minRating, Pageable pageable);

    /**
     * Désactiver (bloquer) un livreur définitivement (Admin only). Statut → DEACTIVATED.
     *
     * @param courierId ID du profil livreur
     * @param reason Raison de la désactivation
     */
    void deactivateCourier(Long courierId, String reason);

    /**
     * Suspendre un livreur (Admin only)
     * 
     * @param courierId ID du profil livreur
     * @param reason Raison de la suspension
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void suspendCourier(Long courierId, String reason);

    /**
     * Réactiver un livreur suspendu (Admin only)
     * 
     * @param courierId ID du profil livreur
     * @throws CourierNotFoundException si le livreur n'existe pas
     */
    void reactivateCourier(Long courierId);

    /**
     * Vérifier si un livreur existe
     * 
     * @param userId ID de l'utilisateur
     * @return boolean true si existe
     */
    boolean existsByUserId(Long userId);
}
