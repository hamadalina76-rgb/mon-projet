package com.speedline.user.service;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.client.NotificationServiceClient;
import com.speedline.user.domain.Courier;
import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.VehicleType;
import com.speedline.user.dto.*;
import com.speedline.user.exception.CourierNotAvailableException;
import com.speedline.user.exception.CourierNotFoundException;
import com.speedline.user.exception.UserAlreadyExistsException;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.impl.CourierServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourierServiceImpl covering CRUD, availability, statistics, and schedule.
 */
@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private CourierServiceImpl courierService;

    private Courier sampleCourier;
    private UserInfoDTO sampleUserInfo;

    @BeforeEach
    void setUp() {
        sampleCourier = Courier.builder()
                .id(1L)
                .userId(42L)
                .vehicleType(VehicleType.MOTORCYCLE)
                .vehicleNumber("TU-1234")
                .vehicleModel("Honda PCX")
                .vehicleColor("Black")
                .status(CourierStatus.ACTIVE)
                .isAvailable(false)
                .isOnline(false)
                .documentsVerified(true)
                .rating(BigDecimal.ZERO)
                .totalRatings(0)
                .totalDeliveries(0)
                .successfulDeliveries(0)
                .cancelledDeliveries(0)
                .averageDeliveryTime(0)
                .totalEarnings(BigDecimal.ZERO)
                .weeklyEarnings(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .totalDistanceTravelled(BigDecimal.ZERO)
                .assignedZoneIds(new ArrayList<>())
                .build();

        sampleUserInfo = UserInfoDTO.builder()
                .id(42L)
                .email("courier@example.com")
                .firstName("Mohamed")
                .lastName("Trabelsi")
                .phoneNumber("+216 99 888 777")
                .build();
    }

    // ==================== createCourier ====================

    @Nested
    @DisplayName("createCourier")
    class CreateCourier {

        @Test
        @DisplayName("should create courier with PENDING_APPROVAL status")
        void createCourier_success() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(42L)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .vehicleNumber("TU-1234")
                    .build();

            when(courierRepository.existsByUserId(42L)).thenReturn(false);
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> {
                Courier c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CourierDTO result = courierService.createCourier(request);

            assertThat(result).isNotNull();
            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.PENDING_APPROVAL);
            assertThat(captor.getValue().getIsAvailable()).isFalse();
            assertThat(captor.getValue().getDocumentsVerified()).isFalse();
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException when profile exists")
        void createCourier_alreadyExists() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(42L)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .build();

            when(courierRepository.existsByUserId(42L)).thenReturn(true);

            assertThatThrownBy(() -> courierService.createCourier(request))
                    .isInstanceOf(UserAlreadyExistsException.class);
            verify(courierRepository, never()).save(any());
        }

        @Test
        @DisplayName("should send admin notification after creation")
        void createCourier_sendsAdminNotification() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(42L)
                    .vehicleType(VehicleType.BICYCLE)
                    .build();

            when(courierRepository.existsByUserId(42L)).thenReturn(false);
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> {
                Courier c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            courierService.createCourier(request);

            verify(notificationServiceClient).sendAdminBroadcast(any());
        }

        @Test
        @DisplayName("should not fail if admin notification throws")
        void createCourier_notificationFailure_doesNotThrow() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(42L)
                    .vehicleType(VehicleType.BICYCLE)
                    .build();

            when(courierRepository.existsByUserId(42L)).thenReturn(false);
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> {
                Courier c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);
            doThrow(new RuntimeException("Notification service down"))
                    .when(notificationServiceClient).sendAdminBroadcast(any());

            // Should not throw
            CourierDTO result = courierService.createCourier(request);
            assertThat(result).isNotNull();
        }
    }

    // ==================== getCourierById ====================

    @Nested
    @DisplayName("getCourierById")
    class GetCourierById {

        @Test
        @DisplayName("should return courier when found")
        void getCourierById_success() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CourierDTO result = courierService.getCourierById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getVehicleType()).isEqualTo(VehicleType.MOTORCYCLE);
        }

        @Test
        @DisplayName("should throw CourierNotFoundException when not found")
        void getCourierById_notFound() {
            when(courierRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierService.getCourierById(999L))
                    .isInstanceOf(CourierNotFoundException.class);
        }
    }

    // ==================== getCourierByUserId ====================

    @Nested
    @DisplayName("getCourierByUserId")
    class GetCourierByUserId {

        @Test
        @DisplayName("should return courier when found by userId")
        void getCourierByUserId_success() {
            when(courierRepository.findByUserId(42L)).thenReturn(Optional.of(sampleCourier));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CourierDTO result = courierService.getCourierByUserId(42L);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(42L);
            assertThat(result.getEmail()).isEqualTo("courier@example.com");
        }

        @Test
        @DisplayName("should throw CourierNotFoundException when userId not found")
        void getCourierByUserId_notFound() {
            when(courierRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierService.getCourierByUserId(999L))
                    .isInstanceOf(CourierNotFoundException.class);
        }
    }

    // ==================== updateCourier ====================

    @Nested
    @DisplayName("updateCourier")
    class UpdateCourier {

        @Test
        @DisplayName("should update vehicle fields when provided")
        void updateCourier_vehicleFields() {
            CourierUpdateRequest request = CourierUpdateRequest.builder()
                    .vehicleType(VehicleType.CAR)
                    .vehicleNumber("TU-5678")
                    .vehicleModel("Peugeot 208")
                    .vehicleColor("White")
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            courierService.updateCourier(1L, request);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            Courier saved = captor.getValue();
            assertThat(saved.getVehicleType()).isEqualTo(VehicleType.CAR);
            assertThat(saved.getVehicleNumber()).isEqualTo("TU-5678");
            assertThat(saved.getVehicleModel()).isEqualTo("Peugeot 208");
        }

        @Test
        @DisplayName("should not overwrite fields when request values are null")
        void updateCourier_nullFieldsPreserved() {
            CourierUpdateRequest request = CourierUpdateRequest.builder().build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            courierService.updateCourier(1L, request);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            Courier saved = captor.getValue();
            assertThat(saved.getVehicleType()).isEqualTo(VehicleType.MOTORCYCLE);
            assertThat(saved.getVehicleNumber()).isEqualTo("TU-1234");
        }

        @Test
        @DisplayName("should throw CourierNotFoundException when courier not found")
        void updateCourier_notFound() {
            CourierUpdateRequest request = CourierUpdateRequest.builder().build();
            when(courierRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierService.updateCourier(999L, request))
                    .isInstanceOf(CourierNotFoundException.class);
        }
    }

    // ==================== updateAvailability ====================

    @Nested
    @DisplayName("updateAvailability")
    class UpdateAvailability {

        @Test
        @DisplayName("should set AVAILABLE status when online and available")
        void updateAvailability_online() {
            CourierAvailabilityRequest request = CourierAvailabilityRequest.builder()
                    .isOnline(true)
                    .isAvailable(true)
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CourierDTO result = courierService.updateAvailability(1L, request);

            assertThat(result).isNotNull();
            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.AVAILABLE);
            assertThat(captor.getValue().getIsOnline()).isTrue();
            assertThat(captor.getValue().getIsAvailable()).isTrue();
        }

        @Test
        @DisplayName("should set OFFLINE status when going offline")
        void updateAvailability_offline() {
            CourierAvailabilityRequest request = CourierAvailabilityRequest.builder()
                    .isOnline(false)
                    .isAvailable(false)
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            courierService.updateAvailability(1L, request);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.OFFLINE);
        }

        @Test
        @DisplayName("should throw CourierNotAvailableException for PENDING_APPROVAL courier")
        void updateAvailability_pendingApproval() {
            sampleCourier.setStatus(CourierStatus.PENDING_APPROVAL);
            CourierAvailabilityRequest request = CourierAvailabilityRequest.builder()
                    .isOnline(true)
                    .isAvailable(true)
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.updateAvailability(1L, request))
                    .isInstanceOf(CourierNotAvailableException.class);
        }

        @Test
        @DisplayName("should throw CourierNotAvailableException for SUSPENDED courier")
        void updateAvailability_suspended() {
            sampleCourier.setStatus(CourierStatus.SUSPENDED);
            CourierAvailabilityRequest request = CourierAvailabilityRequest.builder()
                    .isOnline(true)
                    .isAvailable(true)
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.updateAvailability(1L, request))
                    .isInstanceOf(CourierNotAvailableException.class);
        }

        @Test
        @DisplayName("should throw CourierNotAvailableException for DEACTIVATED courier")
        void updateAvailability_deactivated() {
            sampleCourier.setStatus(CourierStatus.DEACTIVATED);
            CourierAvailabilityRequest request = CourierAvailabilityRequest.builder()
                    .isOnline(true)
                    .isAvailable(true)
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.updateAvailability(1L, request))
                    .isInstanceOf(CourierNotAvailableException.class);
        }

        @Test
        @DisplayName("should not change status to OFFLINE when courier has active delivery")
        void updateAvailability_busyCourier_noOffline() {
            sampleCourier.setCurrentDeliveryId(500L);
            sampleCourier.setStatus(CourierStatus.BUSY);
            CourierAvailabilityRequest request = CourierAvailabilityRequest.builder()
                    .isOnline(false)
                    .isAvailable(false)
                    .build();

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            courierService.updateAvailability(1L, request);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            // Status should remain BUSY because there is an active delivery
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.BUSY);
        }
    }

    // ==================== goOnline / goOffline ====================

    @Nested
    @DisplayName("goOnline / goOffline")
    class OnlineOffline {

        @Test
        @DisplayName("goOnline sets courier to AVAILABLE")
        void goOnline_success() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CourierDTO result = courierService.goOnline(1L);

            assertThat(result).isNotNull();
            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("goOffline sets courier to OFFLINE")
        void goOffline_success() {
            sampleCourier.setIsOnline(true);
            sampleCourier.setIsAvailable(true);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));
            when(authServiceClient.getUserById(42L)).thenReturn(sampleUserInfo);

            CourierDTO result = courierService.goOffline(1L);

            assertThat(result).isNotNull();
            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.OFFLINE);
        }

        @Test
        @DisplayName("goOnline throws for PENDING_APPROVAL courier")
        void goOnline_pendingApproval() {
            sampleCourier.setStatus(CourierStatus.PENDING_APPROVAL);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.goOnline(1L))
                    .isInstanceOf(CourierNotAvailableException.class);
        }
    }

    // ==================== markAsBusy / markAsAvailable ====================

    @Nested
    @DisplayName("markAsBusy / markAsAvailable")
    class BusyAvailable {

        @Test
        @DisplayName("markAsBusy sets delivery ID and unavailable")
        void markAsBusy() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.markAsBusy(1L, 500L);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentDeliveryId()).isEqualTo(500L);
            assertThat(captor.getValue().getIsAvailable()).isFalse();
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.BUSY);
        }

        @Test
        @DisplayName("markAsAvailable clears delivery ID")
        void markAsAvailable() {
            sampleCourier.setCurrentDeliveryId(500L);
            sampleCourier.setIsOnline(true);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.markAsAvailable(1L);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentDeliveryId()).isNull();
            assertThat(captor.getValue().getIsAvailable()).isTrue();
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        }
    }

    // ==================== getCourierStatistics ====================

    @Nested
    @DisplayName("getCourierStatistics")
    class GetCourierStatistics {

        @Test
        @DisplayName("should return statistics for existing courier")
        void getCourierStatistics_success() {
            sampleCourier.setTotalDeliveries(50);
            sampleCourier.setSuccessfulDeliveries(48);
            sampleCourier.setRating(new BigDecimal("4.75"));
            sampleCourier.setTotalEarnings(new BigDecimal("2500.00"));

            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

            CourierStatisticsDTO stats = courierService.getCourierStatistics(1L);

            assertThat(stats).isNotNull();
            assertThat(stats.getCourierId()).isEqualTo(1L);
            assertThat(stats.getTotalDeliveries()).isEqualTo(50);
            assertThat(stats.getSuccessfulDeliveries()).isEqualTo(48);
            assertThat(stats.getRating()).isEqualByComparingTo(new BigDecimal("4.75"));
            assertThat(stats.getTotalEarnings()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }

        @Test
        @DisplayName("should throw CourierNotFoundException when courier not found")
        void getCourierStatistics_notFound() {
            when(courierRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierService.getCourierStatistics(999L))
                    .isInstanceOf(CourierNotFoundException.class);
        }
    }

    // ==================== updateRating ====================

    @Nested
    @DisplayName("updateRating")
    class UpdateRating {

        @Test
        @DisplayName("should update rating for valid score")
        void updateRating_success() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.updateRating(1L, new BigDecimal("4.5"));

            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("should throw for rating below 1")
        void updateRating_belowMinimum() {
            assertThatThrownBy(() -> courierService.updateRating(1L, new BigDecimal("0.5")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw for rating above 5")
        void updateRating_aboveMaximum() {
            assertThatThrownBy(() -> courierService.updateRating(1L, new BigDecimal("5.5")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ==================== recordCompletedDelivery / recordCancelledDelivery ====================

    @Nested
    @DisplayName("recordCompletedDelivery / recordCancelledDelivery")
    class RecordDelivery {

        @Test
        @DisplayName("recordCompletedDelivery updates all stats")
        void recordCompletedDelivery() {
            sampleCourier.setTotalDeliveries(10);
            sampleCourier.setAverageDeliveryTime(20);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.recordCompletedDelivery(1L, new BigDecimal("15.00"),
                    new BigDecimal("5.5"), 25);

            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("recordCancelledDelivery increments cancelled count")
        void recordCancelledDelivery() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.recordCancelledDelivery(1L);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getCancelledDeliveries()).isEqualTo(1);
            assertThat(captor.getValue().getTotalDeliveries()).isEqualTo(1);
            assertThat(captor.getValue().getCurrentDeliveryId()).isNull();
            assertThat(captor.getValue().getIsAvailable()).isTrue();
        }
    }

    // ==================== existsByUserId ====================

    @Nested
    @DisplayName("existsByUserId")
    class ExistsByUserId {

        @Test
        @DisplayName("returns true when courier exists")
        void existsByUserId_true() {
            when(courierRepository.existsByUserId(42L)).thenReturn(true);
            assertThat(courierService.existsByUserId(42L)).isTrue();
        }

        @Test
        @DisplayName("returns false when courier does not exist")
        void existsByUserId_false() {
            when(courierRepository.existsByUserId(999L)).thenReturn(false);
            assertThat(courierService.existsByUserId(999L)).isFalse();
        }
    }

    // ==================== deactivateCourier / suspendCourier / reactivateCourier ====================

    @Nested
    @DisplayName("Admin actions: deactivate / suspend / reactivate")
    class AdminActions {

        @Test
        @DisplayName("deactivateCourier sets DEACTIVATED status and offline")
        void deactivateCourier() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.deactivateCourier(1L, "Policy violation");

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.DEACTIVATED);
            assertThat(captor.getValue().getIsAvailable()).isFalse();
            assertThat(captor.getValue().getIsOnline()).isFalse();
        }

        @Test
        @DisplayName("suspendCourier sets SUSPENDED status")
        void suspendCourier() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.suspendCourier(1L, "Temporary suspension");

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.SUSPENDED);
        }

        @Test
        @DisplayName("reactivateCourier sets ACTIVE status for SUSPENDED courier")
        void reactivateCourier_fromSuspended() {
            sampleCourier.setStatus(CourierStatus.SUSPENDED);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.reactivateCourier(1L);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.ACTIVE);
        }

        @Test
        @DisplayName("reactivateCourier sets PENDING_APPROVAL for REJECTED courier")
        void reactivateCourier_fromRejected() {
            sampleCourier.setStatus(CourierStatus.REJECTED);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);

            courierService.reactivateCourier(1L);

            ArgumentCaptor<Courier> captor = ArgumentCaptor.forClass(Courier.class);
            verify(courierRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CourierStatus.PENDING_APPROVAL);
        }

        @Test
        @DisplayName("reactivateCourier throws for ACTIVE courier")
        void reactivateCourier_invalidState() {
            sampleCourier.setStatus(CourierStatus.ACTIVE);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.reactivateCourier(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ==================== Courier entity helpers ====================

    @Nested
    @DisplayName("Courier entity helpers")
    class CourierEntityHelpers {

        @Test
        @DisplayName("goOnline sets isOnline and isAvailable to true")
        void goOnline() {
            sampleCourier.goOnline();
            assertThat(sampleCourier.getIsOnline()).isTrue();
            assertThat(sampleCourier.getIsAvailable()).isTrue();
            assertThat(sampleCourier.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("goOffline sets both to false")
        void goOffline() {
            sampleCourier.goOffline();
            assertThat(sampleCourier.getIsOnline()).isFalse();
            assertThat(sampleCourier.getIsAvailable()).isFalse();
        }

        @Test
        @DisplayName("startDelivery sets BUSY status and delivery ID")
        void startDelivery() {
            sampleCourier.startDelivery(500L);
            assertThat(sampleCourier.getCurrentDeliveryId()).isEqualTo(500L);
            assertThat(sampleCourier.getIsAvailable()).isFalse();
            assertThat(sampleCourier.getStatus()).isEqualTo(CourierStatus.BUSY);
        }

        @Test
        @DisplayName("completeDelivery updates all financial and delivery stats")
        void completeDelivery() {
            sampleCourier.startDelivery(500L);
            sampleCourier.completeDelivery(new BigDecimal("15.00"), new BigDecimal("5.5"));

            assertThat(sampleCourier.getCurrentDeliveryId()).isNull();
            assertThat(sampleCourier.getIsAvailable()).isTrue();
            assertThat(sampleCourier.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
            assertThat(sampleCourier.getTotalDeliveries()).isEqualTo(1);
            assertThat(sampleCourier.getSuccessfulDeliveries()).isEqualTo(1);
            assertThat(sampleCourier.getTotalEarnings()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(sampleCourier.getTotalDistanceTravelled()).isEqualByComparingTo(new BigDecimal("5.5"));
        }

        @Test
        @DisplayName("updateRating computes weighted average")
        void updateRating() {
            sampleCourier.setRating(new BigDecimal("4.00"));
            sampleCourier.setTotalRatings(4);
            sampleCourier.updateRating(new BigDecimal("5.00"));

            // (4.00*4 + 5.00) / 5 = 21/5 = 4.20
            assertThat(sampleCourier.getRating()).isEqualByComparingTo(new BigDecimal("4.20"));
            assertThat(sampleCourier.getTotalRatings()).isEqualTo(5);
        }

        @Test
        @DisplayName("getSuccessRate returns 0 when no deliveries")
        void getSuccessRate_noDeliveries() {
            assertThat(sampleCourier.getSuccessRate()).isEqualTo(0);
        }

        @Test
        @DisplayName("getSuccessRate computes percentage correctly")
        void getSuccessRate_withDeliveries() {
            sampleCourier.setTotalDeliveries(10);
            sampleCourier.setSuccessfulDeliveries(8);
            assertThat(sampleCourier.getSuccessRate()).isEqualTo(80.0);
        }
    }
}
