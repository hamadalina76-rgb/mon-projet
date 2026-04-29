package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.client.NotificationServiceClient;
import com.speedline.user.domain.Courier;
import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.CourierType;
import com.speedline.user.domain.VehicleType;
import com.speedline.user.dto.*;
import com.speedline.user.exception.CourierNotAvailableException;
import com.speedline.user.exception.CourierNotFoundException;
import com.speedline.user.exception.UserAlreadyExistsException;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.FileStorageService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierServiceImplTest {

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

    private Courier testCourier;
    private UserInfoDTO testUserInfo;

    @BeforeEach
    void setUp() {
        testCourier = Courier.builder()
                .id(1L)
                .userId(200L)
                .vehicleType(VehicleType.MOTORCYCLE)
                .vehicleNumber("TU-1234")
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
                .createdAt(LocalDateTime.now())
                .build();

        testUserInfo = UserInfoDTO.builder()
                .id(200L)
                .email("courier@speedline.com")
                .firstName("Mohamed")
                .lastName("Trabelsi")
                .phoneNumber("+21698765432")
                .build();
    }

    // ===================== CREATE COURIER =====================

    @Nested
    @DisplayName("createCourier")
    class CreateCourier {

        @Test
        @DisplayName("should create courier with PENDING_APPROVAL status")
        void shouldCreateCourier() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(200L)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .vehicleNumber("TU-1234")
                    .build();

            Courier pendingCourier = Courier.builder()
                    .id(1L)
                    .userId(200L)
                    .vehicleType(VehicleType.MOTORCYCLE)
                    .vehicleNumber("TU-1234")
                    .status(CourierStatus.PENDING_APPROVAL)
                    .isAvailable(false)
                    .isOnline(false)
                    .documentsVerified(false)
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

            when(courierRepository.existsByUserId(200L)).thenReturn(false);
            when(courierRepository.save(any(Courier.class))).thenReturn(pendingCourier);
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            CourierDTO result = courierService.createCourier(request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CourierStatus.PENDING_APPROVAL);
            assertThat(result.getIsAvailable()).isFalse();
            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException when profile exists")
        void shouldThrowWhenProfileExists() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(200L)
                    .build();

            when(courierRepository.existsByUserId(200L)).thenReturn(true);

            assertThatThrownBy(() -> courierService.createCourier(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(courierRepository, never()).save(any(Courier.class));
        }

        @Test
        @DisplayName("should still create courier even if admin notification fails")
        void shouldCreateCourierEvenIfNotificationFails() {
            CourierCreateRequest request = CourierCreateRequest.builder()
                    .userId(200L)
                    .build();

            Courier saved = Courier.builder()
                    .id(1L).userId(200L).status(CourierStatus.PENDING_APPROVAL)
                    .isAvailable(false).isOnline(false).documentsVerified(false)
                    .rating(BigDecimal.ZERO).totalRatings(0).totalDeliveries(0)
                    .successfulDeliveries(0).cancelledDeliveries(0).averageDeliveryTime(0)
                    .totalEarnings(BigDecimal.ZERO).weeklyEarnings(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO).totalDistanceTravelled(BigDecimal.ZERO)
                    .assignedZoneIds(new ArrayList<>())
                    .build();

            when(courierRepository.existsByUserId(200L)).thenReturn(false);
            when(courierRepository.save(any(Courier.class))).thenReturn(saved);
            doThrow(new RuntimeException("notification-service down"))
                    .when(notificationServiceClient).sendAdminBroadcast(any());
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            CourierDTO result = courierService.createCourier(request);

            assertThat(result).isNotNull();
        }
    }

    // ===================== GET COURIER =====================

    @Nested
    @DisplayName("getCourierById / getCourierByUserId")
    class GetCourier {

        @Test
        @DisplayName("should return courier by ID")
        void shouldReturnCourierById() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            CourierDTO result = courierService.getCourierById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo("Mohamed");
        }

        @Test
        @DisplayName("should throw CourierNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(courierRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierService.getCourierById(999L))
                    .isInstanceOf(CourierNotFoundException.class);
        }

        @Test
        @DisplayName("should return courier by userId")
        void shouldReturnCourierByUserId() {
            when(courierRepository.findByUserId(200L)).thenReturn(Optional.of(testCourier));
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            CourierDTO result = courierService.getCourierByUserId(200L);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(200L);
        }
    }

    // ===================== AVAILABILITY =====================

    @Nested
    @DisplayName("Availability management")
    class Availability {

        @Test
        @DisplayName("goOnline should set AVAILABLE status for active courier")
        void goOnlineShouldSetAvailable() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            CourierDTO result = courierService.goOnline(1L);

            assertThat(result).isNotNull();
            verify(courierRepository).save(argThat(c ->
                    c.getIsOnline() && c.getIsAvailable() &&
                    c.getStatus() == CourierStatus.AVAILABLE));
        }

        @Test
        @DisplayName("goOnline should throw for PENDING_APPROVAL courier")
        void goOnlineShouldThrowForPending() {
            testCourier.setStatus(CourierStatus.PENDING_APPROVAL);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));

            assertThatThrownBy(() -> courierService.goOnline(1L))
                    .isInstanceOf(CourierNotAvailableException.class);
        }

        @Test
        @DisplayName("goOnline should throw for SUSPENDED courier")
        void goOnlineShouldThrowForSuspended() {
            testCourier.setStatus(CourierStatus.SUSPENDED);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));

            assertThatThrownBy(() -> courierService.goOnline(1L))
                    .isInstanceOf(CourierNotAvailableException.class);
        }

        @Test
        @DisplayName("goOnline should throw for DEACTIVATED courier")
        void goOnlineShouldThrowForDeactivated() {
            testCourier.setStatus(CourierStatus.DEACTIVATED);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));

            assertThatThrownBy(() -> courierService.goOnline(1L))
                    .isInstanceOf(CourierNotAvailableException.class);
        }

        @Test
        @DisplayName("goOffline should set offline and unavailable")
        void goOfflineShouldSetOffline() {
            testCourier.setIsOnline(true);
            testCourier.setIsAvailable(true);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            courierService.goOffline(1L);

            verify(courierRepository).save(argThat(c ->
                    !c.getIsOnline() && !c.getIsAvailable()));
        }

        @Test
        @DisplayName("markAsBusy should assign delivery and set BUSY")
        void markAsBusyShouldSetBusy() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.markAsBusy(1L, 500L);

            verify(courierRepository).save(argThat(c ->
                    c.getCurrentDeliveryId() != null &&
                    c.getCurrentDeliveryId().equals(500L) &&
                    c.getStatus() == CourierStatus.BUSY));
        }

        @Test
        @DisplayName("markAsAvailable should clear delivery and set available")
        void markAsAvailableShouldClear() {
            testCourier.setCurrentDeliveryId(500L);
            testCourier.setIsOnline(true);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.markAsAvailable(1L);

            verify(courierRepository).save(argThat(c ->
                    c.getCurrentDeliveryId() == null &&
                    c.getIsAvailable()));
        }
    }

    // ===================== DOCUMENT VERIFICATION =====================

    @Nested
    @DisplayName("Document verification")
    class DocumentVerification {

        @Test
        @DisplayName("should approve courier with PENDING_APPROVAL status")
        void shouldApproveCourier() {
            testCourier.setStatus(CourierStatus.PENDING_APPROVAL);
            testCourier.setAssignedZoneIds(new ArrayList<>());

            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);
            when(authServiceClient.getUserById(200L)).thenReturn(testUserInfo);

            CourierDTO result = courierService.verifyDocuments(1L, CourierType.INTERNAL, List.of(10L, 20L));

            assertThat(result).isNotNull();
            verify(courierRepository).save(argThat(c ->
                    c.getDocumentsVerified() &&
                    c.getStatus() == CourierStatus.ACTIVE &&
                    c.getCourierType() == CourierType.INTERNAL));
        }

        @Test
        @DisplayName("should throw if courier is not PENDING_APPROVAL")
        void shouldThrowIfNotPending() {
            testCourier.setStatus(CourierStatus.ACTIVE);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));

            assertThatThrownBy(() -> courierService.verifyDocuments(1L, CourierType.EXTERNAL, List.of()))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should reject courier and set rejection reason")
        void shouldRejectCourier() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.rejectDocuments(1L, "Documents illegibles");

            verify(courierRepository).save(argThat(c ->
                    c.getStatus() == CourierStatus.REJECTED &&
                    "Documents illegibles".equals(c.getRejectionReason())));
        }
    }

    // ===================== STATISTICS & RATINGS =====================

    @Nested
    @DisplayName("Statistics and Ratings")
    class Statistics {

        @Test
        @DisplayName("should return courier statistics")
        void shouldReturnStatistics() {
            testCourier.setTotalDeliveries(50);
            testCourier.setSuccessfulDeliveries(48);
            testCourier.setRating(new BigDecimal("4.75"));
            testCourier.setTotalEarnings(new BigDecimal("2500.00"));

            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));

            CourierStatisticsDTO stats = courierService.getCourierStatistics(1L);

            assertThat(stats.getCourierId()).isEqualTo(1L);
            assertThat(stats.getTotalDeliveries()).isEqualTo(50);
            assertThat(stats.getSuccessfulDeliveries()).isEqualTo(48);
            assertThat(stats.getRating()).isEqualTo(new BigDecimal("4.75"));
        }

        @Test
        @DisplayName("should update rating within valid range")
        void shouldUpdateRating() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.updateRating(1L, new BigDecimal("4.5"));

            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("should throw for rating below 1")
        void shouldThrowForLowRating() {
            assertThatThrownBy(() -> courierService.updateRating(1L, new BigDecimal("0.5")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entre 1 et 5");
        }

        @Test
        @DisplayName("should throw for rating above 5")
        void shouldThrowForHighRating() {
            assertThatThrownBy(() -> courierService.updateRating(1L, new BigDecimal("5.5")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should record completed delivery")
        void shouldRecordCompletedDelivery() {
            testCourier.setTotalDeliveries(10);
            testCourier.setAverageDeliveryTime(30);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.recordCompletedDelivery(1L, new BigDecimal("15.00"), new BigDecimal("5.5"), 25);

            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("should record cancelled delivery")
        void shouldRecordCancelledDelivery() {
            testCourier.setTotalDeliveries(10);
            testCourier.setCancelledDeliveries(1);
            testCourier.setCurrentDeliveryId(500L);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.recordCancelledDelivery(1L);

            verify(courierRepository).save(argThat(c ->
                    c.getCancelledDeliveries() == 2 &&
                    c.getTotalDeliveries() == 11 &&
                    c.getCurrentDeliveryId() == null &&
                    c.getIsAvailable()));
        }
    }

    // ===================== ADMIN OPERATIONS =====================

    @Nested
    @DisplayName("Admin operations")
    class AdminOperations {

        @Test
        @DisplayName("should deactivate courier")
        void shouldDeactivateCourier() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.deactivateCourier(1L, "Comportement inapproprie");

            verify(courierRepository).save(argThat(c ->
                    c.getStatus() == CourierStatus.DEACTIVATED &&
                    !c.getIsAvailable() && !c.getIsOnline()));
        }

        @Test
        @DisplayName("should suspend courier")
        void shouldSuspendCourier() {
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.suspendCourier(1L, "Enquete en cours");

            verify(courierRepository).save(argThat(c ->
                    c.getStatus() == CourierStatus.SUSPENDED));
        }

        @Test
        @DisplayName("should reactivate suspended courier to ACTIVE")
        void shouldReactivateSuspendedCourier() {
            testCourier.setStatus(CourierStatus.SUSPENDED);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.reactivateCourier(1L);

            verify(courierRepository).save(argThat(c ->
                    c.getStatus() == CourierStatus.ACTIVE));
        }

        @Test
        @DisplayName("should reactivate rejected courier to PENDING_APPROVAL")
        void shouldReactivateRejectedCourier() {
            testCourier.setStatus(CourierStatus.REJECTED);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(testCourier);

            courierService.reactivateCourier(1L);

            verify(courierRepository).save(argThat(c ->
                    c.getStatus() == CourierStatus.PENDING_APPROVAL));
        }

        @Test
        @DisplayName("should throw when reactivating already ACTIVE courier")
        void shouldThrowWhenReactivatingActive() {
            testCourier.setStatus(CourierStatus.ACTIVE);
            when(courierRepository.findById(1L)).thenReturn(Optional.of(testCourier));

            assertThatThrownBy(() -> courierService.reactivateCourier(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should check existsByUserId")
        void shouldCheckExistsByUserId() {
            when(courierRepository.existsByUserId(200L)).thenReturn(true);

            assertThat(courierService.existsByUserId(200L)).isTrue();
        }
    }
}
