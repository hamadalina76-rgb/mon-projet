package com.speedline.user.service.impl;

import com.speedline.user.domain.AddressType;
import com.speedline.user.dto.*;
import com.speedline.user.repository.AddressRepository;
import com.speedline.user.repository.CustomerRepository;
import com.speedline.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation du service de gestion des adresses
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    // TODO: Injecter LocationServiceClient pour géocodage si nécessaire

    // ==================== OPÉRATIONS CRUD ====================

    @Override
    @Transactional
    public AddressDTO createAddress(Long customerId, AddressCreateRequest request) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressUpdateRequest request) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== ADRESSES D'UN CLIENT ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getCustomerAddresses(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddressDTO> getCustomerAddressesPaginated(Long customerId, Pageable pageable) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getCustomerAddressesByType(Long customerId, AddressType type) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public long countCustomerAddresses(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== ADRESSE PAR DÉFAUT ====================

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getDefaultAddress(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public AddressDTO setAsDefault(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== VALIDATION ET GÉOCODAGE ====================

    @Override
    @Transactional
    public AddressDTO geocodeAddress(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInDeliveryZone(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public AddressDTO markAsVerified(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== UTILISATION ====================

    @Override
    @Transactional
    public void markAsUsed(Long addressId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getMostUsedAddresses(Long customerId, int limit) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== RECHERCHE ====================

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> searchCustomerAddresses(Long customerId, String searchTerm) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableCities() {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== VALIDATION ====================

    @Override
    @Transactional(readOnly = true)
    public boolean belongsToCustomer(Long addressId, Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddAddress(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }
}
