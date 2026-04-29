package com.speedline.location.service.impl;

import com.speedline.location.dto.ReverseGeocodeResponse;
import com.speedline.location.integration.MapboxClient;
import com.speedline.location.repository.ZoneRepository;
import com.speedline.location.service.GeolocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeolocationServiceImplTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private MapboxClient mapboxClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private GeolocationServiceImpl geolocationService;

    // ===================== CALCULATE DISTANCE =====================

    @Nested
    @DisplayName("calculateDistance")
    class CalculateDistance {

        @Test
        @DisplayName("should calculate distance between two points")
        void shouldCalculateDistance() {
            BigDecimal lat1 = new BigDecimal("36.8065");
            BigDecimal lon1 = new BigDecimal("10.1815");
            BigDecimal lat2 = new BigDecimal("36.8500");
            BigDecimal lon2 = new BigDecimal("10.2200");

            // Mock PostGIS returning ~5500 meters
            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class),
                    any(), any(), any(), any()))
                    .thenReturn(5500.0);

            GeolocationService.DistanceResult result =
                    geolocationService.calculateDistance(lat1, lon1, lat2, lon2);

            assertThat(result).isNotNull();
            assertThat(result.distanceKm()).isCloseTo(5.5, within(0.01));
            // 5.5 km / 30 km/h * 60 min = 11 min
            assertThat(result.durationMinutes()).isEqualTo(11);
        }

        @Test
        @DisplayName("should return 0 distance for same point")
        void shouldReturnZeroForSamePoint() {
            BigDecimal lat = new BigDecimal("36.8065");
            BigDecimal lon = new BigDecimal("10.1815");

            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class),
                    any(), any(), any(), any()))
                    .thenReturn(0.0);

            GeolocationService.DistanceResult result =
                    geolocationService.calculateDistance(lat, lon, lat, lon);

            assertThat(result.distanceKm()).isEqualTo(0.0);
            assertThat(result.durationMinutes()).isEqualTo(0);
        }

        @Test
        @DisplayName("should calculate correct duration for long distance")
        void shouldCalculateCorrectDurationForLongDistance() {
            BigDecimal lat1 = new BigDecimal("36.8065");
            BigDecimal lon1 = new BigDecimal("10.1815");
            BigDecimal lat2 = new BigDecimal("35.7643");
            BigDecimal lon2 = new BigDecimal("10.8113");

            // ~120 km
            when(jdbcTemplate.queryForObject(anyString(), eq(Double.class),
                    any(), any(), any(), any()))
                    .thenReturn(120000.0);

            GeolocationService.DistanceResult result =
                    geolocationService.calculateDistance(lat1, lon1, lat2, lon2);

            assertThat(result.distanceKm()).isCloseTo(120.0, within(0.01));
            // 120 km / 30 km/h * 60 = 240 min
            assertThat(result.durationMinutes()).isEqualTo(240);
        }
    }

    // ===================== REVERSE GEOCODE =====================

    @Nested
    @DisplayName("reverseGeocode")
    class ReverseGeocode {

        @Test
        @DisplayName("should delegate to MapboxClient")
        void shouldDelegateToMapbox() {
            BigDecimal lat = new BigDecimal("36.8065");
            BigDecimal lon = new BigDecimal("10.1815");

            ReverseGeocodeResponse expected = ReverseGeocodeResponse.builder()
                    .formattedAddress("Avenue Habib Bourguiba, Tunis 1000, Tunisie")
                    .street("Avenue Habib Bourguiba")
                    .city("Tunis")
                    .postalCode("1000")
                    .country("Tunisie")
                    .latitude(lat)
                    .longitude(lon)
                    .confidence(0.95)
                    .build();

            when(mapboxClient.reverseGeocode(lat, lon)).thenReturn(expected);

            ReverseGeocodeResponse result = geolocationService.reverseGeocode(lat, lon);

            assertThat(result).isNotNull();
            assertThat(result.getFormattedAddress()).isEqualTo("Avenue Habib Bourguiba, Tunis 1000, Tunisie");
            assertThat(result.getCity()).isEqualTo("Tunis");
            assertThat(result.getCountry()).isEqualTo("Tunisie");
            verify(mapboxClient).reverseGeocode(lat, lon);
        }

        @Test
        @DisplayName("should propagate MapboxClient exceptions")
        void shouldPropagateExceptions() {
            BigDecimal lat = new BigDecimal("36.8065");
            BigDecimal lon = new BigDecimal("10.1815");

            when(mapboxClient.reverseGeocode(lat, lon))
                    .thenThrow(new RuntimeException("Mapbox API error"));

            assertThatThrownBy(() -> geolocationService.reverseGeocode(lat, lon))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mapbox API error");
        }
    }

    // ===================== UNIMPLEMENTED METHODS =====================

    @Nested
    @DisplayName("Unimplemented methods")
    class UnimplementedMethods {

        @Test
        @DisplayName("isInDeliveryZone should throw UnsupportedOperationException")
        void isInDeliveryZoneShouldThrow() {
            assertThatThrownBy(() ->
                    geolocationService.isInDeliveryZone(BigDecimal.ONE, BigDecimal.ONE))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("geocodeAddress should throw UnsupportedOperationException")
        void geocodeAddressShouldThrow() {
            assertThatThrownBy(() ->
                    geolocationService.geocodeAddress("test address"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("calculateRoute should throw UnsupportedOperationException")
        void calculateRouteShouldThrow() {
            assertThatThrownBy(() ->
                    geolocationService.calculateRoute(
                            BigDecimal.ONE, BigDecimal.ONE,
                            BigDecimal.TEN, BigDecimal.TEN, "driving"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getZoneForLocation should throw UnsupportedOperationException")
        void getZoneForLocationShouldThrow() {
            assertThatThrownBy(() ->
                    geolocationService.getZoneForLocation(BigDecimal.ONE, BigDecimal.ONE))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
