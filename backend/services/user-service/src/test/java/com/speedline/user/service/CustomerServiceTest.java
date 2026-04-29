package com.speedline.user.service;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.domain.Address;
import com.speedline.user.domain.Customer;
import com.speedline.user.domain.Customer.CustomerStatus;
import com.speedline.user.dto.*;
import com.speedline.user.exception.CustomerNotFoundException;
import com.speedline.user.exception.DuplicateAddressLabelException;
import com.speedline.user.repository.AddressRepository;
import com.speedline.user.repository.CustomerRepository;
import com.speedline.user.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerServiceImpl covering CRUD, addresses, and favorites.
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer sampleCustomer;
    private UserInfoDTO sampleUserInfo;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(1L)
                .userId(42L)
                .status(CustomerStatus.ACTIVE)
                .walletBalance(BigDecimal.ZERO)
                .loyaltyPoints(0)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .successfulReferrals(0)
                .isVip(false)
                .build();

        sampleUserInfo = UserInfoDTO.builder()
                .id(42L)
                .email("ahmed@example.com")
                .firstName("Ahmed")
                .lastName("Ben Ali")
                .phoneNumber("+216 55 123 456")
                .build();
    }

    // ==================== createCustomer ====================

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomer {

        @Test
        @DisplayName("should create new customer when none exists for userId")
        void createCustomer_success() {
            CustomerCreateRequest request = CustomerCreateRequest.builder()
                    .userId(42L)
                    .build();

            when(customerRepository.findByUserId(42L)).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CustomerDTO result = customerService.createCustomer(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUserId()).isEqualTo(42L);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should return existing customer idempotently when profile already exists")
        void createCustomer_idempotent() {
            CustomerCreateRequest request = CustomerCreateRequest.builder()
                    .userId(42L)
                    .build();

            when(customerRepository.findByUserId(42L)).thenReturn(Optional.of(sampleCustomer));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CustomerDTO result = customerService.createCustomer(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("should set status to ACTIVE for new customer")
        void createCustomer_setsActiveStatus() {
            CustomerCreateRequest request = CustomerCreateRequest.builder()
                    .userId(42L)
                    .build();

            when(customerRepository.findByUserId(42L)).thenReturn(Optional.empty());
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CustomerDTO result = customerService.createCustomer(request);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }
    }

    // ==================== getCustomerById ====================

    @Nested
    @DisplayName("getCustomerById")
    class GetCustomerById {

        @Test
        @DisplayName("should return customer when found")
        void getCustomerById_success() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CustomerDTO result = customerService.getCustomerById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("ahmed@example.com");
            assertThat(result.getFirstName()).isEqualTo("Ahmed");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when not found")
        void getCustomerById_notFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerById(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("should still return customer when auth-service call fails")
        void getCustomerById_authServiceFailure() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
            when(authServiceClient.getUserById(42L)).thenThrow(new RuntimeException("Auth service down"));

            CustomerDTO result = customerService.getCustomerById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            // Email/name fields will be null since auth call failed
            assertThat(result.getEmail()).isNull();
        }
    }

    // ==================== getCustomerByUserId ====================

    @Nested
    @DisplayName("getCustomerByUserId")
    class GetCustomerByUserId {

        @Test
        @DisplayName("should return customer when found by userId")
        void getCustomerByUserId_success() {
            when(customerRepository.findByUserId(42L)).thenReturn(Optional.of(sampleCustomer));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CustomerDTO result = customerService.getCustomerByUserId(42L);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when userId not found")
        void getCustomerByUserId_notFound() {
            when(customerRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerByUserId(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ==================== updateCustomer ====================

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomer {

        @Test
        @DisplayName("should update preferences when provided")
        void updateCustomer_updatePreferences() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                    .favoritePartnerIds("1,2,3")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CustomerDTO result = customerService.updateCustomer(1L, request);

            assertThat(result).isNotNull();
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should update favoritePartnerIds when provided")
        void updateCustomer_updateFavorites() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder()
                    .favoritePartnerIds("10,20,30")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            customerService.updateCustomer(1L, request);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getFavoritePartnerIds()).isEqualTo("10,20,30");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void updateCustomer_notFound() {
            CustomerUpdateRequest request = CustomerUpdateRequest.builder().build();
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(999L, request))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("should not overwrite fields when request values are null")
        void updateCustomer_nullFieldsPreserved() {
            sampleCustomer.setFavoritePartnerIds("1,2,3");
            CustomerUpdateRequest request = CustomerUpdateRequest.builder().build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            customerService.updateCustomer(1L, request);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getFavoritePartnerIds()).isEqualTo("1,2,3");
        }
    }

    // ==================== deleteCustomer ====================

    @Nested
    @DisplayName("deleteCustomer")
    class DeleteCustomer {

        @Test
        @DisplayName("should soft-delete customer by setting status to DELETED")
        void deleteCustomer_success() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            customerService.deleteCustomer(1L);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.DELETED);
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void deleteCustomer_notFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ==================== getCustomerAddresses ====================

    @Nested
    @DisplayName("getCustomerAddresses")
    class GetCustomerAddresses {

        @Test
        @DisplayName("should return empty list when customer has no addresses")
        void getCustomerAddresses_empty() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());

            List<AddressDTO> result = customerService.getCustomerAddresses(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return addresses when customer has them")
        void getCustomerAddresses_withAddresses() {
            Address address = Address.builder()
                    .id(10L)
                    .customerId(1L)
                    .userId(42L)
                    .street("Rue Habib Bourguiba")
                    .city("Tunis")
                    .country("Tunisie")
                    .isDefault(true)
                    .isVerified(false)
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(List.of(address));

            List<AddressDTO> result = customerService.getCustomerAddresses(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStreet()).isEqualTo("Rue Habib Bourguiba");
            assertThat(result.get(0).getCity()).isEqualTo("Tunis");
        }

        @Test
        @DisplayName("should throw CustomerNotFoundException when customer not found")
        void getCustomerAddresses_customerNotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerAddresses(999L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ==================== createAddress ====================

    @Nested
    @DisplayName("createAddress")
    class CreateAddress {

        @Test
        @DisplayName("should create address and set as default when first address")
        void createAddress_firstAddress_isDefault() {
            AddressCreateRequest request = AddressCreateRequest.builder()
                    .street("10 Avenue de la Republique")
                    .city("Tunis")
                    .country("Tunisie")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
                Address a = inv.getArgument(0);
                a.setId(10L);
                return a;
            });

            AddressDTO result = customerService.createAddress(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getIsDefault()).isTrue();
            verify(addressRepository).clearDefaultForCustomer(1L);
        }

        @Test
        @DisplayName("should throw DuplicateAddressLabelException for duplicate label")
        void createAddress_duplicateLabel() {
            AddressCreateRequest request = AddressCreateRequest.builder()
                    .label("Maison")
                    .street("Some street")
                    .city("Tunis")
                    .build();

            Address existing = Address.builder().id(5L).label("Maison").build();
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndLabelIgnoreCaseAndIsActiveTrue(1L, "Maison"))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> customerService.createAddress(1L, request))
                    .isInstanceOf(DuplicateAddressLabelException.class);
        }

        @Test
        @DisplayName("should use formatted address from request when provided")
        void createAddress_usesFormattedAddress() {
            AddressCreateRequest request = AddressCreateRequest.builder()
                    .street("5 Rue de la Paix")
                    .city("Sfax")
                    .formattedAddress("5 Rue de la Paix, Sfax, Tunisie")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
                Address a = inv.getArgument(0);
                a.setId(11L);
                return a;
            });

            AddressDTO result = customerService.createAddress(1L, request);

            assertThat(result.getFormattedAddress()).isEqualTo("5 Rue de la Paix, Sfax, Tunisie");
        }

        @Test
        @DisplayName("should default country to Tunisie when not provided")
        void createAddress_defaultCountry() {
            AddressCreateRequest request = AddressCreateRequest.builder()
                    .street("Test Street")
                    .city("Sousse")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(addressRepository.findByCustomerIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
                Address a = inv.getArgument(0);
                a.setId(12L);
                return a;
            });

            AddressDTO result = customerService.createAddress(1L, request);

            assertThat(result.getCountry()).isEqualTo("Tunisie");
        }
    }

    // ==================== getFavoritePartnerIds ====================

    @Nested
    @DisplayName("getFavoritePartnerIds")
    class GetFavoritePartnerIds {

        @Test
        @DisplayName("should return parsed list of partner IDs")
        void getFavoritePartnerIds_success() {
            sampleCustomer.setFavoritePartnerIds("1,5,12,45");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

            List<Long> result = customerService.getFavoritePartnerIds(1L);

            assertThat(result).containsExactly(1L, 5L, 12L, 45L);
        }

        @Test
        @DisplayName("should return empty list when favorites is null")
        void getFavoritePartnerIds_null() {
            sampleCustomer.setFavoritePartnerIds(null);
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

            List<Long> result = customerService.getFavoritePartnerIds(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when favorites is blank")
        void getFavoritePartnerIds_blank() {
            sampleCustomer.setFavoritePartnerIds("  ");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

            List<Long> result = customerService.getFavoritePartnerIds(1L);

            assertThat(result).isEmpty();
        }
    }

    // ==================== addFavoritePartner ====================

    @Nested
    @DisplayName("addFavoritePartner")
    class AddFavoritePartner {

        @Test
        @DisplayName("should add partner to empty favorites")
        void addFavoritePartner_toEmpty() {
            sampleCustomer.setFavoritePartnerIds(null);
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            customerService.addFavoritePartner(1L, 10L);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getFavoritePartnerIds()).isEqualTo("10");
        }

        @Test
        @DisplayName("should append partner to existing favorites")
        void addFavoritePartner_toExisting() {
            sampleCustomer.setFavoritePartnerIds("1,5");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

            customerService.addFavoritePartner(1L, 10L);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getFavoritePartnerIds()).isEqualTo("1,5,10");
        }

        @Test
        @DisplayName("should not duplicate if partner already in favorites")
        void addFavoritePartner_alreadyExists() {
            sampleCustomer.setFavoritePartnerIds("1,5,10");
            when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

            customerService.addFavoritePartner(1L, 10L);

            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    // ==================== Customer entity helpers ====================

    @Nested
    @DisplayName("Customer entity helpers")
    class CustomerEntityHelpers {

        @Test
        @DisplayName("addLoyaltyPoints updates VIP level to BRONZE at 500 points")
        void addLoyaltyPoints_bronze() {
            sampleCustomer.addLoyaltyPoints(500);
            assertThat(sampleCustomer.getVipLevel()).isEqualTo(Customer.VipLevel.BRONZE);
            assertThat(sampleCustomer.getIsVip()).isTrue();
        }

        @Test
        @DisplayName("addLoyaltyPoints updates VIP level to PLATINUM at 10000 points")
        void addLoyaltyPoints_platinum() {
            sampleCustomer.addLoyaltyPoints(10000);
            assertThat(sampleCustomer.getVipLevel()).isEqualTo(Customer.VipLevel.PLATINUM);
        }

        @Test
        @DisplayName("useLoyaltyPoints returns true and deducts when sufficient")
        void useLoyaltyPoints_sufficient() {
            sampleCustomer.setLoyaltyPoints(100);
            boolean result = sampleCustomer.useLoyaltyPoints(50);
            assertThat(result).isTrue();
            assertThat(sampleCustomer.getLoyaltyPoints()).isEqualTo(50);
        }

        @Test
        @DisplayName("useLoyaltyPoints returns false when insufficient")
        void useLoyaltyPoints_insufficient() {
            sampleCustomer.setLoyaltyPoints(30);
            boolean result = sampleCustomer.useLoyaltyPoints(50);
            assertThat(result).isFalse();
            assertThat(sampleCustomer.getLoyaltyPoints()).isEqualTo(30);
        }

        @Test
        @DisplayName("incrementOrderCount updates totalOrders, totalSpent, and lastOrderDate")
        void incrementOrderCount() {
            sampleCustomer.incrementOrderCount(new BigDecimal("25.00"));
            assertThat(sampleCustomer.getTotalOrders()).isEqualTo(1);
            assertThat(sampleCustomer.getTotalSpent()).isEqualByComparingTo(new BigDecimal("25.00"));
            assertThat(sampleCustomer.getLastOrderDate()).isNotNull();
        }
    }
}
