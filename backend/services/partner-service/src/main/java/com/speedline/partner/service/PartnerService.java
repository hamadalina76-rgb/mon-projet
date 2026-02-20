package com.speedline.partner.service;

import com.speedline.partner.domain.PartnerStatus;
import com.speedline.partner.domain.PartnerType;
import com.speedline.partner.dto.CompletePartnerProfileRequest;
import com.speedline.partner.dto.PartnerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des partenaires (restaurants, magasins)
 * 
 * Ce service gère toutes les opérations liées aux partenaires :
 * - Création, modification, suppression de partenaires
 * - Gestion du statut et de la disponibilité
 * - Recherche et filtrage
 * - Statistiques
 */
public interface PartnerService {

    // ==================== SYNC AUTH-SERVICE ====================

    /**
     * Créer un profil partner initial depuis auth-service
     * Appelé lors de l'inscription avec role=PARTNER
     * 
     * @param userId ID de l'utilisateur propriétaire (auth-service)
     * @param email Email du partenaire
     * @param firstName Prénom
     * @param lastName Nom
     * @param phoneNumber Téléphone
     * @return PartnerDTO avec status=PENDING, isProfileComplete=false
     */
    PartnerDTO createPartnerFromAuth(Long userId, String email, String firstName, 
                                     String lastName, String phoneNumber);

    /**
     * Compléter le profil partner (Phase 2 - après login)
     * 
     * @param partnerId ID du partner
     * @param request Données complètes du profil
     * @return PartnerDTO mis à jour
     */
    PartnerDTO completeProfile(Long partnerId, CompletePartnerProfileRequest request);

    // ==================== OPÉRATIONS CRUD ====================

    /**
     * Créer un nouveau partenaire
     * 
     * @param userId ID de l'utilisateur propriétaire (auth-service)
     * @param businessName Nom commercial
     * @param type Type de partenaire (RESTAURANT, FAST_FOOD, etc.)
     * @param description Description du partenaire
     * @param address Adresse complète
     * @param city Ville
     * @param latitude Latitude GPS
     * @param longitude Longitude GPS
     * @return PartnerDTO avec id, slug généré, status=PENDING
     * @throws UserAlreadyHasPartnerException si l'utilisateur a déjà un partenaire
     */
    PartnerDTO createPartner(Long userId, String businessName, PartnerType type, 
                             String description, String address, String city,
                             BigDecimal latitude, BigDecimal longitude);

    /**
     * Récupérer un partenaire par son ID
     * 
     * @param partnerId ID du partenaire
     * @return PartnerDTO complet
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO getPartnerById(Long partnerId);

    /**
     * Récupérer un partenaire par son slug URL
     * 
     * @param slug Slug unique (ex: "pizza-house-tunis")
     * @return PartnerDTO complet
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO getPartnerBySlug(String slug);

    /**
     * Récupérer un partenaire par l'ID de son propriétaire
     * 
     * @param userId ID de l'utilisateur propriétaire
     * @return PartnerDTO complet
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO getPartnerByUserId(Long userId);

    /**
     * Mettre à jour les informations d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @param businessName Nouveau nom (null = pas de changement)
     * @param description Nouvelle description
     * @param phoneNumber Nouveau téléphone
     * @param email Nouvel email
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updatePartner(Long partnerId, String businessName, String description,
                             String phoneNumber, String email);

    /**
     * Mettre à jour l'adresse et la localisation
     * 
     * @param partnerId ID du partenaire
     * @param address Nouvelle adresse
     * @param city Nouvelle ville
     * @param postalCode Nouveau code postal
     * @param latitude Nouvelle latitude
     * @param longitude Nouvelle longitude
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updateLocation(Long partnerId, String address, String city, 
                              String postalCode, BigDecimal latitude, BigDecimal longitude);

    /**
     * Mettre à jour les images du partenaire
     * 
     * @param partnerId ID du partenaire
     * @param logo URL du nouveau logo
     * @param coverImage URL de la nouvelle image de couverture
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updateImages(Long partnerId, String logo, String coverImage);

    /**
     * Supprimer (fermer) un partenaire
     * 
     * @param partnerId ID du partenaire
     * @throws PartnerNotFoundException si non trouvé
     * @throws PartnerHasActiveOrdersException si des commandes sont en cours
     */
    void deletePartner(Long partnerId);

