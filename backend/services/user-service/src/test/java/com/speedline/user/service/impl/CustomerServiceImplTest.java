package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.domain.Address;
import com.speedline.user.domain.Customer;
import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.domain.CustomerPreferences;
import com.speedline.user.dto.*;
import com.speedline.user.exception.CustomerNotFoundException;
import com.speedline.user.exception.DuplicateAddressLabelException;
import com.speedline.user.repository.AddressRepository;
import com.speedline.user.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer testCustomer;
    private UserInfoDTO testUserInfo;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .userId(100L)
                .status(CustomerStatus.ACTIVE)
                .walletBalance(BigDecimal.ZERO)
                .loyaltyPoints(0)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .isVip(false)
                .successfulReferrals(0)
                .preferences(new CustomerPreferences())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUserInfo = UserInfoDTO.builder()
                .id(100L)
                .email("test@speedline.com")
                .firstName("Ali")
                .lastName("Ben Salem")
                .phoneNumber("+21612345678")
                .build();
    }

    // ===================== CREATE CUSTOMER =====================

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomer {

        @Test
        @DisplayName("should create new customer when userId does not exist")
        void shouldCreateNewCustomer() {
            CustomerCreateRequest request = CustomerCreateRequest.builder()
                    .userId(100L)
                    .preferences(new CustomerPreferences())
                    .build();

            when(customerRepository.findByUserId(100L)).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
            when(authServiceClient.getUserById(100L)).thenReturn(testUserInfo);

            CustomerDTO result = customerService.createCustomer(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUserId()).isEqualTo(100L);
            assertThat(result.getEmail()).isEqualTo("test@speedline.com");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should return existing customer when userId already exists (idempotent)")
        void shouldReturnExistingCustomerIdempotent() {
            CustomerCreateRequest request = CustomerCreateRequest.builder()
                    .userId(100L)
                    .build();

            when(customerRepository.findByUserId(100L)).thenReturn(Optional.of(testCustomer));
            when(authServiceClient.getUserById(100L)).thenReturn(testUserInfo);

            CustomerDTO result = customerService.createCustomer(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    // ===================== GET CUSTOMER BY ID =====================

    @Nested
    @DisplayName("getCustomerById")
    class GetCustomerById {

        @Test
        @DisplayName("should return customer with addresses when found")
        void shouldReturnCustomerWhenFound() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(authServiceClient.getUserById(100L)).thenReturn(testUserInfo);
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());

            CustomerDTO result = customerService.getCustomerById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("Ali");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerById(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("should return customer even if auth-service call fails")
        void shouldReturnCustomerEvenIfAuthFails() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(authServiceClient.getUserById(100L)).thenThrow(new RuntimeException("auth-service down"));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());

            CustomerDTO result = customerService.getCustomerById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            // email/name are null because auth-service call failed
            assertThat(result.getEmail()).isNull();
        }
    }

    // ===================== UPDATE CUSTOMER =====================

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomer {

        @Test
        @DisplayName("should update preferences when provided")
        void shouldUpdatePreferences() {
            CustomerPreferences newPrefs = new CustomerPreferences();
            CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                    .preferences(newPrefs)
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
            when(authServiceClient.getUserById(100L)).thenReturn(testUserInfo);

            CustomerDTO result = customerService.updateCustomer(1L, request);

            assertThat(result).isNotNull();
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should update favorite partner ids")
        void shouldUpdateFavoritePartnerIds() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                    .favoritePartnerIds("1,5,12")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
            when(authServiceClient.getUserById(100L)).thenReturn(testUserInfo);

            CustomerDTO result = customerService.updateCustomer(1L, request);

            assertThat(result).isNotNull();
            verify(customerRepository).save(argThat(c ->
                    "1,5,12".equals(c.getFavoritePartnerIds())));
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when updating nonexistent customer")
        void shouldThrowWhenUpdatingNonexistent() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(999L, new CustomerUpdateRequest()))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ===================== DELETE CUSTOMER =====================

    @Nested
    @DisplayName("deleteCustomer")
    class DeleteCustomer {

        @Test
        @DisplayName("should soft delete customer by setting status to DELETED")
        void shouldSoftDeleteCustomer() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            customerService.deleteCustomer(1L);

            verify(customerRepository).save(argThat(c ->
                    c.getStatus() == CustomerStatus.DELETED));
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when deleting nonexistent")
        void shouldThrowWhenDeletingNonexistent() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ===================== GET CUSTOMER BY USER ID =====================

    @Nested
    @DisplayName("getCustomerByUserId")
    class GetCustomerByUserId {

        @Test
        @DisplayName("should return customer when found by userId")
        void shouldReturnByUserId() {
            when(customerRepository.findByUserId(100L)).thenReturn(Optional.of(testCustomer));
            when(authServiceClient.getUserById(100L)).thenReturn(testUserInfo);

            CustomerDTO result = customerService.getCustomerByUserId(100L);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when userId not found")
        void shouldThrowWhenUserIdNotFound() {
            when(customerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerByUserId(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ===================== ADDRESSES =====================

    @Nested
    @DisplayName("getCustomerAddresses")
    class GetCustomerAddresses {

        @Test
        @DisplayName("should return addresses for existing customer")
        void shouldReturnAddresses() {
            Address address = Address.builder()
                    .id(10L)
                    .customerId(1L)
                    .userId(100L)
                    .street("Rue Habib Bourguiba")
                    .city("Tunis")
                    .country("Tunisie")
                    .isDefault(true)
                    .isActive(true)
                    .isVerified(false)
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(List.of(address));

            List<AddressDTO> result = customerService.getCustomerAddresses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStreet()).isEqualTo("Rue Habib Bourguiba");
        }

        @Test
        @DisplayName("should return empty list when customer has no addresses")
        void shouldReturnEmptyListWhenNoAddresses() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());

            List<AddressDTO> result = customerService.getCustomerAddresses(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createAddress")
    class CreateAddress {

        @Test
        @DisplayName("should create first address as default")
        void shouldCreateFirstAddressAsDefault() {
            AddressCreateRequest request = AddressCreateRequest.builder()
                    .street("Avenue de la Republique")
                    .city("Tunis")
                    .latitude(new BigDecimal("36.8065"))
                    .longitude(new BigDecimal("10.1815"))
                    .build();

            Address savedAddress = Address.builder()
                    .id(10L)
                    .customerId(1L)
                    .userId(100L)
                    .street("Avenue de la Republique")
                    .city("Tunis")
                    .country("Tunisie")
                    .latitude(new BigDecimal("36.8065"))
                    .longitude(new BigDecimal("10.1815"))
                    .isDefault(true)
                    .isActive(true)
                    .isVerified(false)
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
            when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

            AddressDTO result = customerService.createAddress(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getIsDefault()).isTrue();
            verify(addressRepository).clearDefaultForCustomer(1L);
        }

        @Test
        @DisplayName("should throw DuplicateAddressLabelException for duplicate label")
        void shouldThrowForDuplicateLabel() {
            AddressCreateRequest request = AddressCreateRequest.builder()
                    .label("Maison")
                    .street("Some street")
                    .city("Tunis")
                    .build();

            Address existing = Address.builder().id(5L).customerId(1L).label("Maison").build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(addressRepository.findByCustomerIdAndLabelIgnoreCaseAndIsActiveTrue(1L, "Maison"))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> customerService.createAddress(1L, request))
                    .isInstanceOf(DuplicateAddressLabelException.class);
        }
    }

    // ===================== FAVORITES =====================

    @Nested
    @DisplayName("Favorites")
    class Favorites {

        @Test
        @DisplayName("should return parsed favorite partner ids")
        void shouldReturnFavoritePartnerIds() {
            testCustomer.setFavoritePartnerIds("1,5,12");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

            List<Long> result = customerService.getFavoritePartnerIds(1L);

            assertThat(result).containsExactly(1L, 5L, 12L);
        }

        @Test
        @DisplayName("should return empty list when no favorites")
        void shouldReturnEmptyListWhenNoFavorites() {
            testCustomer.setFavoritePartnerIds(null);
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

            List<Long> result = customerService.getFavoritePartnerIds(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should add new partner to favorites")
        void shouldAddPartnerToFavorites() {
            testCustomer.setFavoritePartnerIds("1,5");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

            customerService.addFavoritePartner(1L, 12L);

            verify(customerRepository).save(argThat(c ->
                    c.getFavoritePartnerIds().contains("12")));
        }

        @Test
        @DisplayName("should not duplicate partner in favorites")
        void shouldNotDuplicatePartner() {
            testCustomer.setFavoritePartnerIds("1,5,12");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

            customerService.addFavoritePartner(1L, 12L);

            verify(customerRepository, never()).save(any(Customer.class));
        }
    }
}
