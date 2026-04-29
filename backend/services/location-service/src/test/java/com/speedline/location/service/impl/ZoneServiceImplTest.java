package com.speedline.location.service.impl;

import com.speedline.location.client.UserServiceClient;
import com.speedline.location.domain.Zone;
import com.speedline.location.domain.Zone.ZoneType;
import com.speedline.location.dto.ZoneCourierCountDTO;
import com.speedline.location.dto.ZoneDTO;
import com.speedline.location.dto.ZoneInternalCourierAssignmentDTO;
import com.speedline.location.exception.ZoneNotFoundException;
import com.speedline.location.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneServiceImplTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ZoneServiceImpl zoneService;

    private Zone testZone;

    @BeforeEach
    void setUp() {
        testZone = Zone.builder()
                .id(1L)
                .name("Centre Ville Tunis")
                .description("Zone du centre de Tunis")
                .city("Tunis")
                .type(ZoneType.DELIVERY)
                .boundaryJson("{\"type\":\"Polygon\",\"coordinates\":[[[10.15,36.78],[10.20,36.78],[10.20,36.82],[10.15,36.82],[10.15,36.78]]]}")
                .deliveryFee(new BigDecimal("3.50"))
                .serviceFee(new BigDecimal("0.50"))
                .minDeliveryTime(20)
                .maxDeliveryTime(45)
                .radiusKm(5)
                .isActive(true)
                .internalCourierAssignments(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ===================== GET ZONE BY ID =====================

    @Nested
    @DisplayName("getZoneById")
    class GetZoneById {

        @Test
        @DisplayName("should return zone when found")
        void shouldReturnZone() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(new BigDecimal("12.5"));
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{36.80, 10.175});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(new BigDecimal("14.2"));
            when(userServiceClient.getZoneCourierCounts(anyList())).thenReturn(Collections.emptyList());

            ZoneDTO result = zoneService.getZoneById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Centre Ville Tunis");
            assertThat(result.getCity()).isEqualTo("Tunis");
            assertThat(result.getType()).isEqualTo(ZoneType.DELIVERY);
            assertThat(result.getDeliveryFee()).isEqualTo(new BigDecimal("3.50"));
        }

        @Test
        @DisplayName("should throw ZoneNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> zoneService.getZoneById(999L))
                    .isInstanceOf(ZoneNotFoundException.class);
        }

        @Test
        @DisplayName("should return zone even if geometry calculation fails")
        void shouldReturnZoneEvenIfGeometryFails() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
            when(zoneRepository.calculateAreaKm2(anyString())).thenThrow(new RuntimeException("PostGIS error"));
            when(userServiceClient.getZoneCourierCounts(anyList())).thenReturn(Collections.emptyList());

            ZoneDTO result = zoneService.getZoneById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            // Geometry defaults to zero on error
            assertThat(result.getAreaKm2()).isEqualTo(BigDecimal.ZERO);
        }
    }

    // ===================== SET ACTIVE STATUS =====================

    @Nested
    @DisplayName("setActiveStatus")
    class SetActiveStatus {

        @Test
        @DisplayName("should deactivate zone")
        void shouldDeactivateZone() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
            when(zoneRepository.save(any(Zone.class))).thenReturn(testZone);
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(BigDecimal.ZERO);
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{0.0, 0.0});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(BigDecimal.ZERO);

            zoneService.setActiveStatus(1L, false);

            verify(zoneRepository).save(argThat(z -> !z.getIsActive()));
        }

        @Test
        @DisplayName("should activate zone")
        void shouldActivateZone() {
            testZone.setIsActive(false);
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
            when(zoneRepository.save(any(Zone.class))).thenReturn(testZone);
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(BigDecimal.ZERO);
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{0.0, 0.0});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(BigDecimal.ZERO);

            zoneService.setActiveStatus(1L, true);

            verify(zoneRepository).save(argThat(z -> z.getIsActive()));
        }

        @Test
        @DisplayName("should throw ZoneNotFoundException for nonexistent zone")
        void shouldThrowForNonexistent() {
            when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> zoneService.setActiveStatus(999L, true))
                    .isInstanceOf(ZoneNotFoundException.class);
        }
    }

    // ===================== DELETE ZONE =====================

    @Nested
    @DisplayName("deleteZone")
    class DeleteZone {

        @Test
        @DisplayName("should delete existing zone")
        void shouldDeleteZone() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));

            zoneService.deleteZone(1L);

            verify(zoneRepository).delete(testZone);
        }

        @Test
        @DisplayName("should throw ZoneNotFoundException when zone does not exist")
        void shouldThrowWhenNotExist() {
            when(zoneRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> zoneService.deleteZone(999L))
                    .isInstanceOf(ZoneNotFoundException.class);
        }
    }

    // ===================== GET ACTIVE ZONES =====================

    @Nested
    @DisplayName("getActiveZones")
    class GetActiveZones {

        @Test
        @DisplayName("should return all active zones")
        void shouldReturnActiveZones() {
            when(zoneRepository.findByIsActiveTrue()).thenReturn(List.of(testZone));

            List<ZoneDTO> result = zoneService.getActiveZones();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Centre Ville Tunis");
        }

        @Test
        @DisplayName("should skip zones with invalid boundary gracefully")
        void shouldSkipInvalidBoundary() {
            Zone badZone = Zone.builder()
                    .id(2L)
                    .name("Bad Zone")
                    .boundaryJson(null)
                    .isActive(true)
                    .internalCourierAssignments(new ArrayList<>())
                    .build();

            when(zoneRepository.findByIsActiveTrue()).thenReturn(List.of(testZone, badZone));

            List<ZoneDTO> result = zoneService.getActiveZones();

            assertThat(result.size()).isGreaterThanOrEqualTo(1);
        }
    }

    // ===================== GET ALL ZONES (PAGINATED) =====================

    @Nested
    @DisplayName("getAllZones")
    class GetAllZones {

        @Test
        @DisplayName("should return all zones without filters")
        void shouldReturnAllZonesWithoutFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Zone> page = new PageImpl<>(List.of(testZone), pageable, 1);

            when(zoneRepository.findAll(pageable)).thenReturn(page);
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(BigDecimal.ZERO);
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{0.0, 0.0});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(BigDecimal.ZERO);
            when(userServiceClient.getZoneCourierCounts(anyList())).thenReturn(Collections.emptyList());

            Page<ZoneDTO> result = zoneService.getAllZones(pageable, null, null);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Centre Ville Tunis");
        }

        @Test
        @DisplayName("should filter by active status")
        void shouldFilterByActive() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Zone> page = new PageImpl<>(List.of(testZone), pageable, 1);

            when(zoneRepository.findByIsActiveTrue(pageable)).thenReturn(page);
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(BigDecimal.ZERO);
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{0.0, 0.0});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(BigDecimal.ZERO);
            when(userServiceClient.getZoneCourierCounts(anyList())).thenReturn(Collections.emptyList());

            Page<ZoneDTO> result = zoneService.getAllZones(pageable, null, true);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by search term")
        void shouldFilterBySearch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Zone> page = new PageImpl<>(List.of(testZone), pageable, 1);

            when(zoneRepository.searchByNameOrCity("Tunis", pageable)).thenReturn(page);
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(BigDecimal.ZERO);
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{0.0, 0.0});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(BigDecimal.ZERO);
            when(userServiceClient.getZoneCourierCounts(anyList())).thenReturn(Collections.emptyList());

            Page<ZoneDTO> result = zoneService.getAllZones(pageable, "Tunis", null);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // ===================== FIND ZONE FOR POINT =====================

    @Nested
    @DisplayName("findZoneForPoint")
    class FindZoneForPoint {

        @Test
        @DisplayName("should return zone containing point")
        void shouldReturnZoneForPoint() {
            BigDecimal lat = new BigDecimal("36.80");
            BigDecimal lon = new BigDecimal("10.18");
            when(zoneRepository.findZoneForPoint(lat, lon)).thenReturn(Optional.of(testZone));
            when(zoneRepository.calculateAreaKm2(anyString())).thenReturn(BigDecimal.ZERO);
            when(zoneRepository.calculateCenter(anyString())).thenReturn(new Double[]{0.0, 0.0});
            when(zoneRepository.calculatePerimeterKm(anyString())).thenReturn(BigDecimal.ZERO);

            ZoneDTO result = zoneService.findZoneForPoint(lat, lon);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Centre Ville Tunis");
        }

        @Test
        @DisplayName("should return null when no zone contains point")
        void shouldReturnNullWhenOutside() {
            BigDecimal lat = new BigDecimal("0.00");
            BigDecimal lon = new BigDecimal("0.00");
            when(zoneRepository.findZoneForPoint(lat, lon)).thenReturn(Optional.empty());

            ZoneDTO result = zoneService.findZoneForPoint(lat, lon);

            assertThat(result).isNull();
        }
    }

    // ===================== IS POINT IN ZONE =====================

    @Nested
    @DisplayName("isPointInZone")
    class IsPointInZone {

        @Test
        @DisplayName("should return true when point is in zone")
        void shouldReturnTrueWhenInZone() {
            BigDecimal lat = new BigDecimal("36.80");
            BigDecimal lon = new BigDecimal("10.18");
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
            when(zoneRepository.findZonesContainingPoint(lat, lon)).thenReturn(List.of(testZone));

            boolean result = zoneService.isPointInZone(1L, lat, lon);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when point is not in zone")
        void shouldReturnFalseWhenNotInZone() {
            BigDecimal lat = new BigDecimal("36.80");
            BigDecimal lon = new BigDecimal("10.18");
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(testZone));
            when(zoneRepository.findZonesContainingPoint(lat, lon)).thenReturn(Collections.emptyList());

            boolean result = zoneService.isPointInZone(1L, lat, lon);

            assertThat(result).isFalse();
        }
    }

    // ===================== DELIVERY FEE FOR POINT =====================

    @Nested
    @DisplayName("getDeliveryFeeForPoint")
    class GetDeliveryFeeForPoint {

        @Test
        @DisplayName("should return delivery fee when zone exists")
        void shouldReturnFee() {
            BigDecimal lat = new BigDecimal("36.80");
            BigDecimal lon = new BigDecimal("10.18");
            when(zoneRepository.findZoneForPoint(lat, lon)).thenReturn(Optional.of(testZone));

            BigDecimal fee = zoneService.getDeliveryFeeForPoint(lat, lon);

            assertThat(fee).isEqualTo(new BigDecimal("3.50"));
        }

        @Test
        @DisplayName("should return null when no zone covers point")
        void shouldReturnNullWhenNoZone() {
            BigDecimal lat = new BigDecimal("0.00");
            BigDecimal lon = new BigDecimal("0.00");
            when(zoneRepository.findZoneForPoint(lat, lon)).thenReturn(Optional.empty());

            BigDecimal fee = zoneService.getDeliveryFeeForPoint(lat, lon);

            assertThat(fee).isNull();
        }
    }

    // ===================== SYNC COURIER ZONES =====================

    @Nested
    @DisplayName("syncCourierZones")
    class SyncCourierZones {

        @Test
        @DisplayName("should do nothing when courierId is null")
        void shouldDoNothingWhenCourierIdNull() {
            zoneService.syncCourierZones(null, List.of(1L));

            verify(zoneRepository, never()).findAll();
        }

        @Test
        @DisplayName("should add courier assignment to target zones")
        void shouldAddCourierToZones() {
            testZone.setInternalCourierAssignments(new ArrayList<>());
            when(zoneRepository.findAll()).thenReturn(List.of(testZone));
            when(zoneRepository.saveAll(anyList())).thenReturn(List.of(testZone));

            zoneService.syncCourierZones(50L, List.of(1L));

            verify(zoneRepository).saveAll(anyList());
        }
    }
}
