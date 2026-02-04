package com.speedline.user.repository;

import com.speedline.user.domain.Customer;
import com.speedline.user.domain.Customer.CustomerStatus;
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
 * Repository pour Customer
 * Fournit les opérations CRUD et requêtes personnalisées
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ==================== RECHERCHE PAR IDENTIFIANTS ====================

    /**
     * Trouver un client par son userId (auth-service)
     * @param userId ID de l'utilisateur dans auth-service
     * @return Optional contenant le client si trouvé
     */
    Optional<Customer> findByUserId(Long userId);

    /**
     * Vérifier si un client existe pour cet userId
     * @param userId ID de l'utilisateur
     * @return true si existe
     */
    boolean existsByUserId(Long userId);

    /**
     * Trouver un client par son code de parrainage
     * @param referralCode Code de parrainage unique
     * @return Optional contenant le client si trouvé
     */
    Optional<Customer> findByReferralCode(String referralCode);

    // ==================== RECHERCHE PAR STATUT ====================

    /**
     * Trouver tous les clients par statut
     * @param status Statut du client
     * @param pageable Pagination
     * @return Page de clients
     */
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    /**
     * Trouver tous les clients VIP
     * @param pageable Pagination
     * @return Page de clients VIP
     */
    Page<Customer> findByIsVipTrue(Pageable pageable);

    // ==================== RECHERCHE PAR ACTIVITÉ ====================

    /**
     * Trouver les clients actifs ayant commandé récemment
     * @param since Date depuis laquelle chercher
     * @param pageable Pagination
     * @return Page de clients actifs
     */
    @Query("SELECT c FROM Customer c WHERE c.lastOrderDate >= :since AND c.status = 'ACTIVE' ORDER BY c.lastOrderDate DESC")
    Page<Customer> findActiveCustomersSince(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Trouver les clients inactifs (pas de commande depuis X jours)
     * @param since Date limite
     * @param pageable Pagination
     * @return Page de clients inactifs
     */
    @Query("SELECT c FROM Customer c WHERE (c.lastOrderDate IS NULL OR c.lastOrderDate < :since) AND c.status = 'ACTIVE'")
    Page<Customer> findInactiveCustomers(@Param("since") LocalDateTime since, Pageable pageable);

    // ==================== RECHERCHE PAR PARRAINAGE ====================

    /**
     * Trouver les clients parrainés par un autre client
     * @param referrerId ID du client parrain
     * @return Liste des clients parrainés
     */
    List<Customer> findByReferredByCustomerId(Long referrerId);

    /**
     * Compter le nombre de filleuls d'un client
     * @param referrerId ID du client parrain
     * @return Nombre de filleuls
     */
    long countByReferredByCustomerId(Long referrerId);

    // ==================== STATISTIQUES ====================

    /**
     * Compter les clients par statut
     * @param status Statut à compter
     * @return Nombre de clients
     */
    long countByStatus(CustomerStatus status);

    /**
     * Compter les clients VIP
     * @return Nombre de clients VIP
     */
    long countByIsVipTrue();

    /**
     * Trouver les meilleurs clients (par montant dépensé)
     * @param pageable Pagination
     * @return Page des meilleurs clients
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' ORDER BY c.totalSpent DESC")
    Page<Customer> findTopSpenders(Pageable pageable);

    /**
     * Trouver les clients avec le plus de commandes
     * @param pageable Pagination
     * @return Page de clients
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' ORDER BY c.totalOrders DESC")
    Page<Customer> findMostFrequentCustomers(Pageable pageable);

    // ==================== MISE À JOUR ====================

    /**
     * Mettre à jour le solde du wallet
     * @param customerId ID du client
     * @param amount Montant à ajouter (négatif pour déduire)
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Customer c SET c.walletBalance = c.walletBalance + :amount WHERE c.id = :customerId")
    int updateWalletBalance(@Param("customerId") Long customerId, @Param("amount") BigDecimal amount);

    /**
     * Ajouter des points de fidélité
     * @param customerId ID du client
     * @param points Points à ajouter
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Customer c SET c.loyaltyPoints = c.loyaltyPoints + :points WHERE c.id = :customerId")
    int addLoyaltyPoints(@Param("customerId") Long customerId, @Param("points") int points);

    /**
     * Incrémenter le compteur de commandes
     * @param customerId ID du client
     * @param orderAmount Montant de la commande
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Customer c SET c.totalOrders = c.totalOrders + 1, c.totalSpent = c.totalSpent + :orderAmount, c.lastOrderDate = CURRENT_TIMESTAMP WHERE c.id = :customerId")
    int incrementOrderCount(@Param("customerId") Long customerId, @Param("orderAmount") BigDecimal orderAmount);

    /**
     * Mettre à jour le statut
     * @param customerId ID du client
     * @param status Nouveau statut
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Customer c SET c.status = :status WHERE c.id = :customerId")
    int updateStatus(@Param("customerId") Long customerId, @Param("status") CustomerStatus status);

    // ==================== RECHERCHE AVANCÉE ====================

    /**
     * Rechercher des clients par IDs
     * @param userIds Liste des userIds à rechercher
     * @return Liste des clients correspondants
     */
    @Query("SELECT c FROM Customer c WHERE c.userId IN :userIds")
    List<Customer> findByUserIdIn(@Param("userIds") List<Long> userIds);
}
