package com.speedline.user.service;

import com.speedline.user.dto.*;

import java.util.List;

/**
 * Service pour la gestion des profils clients
 * 
 * Ce service gère les opérations liées aux clients :
 * - Consultation, mise à jour et suppression de profils
 * - Gestion des adresses
 * - Gestion des favoris (partenaires)
 */
public interface CustomerService {

    // ==================== OPÉRATIONS CLIENT ====================

    /**
     * Créer un nouveau profil client
     * Appelé après l'inscription dans auth-service
     * 
     * @param request CustomerCreateRequest contenant userId et des préférences optionnelles
     * @return CustomerDTO avec le profil créé
     * @throws UserAlreadyExistsException si un profil existe déjà pour ce userId
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
     * Mettre à jour le profil d'un client
     * 
     * @param customerId ID du profil client
     * @param request CustomerUpdateRequest contenant les champs à modifier
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

    // ==================== GESTION DES ADRESSES ====================

    /**
     * Obtenir toutes les adresses d'un client
     * 
     * @param customerId ID du profil client
     * @return List<AddressDTO> liste des adresses du client
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<AddressDTO> getCustomerAddresses(Long customerId);

    /**
     * Créer une nouvelle adresse pour un client
     * 
     * @param customerId ID du profil client
     * @param request AddressCreateRequest contenant les détails de l'adresse
     * @return AddressDTO avec les informations de l'adresse créée
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    AddressDTO createAddress(Long customerId, AddressCreateRequest request);

    // ==================== GESTION DES FAVORIS ====================

    /**
     * Obtenir la liste des IDs des partenaires favoris
     * 
     * @param customerId ID du profil client
     * @return List<Long> liste des IDs de partenaires favoris
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    List<Long> getFavoritePartnerIds(Long customerId);

    /**
     * Ajouter un partenaire aux favoris
     * 
     * @param customerId ID du profil client
     * @param partnerId ID du partenaire à ajouter
     * @throws CustomerNotFoundException si le client n'existe pas
     */
    void addFavoritePartner(Long customerId, Long partnerId);
}