    // ==================== GESTION DU STATUT ====================

    /**
     * Activer/Désactiver la réception de commandes
     * 
     * @param partnerId ID du partenaire
     * @param acceptsOrders true = accepte les commandes
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     * @throws PartnerNotActiveException si le partenaire n'est pas actif
     */
    PartnerDTO setAcceptsOrders(Long partnerId, boolean acceptsOrders);

    /**
     * Mettre à jour le statut du partenaire (Admin only)
     * 
     * @param partnerId ID du partenaire
     * @param status Nouveau statut
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updateStatus(Long partnerId, PartnerStatus status);

    /**
     * Approuver un partenaire (Admin only)
     * Change le statut de PENDING à ACTIVE
     * 
     * @param partnerId ID du partenaire
     * @return PartnerDTO avec status=ACTIVE, isActive=true
     * @throws PartnerNotFoundException si non trouvé
     * @throws InvalidStatusTransitionException si le statut actuel n'est pas PENDING
     */
    PartnerDTO approvePartner(Long partnerId);

    /**
     * Rejeter un partenaire (Admin only)
     * 
     * @param partnerId ID du partenaire
     * @param reason Raison du rejet
     * @throws PartnerNotFoundException si non trouvé
     */
    void rejectPartner(Long partnerId, String reason);

    /**
     * Suspendre un partenaire (Admin only)
     * 
     * @param partnerId ID du partenaire
     * @param reason Raison de la suspension
     * @throws PartnerNotFoundException si non trouvé
     */
    void suspendPartner(Long partnerId, String reason);

    /**
     * Activer un partenaire (Admin only)
     * Change le statut à ACTIVE et permet la réception de commandes
     * 
     * @param partnerId ID du partenaire
     * @return PartnerDTO avec status=ACTIVE, isActive=true
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO activatePartner(Long partnerId);

    /**
     * Désactiver un partenaire (Admin only)
     * Change le statut à INACTIVE et arrête la réception de commandes
     * 
     * @param partnerId ID du partenaire
     * @param reason Raison de la désactivation
     * @return PartnerDTO avec status=INACTIVE, isActive=false
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO deactivatePartner(Long partnerId, String reason);

    /**
     * Demander des informations complémentaires à un partenaire (Admin only)
     * Change le statut à DOCUMENTS_MISSING et notifie le partenaire
     * 
     * @param partnerId ID du partenaire
     * @param message Message détaillant les informations/documents manquants
     * @return PartnerDTO avec status=DOCUMENTS_MISSING
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO requestMoreInfo(Long partnerId, String message);

    /**
     * Mettre à jour les notes internes d'un partenaire (Admin only)
     *
     * @param partnerId ID du partenaire
     * @param notes Contenu des notes internes
     * @return PartnerDTO mis à jour
     */
    PartnerDTO updateInternalNotes(Long partnerId, String notes);

    // ==================== PARAMÈTRES DE LIVRAISON ====================

    /**
     * Mettre à jour les paramètres de livraison
     * 
     * @param partnerId ID du partenaire
     * @param preparationTime Temps de préparation moyen (minutes)
     * @param deliveryFee Frais de livraison
     * @param minimumOrder Commande minimum
     * @param deliveryRadius Rayon de livraison (mètres)
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updateDeliverySettings(Long partnerId, Integer preparationTime,
                                       BigDecimal deliveryFee, BigDecimal minimumOrder,
                                       Integer deliveryRadius);

    /**
     * Définir le seuil de livraison gratuite
     * 
     * @param partnerId ID du partenaire
     * @param freeDeliveryThreshold Montant minimum pour livraison gratuite (null = pas de livraison gratuite)
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO setFreeDeliveryThreshold(Long partnerId, BigDecimal freeDeliveryThreshold);

    // ==================== HORAIRES ====================

    /**
     * Mettre à jour les horaires d'ouverture
     * 
     * @param partnerId ID du partenaire
     * @param openingHoursJson JSON des horaires
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     * @throws InvalidOpeningHoursException si le format JSON est invalide
     */
    PartnerDTO updateOpeningHours(Long partnerId, String openingHoursJson);

