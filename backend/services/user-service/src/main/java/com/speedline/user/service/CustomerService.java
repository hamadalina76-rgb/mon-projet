package com.speedline.user.service;

import com.speedline.user.domain.Customer;
import com.speedline.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des profils clients
 * 
 * Ce service gère toutes les opérations liées aux clients :
 * - Création et mise à jour des profils
 * - Gestion du wallet et des points de fidélité
 * - Gestion des favoris
 * - Statistiques et recherche
 */
public interface CustomerService {

    // ==================== OPÉRATIONS CRUD ====================

    /**
     * Créer un nouveau profil client
     * Appelé automatiquement après inscription dans auth-service
     * 
     * @param request CustomerCreateRequest contenant:
     *                - userId (Long, obligatoire): ID de l'utilisateur dans auth-service
     *                - referralCode (String, optionnel): Code de parrainage utilisé
     *                - preferences (CustomerPreferences, optionnel): Préférences initiales
     * @return CustomerDTO avec:
     *         - id: ID du profil client créé
     *         - userId: ID de l'utilisateur
     *         - referralCode: Code de parrainage généré pour ce client
     *         - walletBalance: 0
     *         - loyaltyPoints: 0 (ou bonus si parrainage)
     * @throws UserAlreadyExistsException si un profil existe déjà pour cet userId
     * @throws InvalidReferralCodeException si le code de parrainage est invalide
     */
    CustomerDTO createCustomer(CustomerCreateRequest request);

    /**
     * Récupérer un client par son ID
     * 
     * @param customerId ID du profil client
     * @return CustomerDTO avec toutes les informations du profil
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    CustomerDTO getCustomerById(Long customerId);

    /**
     * Récupérer un client par son userId (auth-service)
     * 
     * @param userId ID de l'utilisateur dans auth-service
     * @return CustomerDTO avec toutes les informations du profil
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    CustomerDTO getCustomerByUserId(Long userId);

    /**
     * Mettre à jour le profil d'un client
     * 
     * @param customerId ID du profil client
     * @param request CustomerUpdateRequest contenant les champs à modifier:
     *                - preferences (CustomerPreferences): Nouvelles préférences
     *                - favoritePartnerIds (String): IDs des partenaires favoris
     *                - favoriteProductIds (String): IDs des produits favoris
     * @return CustomerDTO mis à jour
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    CustomerDTO updateCustomer(Long customerId, CustomerUpdateRequest request);

    /**
     * Supprimer (soft delete) un profil client
     * 
     * @param customerId ID du profil client
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void deleteCustomer(Long customerId);

    // ==================== GESTION DU WALLET ====================

    /**
     * Obtenir le solde du wallet d'un client
     * 
     * @param customerId ID du profil client
     * @return BigDecimal représentant le solde actuel
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    BigDecimal getWalletBalance(Long customerId);

    /**
     * Ajouter des fonds au wallet d'un client
     * 
     * @param customerId ID du profil client
     * @param amount Montant à ajouter (doit être > 0)
     * @return BigDecimal nouveau solde après ajout
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws InvalidAmountException si le montant est <= 0
     */
    BigDecimal addToWallet(Long customerId, BigDecimal amount);

    /**
     * Déduire des fonds du wallet d'un client (pour paiement)
     * 
     * @param customerId ID du profil client
     * @param amount Montant à déduire (doit être > 0)
     * @return BigDecimal nouveau solde après déduction
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws InsufficientBalanceException si le solde est insuffisant
     * @throws InvalidAmountException si le montant est <= 0
     */
    BigDecimal deductFromWallet(Long customerId, BigDecimal amount);

    // ==================== GESTION DES POINTS DE FIDÉLITÉ ====================

    /**
     * Obtenir les points de fidélité d'un client
     * 
     * @param customerId ID du profil client
     * @return Integer nombre de points actuels
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    Integer getLoyaltyPoints(Long customerId);

    /**
     * Ajouter des points de fidélité
     * Appelé automatiquement après une commande complétée
     * Règle: 1 TND dépensé = 1 point
     * 
     * @param customerId ID du profil client
     * @param points Nombre de points à ajouter (doit être > 0)
     * @return Integer nouveau total de points
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    Integer addLoyaltyPoints(Long customerId, int points);

    /**
     * Utiliser des points de fidélité (contre réduction)
     * Règle: 100 points = 1 TND de réduction
     * 
     * @param customerId ID du profil client
     * @param points Nombre de points à utiliser (doit être > 0)
     * @return Integer nombre de points restants
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws InsufficientPointsException si pas assez de points
     */
    Integer useLoyaltyPoints(Long customerId, int points);

