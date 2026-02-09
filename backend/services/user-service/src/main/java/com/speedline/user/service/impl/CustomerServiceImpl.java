package com.speedline.user.service.impl;

import com.speedline.user.domain.Address;
import com.speedline.user.domain.Customer;
import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.dto.*;
import com.speedline.user.exception.CustomerNotFoundException;
import com.speedline.user.repository.AddressRepository;
import com.speedline.user.repository.CustomerRepository;
import com.speedline.user.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des clients
 * 
 * Ce service gère les opérations liées aux profils clients :
 * - Consultation, mise à jour et suppression
 * - Gestion des adresses
 * - Gestion des favoris
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AddressRepository addressRepository;

    // ==================== OPÉRATIONS CLIENT ====================

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long customerId) {
        log.debug("Recherche du client par ID: {}", customerId);
        Customer customer = findCustomerById(customerId);
        return mapToDTO(customer);
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(Long customerId, CustomerUpdateRequest request) {
        log.info("Mise à jour du profil client ID: {}", customerId);

        Customer customer = findCustomerById(customerId);

        // Mettre à jour les préférences si fournies
        if (request.getPreferences() != null) {
            customer.setPreferences(request.getPreferences());
        }

        // Mettre à jour les partenaires favoris si fournis
        if (request.getFavoritePartnerIds() != null) {
            customer.setFavoritePartnerIds(request.getFavoritePartnerIds());
        }

        // Mettre à jour les produits favoris si fournis
        if (request.getFavoriteProductIds() != null) {
            customer.setFavoriteProductIds(request.getFavoriteProductIds());
        }

        customer = customerRepository.save(customer);
        log.info("Profil client mis à jour avec succès. ID: {}", customerId);

        return mapToDTO(customer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long customerId) {
        log.info("Suppression (soft delete) du client ID: {}", customerId);

        Customer customer = findCustomerById(customerId);
        customer.setStatus(CustomerStatus.DELETED);
        customerRepository.save(customer);

        log.info("Client {} marqué comme supprimé", customerId);
    }

    // ==================== GESTION DES ADRESSES ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getCustomerAddresses(Long customerId) {
        log.debug("Récupération des adresses du client {}", customerId);
        
        // Vérifier que le client existe
        findCustomerById(customerId);
        
        List<Address> addresses = addressRepository.findByCustomerIdAndIsActiveTrue(customerId);
        return addresses.stream()
                .map(this::mapAddressToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO createAddress(Long customerId, AddressCreateRequest request) {
        log.info("Création d'une nouvelle adresse pour le client {}", customerId);

        Customer customer = findCustomerById(customerId);

        // Créer la nouvelle adresse
        Address address = Address.builder()
                .customerId(customerId)
                .userId(customer.getUserId())
                .type(request.getType())
                .label(request.getLabel())
                .street(request.getStreet())
                .building(request.getBuilding())
                .floor(request.getFloor())
                .apartment(request.getApartment())
                .accessCode(request.getAccessCode())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "Tunisie")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deliveryInstructions(request.getDeliveryInstructions())
                .landmark(request.getLandmark())
                .contactPhone(request.getContactPhone())
                .contactName(request.getContactName())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isActive(true)
                .isVerified(false)
                .build();

        // Générer l'adresse formatée
        address.setFormattedAddress(generateFormattedAddress(address));

        // Si c'est la première adresse ou marquée comme défaut, la définir comme défaut
        List<Address> existingAddresses = addressRepository.findByCustomerIdAndIsActiveTrue(customerId);
        if (existingAddresses.isEmpty() || Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForCustomer(customerId);
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        log.info("Adresse créée avec succès. ID: {}", address.getId());

        return mapAddressToDTO(address);
    }

    // ==================== GESTION DES FAVORIS ====================

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFavoritePartnerIds(Long customerId) {
        log.debug("Récupération des partenaires favoris du client {}", customerId);
        Customer customer = findCustomerById(customerId);
        return parseFavoriteIds(customer.getFavoritePartnerIds());
    }

    @Override
    @Transactional
    public void addFavoritePartner(Long customerId, Long partnerId) {
        log.info("Ajout du partenaire {} aux favoris du client {}", partnerId, customerId);

        Customer customer = findCustomerById(customerId);
        List<Long> favorites = parseFavoriteIds(customer.getFavoritePartnerIds());

        if (!favorites.contains(partnerId)) {
            favorites.add(partnerId);
            customer.setFavoritePartnerIds(joinFavoriteIds(favorites));
            customerRepository.save(customer);
            log.info("Partenaire {} ajouté aux favoris du client {}", partnerId, customerId);
        } else {
            log.debug("Partenaire {} déjà dans les favoris du client {}", partnerId, customerId);
        }
    }

    // ==================== MÉTHODES UTILITAIRES PRIVÉES ====================

    /**
     * Trouve un client par son ID ou lève une exception
     */
    private Customer findCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> CustomerNotFoundException.byId(customerId));
    }

    /**
     * Parse une chaîne d'IDs séparés par virgule en liste
     */
    private List<Long> parseFavoriteIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Joint une liste d'IDs en chaîne séparée par virgule
     */
    private String joinFavoriteIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /**
     * Génère une adresse formatée à partir des composantes
     */
    private String generateFormattedAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        
        if (address.getStreet() != null) {
            sb.append(address.getStreet());
        }
        if (address.getBuilding() != null) {
            sb.append(", ").append(address.getBuilding());
        }
        if (address.getCity() != null) {
            sb.append(", ").append(address.getCity());
        }
        if (address.getPostalCode() != null) {
            sb.append(" ").append(address.getPostalCode());
        }
        if (address.getCountry() != null) {
            sb.append(", ").append(address.getCountry());
        }
        
        return sb.toString();
    }

    /**
     * Mappe une entité Customer vers un DTO
     */
    private CustomerDTO mapToDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .userId(customer.getUserId())
                .walletBalance(customer.getWalletBalance())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .totalOrders(customer.getTotalOrders())
                .totalSpent(customer.getTotalSpent())
                .lastOrderDate(customer.getLastOrderDate())
                .status(customer.getStatus())
                .isVip(customer.getIsVip())
                .vipLevel(customer.getVipLevel())
                .referralCode(customer.getReferralCode())
                .successfulReferrals(customer.getSuccessfulReferrals())
                .preferences(customer.getPreferences())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    /**
     * Mappe une entité Address vers un DTO
     */
    private AddressDTO mapAddressToDTO(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .customerId(address.getCustomerId())
                .userId(address.getUserId())
                .type(address.getType())
                .label(address.getLabel())
                .street(address.getStreet())
                .building(address.getBuilding())
                .floor(address.getFloor())
                .apartment(address.getApartment())
                .accessCode(address.getAccessCode())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .state(address.getState())
                .country(address.getCountry())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .formattedAddress(address.getFormattedAddress())
                .deliveryInstructions(address.getDeliveryInstructions())
                .landmark(address.getLandmark())
                .contactPhone(address.getContactPhone())
                .contactName(address.getContactName())
                .isDefault(address.getIsDefault())
                .isVerified(address.getIsVerified())
                .createdAt(address.getCreatedAt())
                .lastUsedAt(address.getLastUsedAt())
                .build();
    }
}