    /**
     * Vérifier si un partenaire est actuellement ouvert
     * 
     * @param partnerId ID du partenaire
     * @return boolean true si ouvert
     * @throws PartnerNotFoundException si non trouvé
     */
    boolean isCurrentlyOpen(Long partnerId);

    // ==================== CATÉGORIES ET TAGS ====================

    /**
     * Mettre à jour les catégories du partenaire
     * 
     * @param partnerId ID du partenaire
     * @param categoryIds Liste des IDs de catégories
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updateCategories(Long partnerId, List<Long> categoryIds);

    /**
     * Mettre à jour les tags du partenaire
     * 
     * @param partnerId ID du partenaire
     * @param tags Liste des tags
     * @return PartnerDTO mis à jour
     * @throws PartnerNotFoundException si non trouvé
     */
    PartnerDTO updateTags(Long partnerId, List<String> tags);

    // ==================== STATISTIQUES ====================

    /**
     * Incrémenter le compteur de commandes
     * Appelé automatiquement après une commande
     * 
     * @param partnerId ID du partenaire
     * @param orderAmount Montant de la commande
     * @throws PartnerNotFoundException si non trouvé
     */
    void incrementOrderCount(Long partnerId, BigDecimal orderAmount);

    /**
     * Mettre à jour la note moyenne
     * Appelé automatiquement après un avis
     * 
     * @param partnerId ID du partenaire
     * @param rating Nouvelle note (1-5)
     * @throws PartnerNotFoundException si non trouvé
     */
    void updateRating(Long partnerId, BigDecimal rating);

    // ==================== RECHERCHE ET LISTE ====================

    /**
     * Obtenir tous les partenaires actifs avec pagination
     * 
     * @param pageable Pagination
     * @return Page<PartnerDTO>
     */
    Page<PartnerDTO> getAllActivePartners(Pageable pageable);

    /**
     * Obtenir les partenaires par statut
     * 
     * @param status Statut recherché
     * @param pageable Pagination
     * @return Page<PartnerDTO>
     */
    Page<PartnerDTO> getPartnersByStatus(PartnerStatus status, Pageable pageable);

    /**
     * Obtenir les partenaires par statut et recherche (nom, marque, ville) – liste admin paginée.
     *
     * @param status Statut optionnel (null = tous)
     * @param search Texte de recherche optionnel (businessName, brandName, city)
     * @param pageable Pagination
     * @return Page<PartnerDTO>
     */
    Page<PartnerDTO> getPartnersByStatusAndSearch(PartnerStatus status, String search, Pageable pageable);

    /**
     * Obtenir les partenaires par type
     * 
     * @param type Type de partenaire
     * @param pageable Pagination
     * @return Page<PartnerDTO>
     */
    Page<PartnerDTO> getPartnersByType(PartnerType type, Pageable pageable);

    /**
     * Obtenir les partenaires par ville
     * 
     * @param city Nom de la ville
     * @param pageable Pagination
     * @return Page<PartnerDTO>
     */
    Page<PartnerDTO> getPartnersByCity(String city, Pageable pageable);

    /**
     * Obtenir les partenaires proches d'une position
     * 
     * @param latitude Latitude du client
     * @param longitude Longitude du client
     * @param radiusKm Rayon de recherche en km
     * @return List<PartnerDTO> triés par distance
     */
    List<PartnerDTO> getNearbyPartners(BigDecimal latitude, BigDecimal longitude, double radiusKm);

    /**
     * Obtenir les partenaires en vedette
     * 
     * @return List<PartnerDTO>
     */
    List<PartnerDTO> getFeaturedPartners();

    /**
     * Obtenir les meilleurs partenaires (par note)
     * 
     * @param pageable Pagination
     * @return Page<PartnerDTO>
     */
    Page<PartnerDTO> getTopRatedPartners(Pageable pageable);

    /**
     * Obtenir la liste des villes disponibles
     * 
     * @return List<String> noms des villes
     */
    List<String> getAvailableCities();
}
