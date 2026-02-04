package com.speedline.user.service.impl;

import com.speedline.user.domain.Customer;
import com.speedline.user.dto.*;
import com.speedline.user.repository.CustomerRepository;
import com.speedline.user.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implémentation du service de gestion des clients
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    // TODO: Injecter d'autres services si nécessaire (ex: PaymentServiceClient pour wallet)

    // ==================== OPÉRATIONS CRUD ====================

    @Override
    @Transactional
    public CustomerDTO createCustomer(CustomerCreateRequest request) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByUserId(Long userId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(Long customerId, CustomerUpdateRequest request) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void deleteCustomer(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== GESTION DU WALLET ====================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public BigDecimal addToWallet(Long customerId, BigDecimal amount) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public BigDecimal deductFromWallet(Long customerId, BigDecimal amount) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== GESTION DES POINTS DE FIDÉLITÉ ====================

    @Override
    @Transactional(readOnly = true)
    public Integer getLoyaltyPoints(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public Integer addLoyaltyPoints(Long customerId, int points) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public Integer useLoyaltyPoints(Long customerId, int points) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== GESTION DES FAVORIS ====================

    @Override
    @Transactional
    public void addFavoritePartner(Long customerId, Long partnerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void removeFavoritePartner(Long customerId, Long partnerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFavoritePartnerIds(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void addFavoriteProduct(Long customerId, Long productId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void removeFavoriteProduct(Long customerId, Long productId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFavoriteProductIds(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== PARRAINAGE ====================

    @Override
    @Transactional(readOnly = true)
    public String getReferralCode(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void applyReferralCode(Long customerId, String referralCode) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getReferrals(Long customerId) {
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== STATISTIQUES ET RECHERCHE ====================

    @Override
    @Transactional
    public void incrementOrderCount(Long customerId, BigDecimal orderAmount) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getAllCustomers(Pageable pageable) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getVipCustomers(Pageable pageable) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getTopSpenders(Pageable pageable) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getInactiveCustomers(int days, Pageable pageable) {
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        // Exemple d'implémentation finale possible :
        // return customerRepository.existsByUserId(userId);
        throw new UnsupportedOperationException("À implémenter");
    }
}
