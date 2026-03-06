package com.speedline.user.repository;

import com.speedline.user.domain.Address;
import com.speedline.user.domain.AddressType;
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
 * Repository pour Address
 * Fournit les opérations CRUD et requêtes personnalisées
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // ==================== RECHERCHE PAR CLIENT ====================

    /**
     * Trouver toutes les adresses d'un client
     * @param customerId ID du client
     * @return Liste des adresses actives du client
     */
    List<Address> findByCustomerIdAndIsActiveTrue(Long customerId);

    /**
     * Trouver toutes les adresses d'un client (avec pagination)
     * @param customerId ID du client
     * @param pageable Pagination
     * @return Page des adresses du client
     */
    Page<Address> findByCustomerIdAndIsActiveTrue(Long customerId, Pageable pageable);

    /**
     * Trouver les adresses par userId (auth-service)
     * @param userId ID de l'utilisateur
     * @return Liste des adresses actives
     */
    List<Address> findByUserIdAndIsActiveTrue(Long userId);

    // ==================== RECHERCHE PAR TYPE ====================

    /**
     * Trouver les adresses d'un client par type
     * @param customerId ID du client
     * @param type Type d'adresse (HOME, WORK, OTHER)
     * @return Liste des adresses de ce type
     */
    List<Address> findByCustomerIdAndTypeAndIsActiveTrue(Long customerId, AddressType type);

    // ==================== ADRESSE PAR DÉFAUT ====================

    /**
     * Trouver l'adresse par défaut d'un client
     * @param customerId ID du client
     * @return Optional contenant l'adresse par défaut si elle existe
     */
    Optional<Address> findByCustomerIdAndIsDefaultTrueAndIsActiveTrue(Long customerId);

    /**
     * Retirer le flag "par défaut" de toutes les adresses d'un client
     * @param customerId ID du client
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customerId = :customerId")
    int clearDefaultForCustomer(@Param("customerId") Long customerId);

    /**
     * Définir une adresse comme par défaut
     * @param addressId ID de l'adresse
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = true WHERE a.id = :addressId")
    int setAsDefault(@Param("addressId") Long addressId);

    // ==================== RECHERCHE PAR VILLE ====================

    /**
     * Trouver les adresses dans une ville
     * @param city Nom de la ville
     * @param pageable Pagination
     * @return Page des adresses dans cette ville
     */
    Page<Address> findByCityAndIsActiveTrue(String city, Pageable pageable);

    /**
     * Lister les villes distinctes
     * @return Liste des villes
     */
    @Query("SELECT DISTINCT a.city FROM Address a WHERE a.isActive = true ORDER BY a.city")
    List<String> findDistinctCities();

    /**
     * IDs des clients dont une adresse a une ville contenant le terme (pour recherche admin).
     * @param citySearch Terme de recherche (ex: "Tunis", "Paris")
     * @return Liste des customerIds distincts
     */
    @Query("SELECT DISTINCT a.customerId FROM Address a WHERE a.isActive = true AND " +
           "LOWER(COALESCE(a.city, '')) LIKE LOWER(CONCAT('%', :citySearch, '%'))")
    List<Long> findCustomerIdsByCityContaining(@Param("citySearch") String citySearch);

    // ==================== STATISTIQUES ====================

    /**
     * Compter les adresses d'un client
     * @param customerId ID du client
     * @return Nombre d'adresses actives
     */
    long countByCustomerIdAndIsActiveTrue(Long customerId);

    /**
     * Compter les adresses par type pour un client
     * @param customerId ID du client
     * @param type Type d'adresse
     * @return Nombre d'adresses de ce type
     */
    long countByCustomerIdAndTypeAndIsActiveTrue(Long customerId, AddressType type);

    // ==================== MISE À JOUR ====================

    /**
     * Marquer une adresse comme utilisée
     * @param addressId ID de l'adresse
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Address a SET a.lastUsedAt = CURRENT_TIMESTAMP, a.usageCount = a.usageCount + 1 WHERE a.id = :addressId")
    int markAsUsed(@Param("addressId") Long addressId);

    /**
     * Désactiver une adresse (soft delete)
     * @param addressId ID de l'adresse
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Address a SET a.isActive = false WHERE a.id = :addressId")
    int softDelete(@Param("addressId") Long addressId);

    /**
     * Marquer une adresse comme vérifiée
     * @param addressId ID de l'adresse
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query("UPDATE Address a SET a.isVerified = true WHERE a.id = :addressId")
    int markAsVerified(@Param("addressId") Long addressId);

    // ==================== RECHERCHE AVANCÉE ====================

    /**
     * Trouver les adresses les plus utilisées d'un client
     * @param customerId ID du client
     * @param pageable Pagination (limiter le nombre de résultats)
     * @return Liste des adresses triées par utilisation
     */
    @Query("SELECT a FROM Address a WHERE a.customerId = :customerId AND a.isActive = true ORDER BY a.usageCount DESC")
    List<Address> findMostUsedByCustomer(@Param("customerId") Long customerId, Pageable pageable);

    /**
     * Rechercher des adresses par texte (rue, ville, label)
     * @param customerId ID du client
     * @param searchTerm Terme de recherche
     * @return Liste des adresses correspondantes
     */
    @Query("SELECT a FROM Address a WHERE a.customerId = :customerId AND a.isActive = true " +
           "AND (LOWER(a.street) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.label) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Address> searchByCustomer(@Param("customerId") Long customerId, @Param("searchTerm") String searchTerm);

    /**
     * Vérifier si une adresse appartient à un client
     * @param addressId ID de l'adresse
     * @param customerId ID du client
     * @return true si l'adresse appartient au client
     */
    boolean existsByIdAndCustomerId(Long addressId, Long customerId);

    // ==================== UNICITÉ ====================

    /**
     * Vérifier si un client possède déjà une adresse active avec la même étiquette (insensible à la casse).
     * Couvre tous les types : HOME, WORK, APARTMENT, OTHER.
     *
     * @param customerId ID du client
     * @param label      Étiquette à vérifier (ex: "Maison", "Bureau", "Chez maman")
     * @return Optional contenant l'adresse existante si doublon d'étiquette
     */
    Optional<Address> findByCustomerIdAndLabelIgnoreCaseAndIsActiveTrue(Long customerId, String label);

    /**
     * Vérifier si un client possède déjà une adresse active avec la même adresse formatée (insensible à la casse).
     *
     * @param customerId       ID du client
     * @param formattedAddress Adresse formatée GPS (ex: "12 Rue Habib Bourguiba, Tunis")
     * @return Optional contenant l'adresse existante si doublon physique
     */
    Optional<Address> findByCustomerIdAndFormattedAddressIgnoreCaseAndIsActiveTrue(Long customerId, String formattedAddress);

    /**
     * Trouver les adresses actives d'un client dont les coordonnées GPS sont
     * dans un rayon d'environ 11 m (± 0,0001°) autour d'un point donné.
     *
     * @param customerId ID du client
     * @param lat        Latitude de référence
     * @param lon        Longitude de référence
     * @return Liste des adresses proches (vides si aucun doublon)
     */
    @Query("SELECT a FROM Address a " +
           "WHERE a.customerId = :customerId AND a.isActive = true " +
           "AND a.latitude IS NOT NULL AND a.longitude IS NOT NULL " +
           "AND a.latitude  BETWEEN :lat - 0.0001 AND :lat + 0.0001 " +
           "AND a.longitude BETWEEN :lon - 0.0001 AND :lon + 0.0001")
    List<Address> findByCoordinatesNear(@Param("customerId") Long customerId,
                                        @Param("lat") BigDecimal lat,
                                        @Param("lon") BigDecimal lon);
}