    // ==================== GESTION DES FAVORIS ====================

    /**
     * Ajouter un partenaire aux favoris
     * 
     * @param customerId ID du profil client
     * @param partnerId ID du partenaire à ajouter
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void addFavoritePartner(Long customerId, Long partnerId);

    /**
     * Retirer un partenaire des favoris
     * 
     * @param customerId ID du profil client
     * @param partnerId ID du partenaire à retirer
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void removeFavoritePartner(Long customerId, Long partnerId);

    /**
     * Obtenir la liste des IDs des partenaires favoris
     * 
     * @param customerId ID du profil client
     * @return List<Long> liste des IDs de partenaires favoris
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<Long> getFavoritePartnerIds(Long customerId);

    /**
     * Ajouter un produit aux favoris
     * 
     * @param customerId ID du profil client
     * @param productId ID du produit à ajouter
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void addFavoriteProduct(Long customerId, Long productId);

    /**
     * Retirer un produit des favoris
     * 
     * @param customerId ID du profil client
     * @param productId ID du produit à retirer
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void removeFavoriteProduct(Long customerId, Long productId);

    /**
     * Obtenir la liste des IDs des produits favoris
     * 
     * @param customerId ID du profil client
     * @return List<Long> liste des IDs de produits favoris
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<Long> getFavoriteProductIds(Long customerId);

    // ==================== PARRAINAGE ====================

    /**
     * Obtenir le code de parrainage d'un client
     * 
     * @param customerId ID du profil client
     * @return String code de parrainage unique
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    String getReferralCode(Long customerId);

    /**
     * Appliquer un code de parrainage
     * Bonus: Parrain +500 points, Filleul +200 points
     * 
     * @param customerId ID du nouveau client (filleul)
     * @param referralCode Code de parrainage du parrain
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws InvalidReferralCodeException si le code est invalide
     * @throws SelfReferralException si le client essaie de se parrainer lui-même
     */
    void applyReferralCode(Long customerId, String referralCode);

    /**
     * Obtenir les filleuls d'un client
     * 
     * @param customerId ID du profil client (parrain)
     * @return List<CustomerDTO> liste des clients parrainés
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<CustomerDTO> getReferrals(Long customerId);

    // ==================== STATISTIQUES ET RECHERCHE ====================

    /**
     * Incrémenter le compteur de commandes d'un client
     * Appelé automatiquement après une commande complétée
     * 
     * @param customerId ID du profil client
     * @param orderAmount Montant de la commande
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void incrementOrderCount(Long customerId, BigDecimal orderAmount);

    /**
     * Obtenir tous les clients avec pagination
     * 
     * @param pageable Pagination (page, size, sort)
     * @return Page<CustomerDTO> page de clients
     */
    Page<CustomerDTO> getAllCustomers(Pageable pageable);

    /**
     * Obtenir les clients VIP
     * 
     * @param pageable Pagination
     * @return Page<CustomerDTO> page de clients VIP
     */
    Page<CustomerDTO> getVipCustomers(Pageable pageable);

    /**
     * Obtenir les meilleurs clients (par dépenses)
     * 
     * @param pageable Pagination
     * @return Page<CustomerDTO> page des meilleurs clients
     */
    Page<CustomerDTO> getTopSpenders(Pageable pageable);

    /**
     * Obtenir les clients inactifs (pas de commande depuis X jours)
     * 
     * @param days Nombre de jours d'inactivité
     * @param pageable Pagination
     * @return Page<CustomerDTO> page de clients inactifs
     */
    Page<CustomerDTO> getInactiveCustomers(int days, Pageable pageable);

    /**
     * Vérifier si un client existe
     * 
     * @param userId ID de l'utilisateur
     * @return boolean true si existe
     */
    boolean existsByUserId(Long userId);
}
