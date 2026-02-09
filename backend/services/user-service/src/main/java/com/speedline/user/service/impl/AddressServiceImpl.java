package com.speedline.user.service.impl;

import com.speedline.user.domain.Address;
import com.speedline.user.domain.AddressType;
import com.speedline.user.domain.Customer;
import com.speedline.user.dto.*;
import com.speedline.user.exception.*;
import com.speedline.user.repository.AddressRepository;
import com.speedline.user.repository.CustomerRepository;
import com.speedline.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des adresses
 * 
 * Gère toutes les opérations liées aux adresses de livraison :
 * - CRUD complet
 * - Gestion de l'adresse par défaut
 * - Validation des coordonnées GPS
 * - Recherche et statistiques
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    // Constantes de validation
    private static final int MAX_ADDRESSES_PER_CUSTOMER = 10;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    // ==================== OPÉRATIONS CRUD ====================

    @Override
    @Transactional
    public AddressDTO createAddress(Long customerId, AddressCreateRequest request) {
        log.info("Création d'une nouvelle adresse pour le client {}", customerId);

        // Vérifier que le customer existe
        Customer customer = findCustomerById(customerId);

        // Vérifier le nombre max d'adresses
        long addressCount = addressRepository.countByCustomerIdAndIsActiveTrue(customerId);
        if (addressCount >= MAX_ADDRESSES_PER_CUSTOMER) {
            throw new IllegalArgumentException(
                    String.format("Le client a atteint le nombre maximum d'adresses (%d)", MAX_ADDRESSES_PER_CUSTOMER)
            );
        }

        // Valider les coordonnées GPS si fournies
        validateCoordinates(request.getLatitude(), request.getLongitude());

        // Créer l'adresse
        Address address = Address.builder()
                .customerId(customerId)
                .userId(customer.getUserId())
                .type(request.getType() != null ? request.getType() : AddressType.HOME)
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
                .placeId(request.getPlaceId())
                .deliveryInstructions(request.getDeliveryInstructions())
                .landmark(request.getLandmark())
                .contactPhone(request.getContactPhone())
                .contactName(request.getContactName())
                .isDefault(false)
                .isVerified(false)
                .isActive(true)
                .build();

        // Gérer l'adresse par défaut
        boolean isFirstAddress = addressCount == 0;
        boolean shouldBeDefault = Boolean.TRUE.equals(request.getIsDefault()) || isFirstAddress;

        if (shouldBeDefault) {
            addressRepository.clearDefaultForCustomer(customerId);
            address.setIsDefault(true);
            log.debug("Adresse définie comme adresse par défaut");
        }

        address = addressRepository.save(address);
        log.info("Adresse créée avec succès. ID: {}", address.getId());

        return mapToDTO(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long addressId) {
        log.debug("Récupération de l'adresse ID: {}", addressId);
        Address address = findAddressById(addressId);
        return mapToDTO(address);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressUpdateRequest request) {
        log.info("Mise à jour de l'adresse ID: {}", addressId);

        Address address = findAddressById(addressId);

        // Valider les coordonnées GPS si fournies
        BigDecimal newLat = request.getLatitude() != null ? request.getLatitude() : address.getLatitude();
        BigDecimal newLon = request.getLongitude() != null ? request.getLongitude() : address.getLongitude();
        validateCoordinates(newLat, newLon);

        // Mettre à jour les champs si fournis
        if (request.getType() != null) address.setType(request.getType());
        if (request.getLabel() != null) address.setLabel(request.getLabel());
        if (request.getStreet() != null) address.setStreet(request.getStreet());
        if (request.getBuilding() != null) address.setBuilding(request.getBuilding());
        if (request.getFloor() != null) address.setFloor(request.getFloor());
        if (request.getApartment() != null) address.setApartment(request.getApartment());
        if (request.getAccessCode() != null) address.setAccessCode(request.getAccessCode());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getLatitude() != null) address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) address.setLongitude(request.getLongitude());
        if (request.getPlaceId() != null) address.setPlaceId(request.getPlaceId());
        if (request.getDeliveryInstructions() != null) address.setDeliveryInstructions(request.getDeliveryInstructions());
        if (request.getLandmark() != null) address.setLandmark(request.getLandmark());
        if (request.getContactPhone() != null) address.setContactPhone(request.getContactPhone());
        if (request.getContactName() != null) address.setContactName(request.getContactName());

        // Gérer l'adresse par défaut
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultForCustomer(address.getCustomerId());
            address.setIsDefault(true);
            log.debug("Adresse {} définie comme adresse par défaut", addressId);
        }

        // Si coordonnées modifiées, marquer comme non vérifiée
        if (request.getLatitude() != null || request.getLongitude() != null) {
            address.setIsVerified(false);
        }

        address = addressRepository.save(address);
        log.info("Adresse {} mise à jour avec succès", addressId);

        return mapToDTO(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        log.info("Suppression (soft delete) de l'adresse ID: {}", addressId);

        Address address = findAddressById(addressId);
        Long customerId = address.getCustomerId();

        // Vérifier si c'est l'adresse par défaut
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            // Compter les autres adresses actives
            long otherAddressCount = addressRepository.countByCustomerIdAndIsActiveTrue(customerId) - 1;
            
            if (otherAddressCount > 0) {
                throw CannotDeleteDefaultAddressException.otherAddressesExist(addressId);
            }
            // Si c'est la seule adresse, on peut la supprimer
        }

        // Soft delete
        address.setIsActive(false);
        addressRepository.save(address);

        log.info("Adresse {} supprimée avec succès", addressId);
    }

    // ==================== ADRESSES D'UN CLIENT ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getCustomerAddresses(Long customerId) {
        log.debug("Récupération des adresses du client {}", customerId);
        findCustomerById(customerId);

        List<Address> addresses = addressRepository.findByCustomerIdAndIsActiveTrue(customerId);
        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddressDTO> getCustomerAddressesPaginated(Long customerId, Pageable pageable) {
        log.debug("Récupération des adresses du client {} (page {})", customerId, pageable.getPageNumber());
        findCustomerById(customerId);

        Page<Address> addressPage = addressRepository.findByCustomerIdAndIsActiveTrue(customerId, pageable);
        return addressPage.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getCustomerAddressesByType(Long customerId, AddressType type) {
        log.debug("Récupération des adresses de type {} du client {}", type, customerId);
        findCustomerById(customerId);

        List<Address> addresses = addressRepository.findByCustomerIdAndTypeAndIsActiveTrue(customerId, type);
        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countCustomerAddresses(Long customerId) {
        findCustomerById(customerId);
        return addressRepository.countByCustomerIdAndIsActiveTrue(customerId);
    }

    // ==================== ADRESSE PAR DÉFAUT ====================

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getDefaultAddress(Long customerId) {
        log.debug("Récupération de l'adresse par défaut du client {}", customerId);
        findCustomerById(customerId);

        Address address = addressRepository.findByCustomerIdAndIsDefaultTrueAndIsActiveTrue(customerId)
                .orElseThrow(() -> AddressNotFoundException.noDefaultAddress(customerId));
        
        return mapToDTO(address);
    }

    @Override
    @Transactional
    public AddressDTO setAsDefault(Long addressId) {
        log.info("Définition de l'adresse {} comme adresse par défaut", addressId);

        Address address = findAddressById(addressId);
        Long customerId = address.getCustomerId();

        // Retirer le flag des autres adresses
        addressRepository.clearDefaultForCustomer(customerId);

        // Définir cette adresse comme par défaut
        address.setIsDefault(true);
        address = addressRepository.save(address);

        log.info("Adresse {} définie comme adresse par défaut du client {}", addressId, customerId);
        return mapToDTO(address);
    }

    // ==================== VALIDATION ET GÉOCODAGE ====================

    @Override
    @Transactional
    public AddressDTO geocodeAddress(Long addressId) {
        log.info("Géocodage de l'adresse ID: {}", addressId);

        Address address = findAddressById(addressId);

        // TODO: Intégrer avec un service de géocodage (Google Maps, Mapbox, etc.)
        // Pour l'instant, on simule le géocodage
        log.warn("Géocodage non implémenté - service externe requis");

        return mapToDTO(address);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInDeliveryZone(Long addressId) {
        log.debug("Vérification si l'adresse {} est dans une zone de livraison", addressId);

        Address address = findAddressById(addressId);

        if (!address.hasCoordinates()) {
            throw InvalidCoordinatesException.addressNotGeocoded(addressId);
        }

        // TODO: Intégrer avec location-service pour vérifier la zone de livraison
        log.warn("Vérification de zone de livraison non implémentée - retourne true par défaut");
        return true;
    }

    @Override
    @Transactional
    public AddressDTO markAsVerified(Long addressId) {
        log.info("Marquage de l'adresse {} comme vérifiée", addressId);

        Address address = findAddressById(addressId);
        address.setIsVerified(true);
        address = addressRepository.save(address);

        log.info("Adresse {} marquée comme vérifiée", addressId);
        return mapToDTO(address);
    }

    // ==================== UTILISATION ====================

    @Override
    @Transactional
    public void markAsUsed(Long addressId) {
        log.debug("Marquage de l'adresse {} comme utilisée", addressId);

        findAddressById(addressId);
        addressRepository.markAsUsed(addressId);

        log.info("Adresse {} marquée comme utilisée", addressId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getMostUsedAddresses(Long customerId, int limit) {
        log.debug("Récupération des {} adresses les plus utilisées du client {}", limit, customerId);
        findCustomerById(customerId);

        Pageable pageable = PageRequest.of(0, limit);
        List<Address> addresses = addressRepository.findMostUsedByCustomer(customerId, pageable);

        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ==================== RECHERCHE ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> searchCustomerAddresses(Long customerId, String searchTerm) {
        log.debug("Recherche d'adresses pour le client {} avec le terme '{}'", customerId, searchTerm);
        findCustomerById(customerId);

        List<Address> addresses = addressRepository.searchByCustomer(customerId, searchTerm);
        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableCities() {
        log.debug("Récupération des villes disponibles");
        return addressRepository.findDistinctCities();
    }

    // ==================== VALIDATION ====================

    @Override
    @Transactional(readOnly = true)
    public boolean belongsToCustomer(Long addressId, Long customerId) {
        return addressRepository.existsByIdAndCustomerId(addressId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddAddress(Long customerId) {
        findCustomerById(customerId);
        long count = addressRepository.countByCustomerIdAndIsActiveTrue(customerId);
        return count < MAX_ADDRESSES_PER_CUSTOMER;
    }

    // ==================== MÉTHODES UTILITAIRES PRIVÉES ====================

    /**
     * Trouve un customer par son ID ou lève une exception
     */
    private Customer findCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> CustomerNotFoundException.byId(customerId));
    }

    /**
     * Trouve une adresse par son ID ou lève une exception
     */
    private Address findAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .filter(Address::getIsActive)
                .orElseThrow(() -> AddressNotFoundException.byId(addressId));
    }

    /**
     * Valide les coordonnées GPS
     */
    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        // Si une seule coordonnée est fournie, c'est une erreur
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            throw InvalidCoordinatesException.missingCoordinates();
        }

        // Si les deux sont fournies, valider les plages
        if (latitude != null) {
            if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
                throw InvalidCoordinatesException.invalidLatitude(latitude);
            }
        }
        if (longitude != null) {
            if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
                throw InvalidCoordinatesException.invalidLongitude(longitude);
            }
        }
    }

    /**
     * Mappe une entité Address vers un DTO
     */
    private AddressDTO mapToDTO(Address address) {
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
