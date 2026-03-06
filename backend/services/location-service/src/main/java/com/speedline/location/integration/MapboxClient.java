package com.speedline.location.integration;

import com.speedline.location.dto.ReverseGeocodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client Mapbox API pour le géocodage inverse
 */
@Component
@Slf4j
public class MapboxClient {

    private static final String MAPBOX_BASE_URL   = "https://api.mapbox.com";
    private static final String REVERSE_GEOCODE_PATH = "/geocoding/v5/mapbox.places/{lon},{lat}.json";

    // Types demandés du plus précis au moins précis
    private static final String MAPBOX_TYPES =
            "address,street,neighborhood,locality,place,district,region";

    private final WebClient webClient;
    private final String accessToken;

    public MapboxClient(
            WebClient.Builder webClientBuilder,
            @Value("${mapbox.access-token}") String accessToken) {
        this.webClient = webClientBuilder.baseUrl(MAPBOX_BASE_URL).build();
        this.accessToken = accessToken;
    }

    @SuppressWarnings("unchecked")
    public ReverseGeocodeResponse reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        log.debug("Mapbox reverse geocode: lat={}, lon={}", latitude, longitude);

        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(REVERSE_GEOCODE_PATH)
                            .queryParam("access_token", accessToken)
                            .queryParam("language", "fr")
                            .queryParam("types", MAPBOX_TYPES)
                            .queryParam("limit", 1)
                            .build(longitude.toString(), latitude.toString()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("Mapbox null response for lat={}, lon={}", latitude, longitude);
                return buildFallbackResponse(latitude, longitude);
            }

            List<Map<String, Object>> features =
                    (List<Map<String, Object>>) response.get("features");
            if (features == null || features.isEmpty()) {
                log.warn("No Mapbox features for lat={}, lon={}", latitude, longitude);
                return buildFallbackResponse(latitude, longitude);
            }

            return parseFeature(features.get(0), latitude, longitude);

        } catch (WebClientResponseException e) {
            log.error("Mapbox API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return buildFallbackResponse(latitude, longitude);
        } catch (Exception e) {
            log.error("Unexpected Mapbox error: {}", e.getMessage(), e);
            return buildFallbackResponse(latitude, longitude);
        }
    }

    @SuppressWarnings("unchecked")
    private ReverseGeocodeResponse parseFeature(
            Map<String, Object> feature, BigDecimal lat, BigDecimal lon) {

        String placeId = (String) feature.getOrDefault("id", "");
        double relevance = ((Number) feature.getOrDefault("relevance", 0.0)).doubleValue();

        // Determine the feature type (address / neighborhood / locality / place / district / region)
        String featureType = placeId.contains(".")
                ? placeId.substring(0, placeId.indexOf('.'))
                : "";

        String featureText   = (String) feature.getOrDefault("text", "");
        String addressNumber = (String) feature.getOrDefault("address", "");

        // ── Décider ce que représente featureText selon le type ──────────────
        String street     = "";
        String city       = "";
        String state      = "";
        String postalCode = "";
        String country    = "Tunisie";

        switch (featureType) {
            case "address":
            case "street":
                // featureText = nom de rue ; addressNumber = numéro
                street = addressNumber.isBlank()
                        ? featureText
                        : addressNumber + " " + featureText;
                break;
            case "neighborhood":
            case "locality":
            case "suburb":
                // featureText = quartier/banlieue → traiter comme info de rue
                street = featureText;
                break;
            case "place":
            case "district":
                // featureText = nom de ville
                city = featureText;
                break;
            case "region":
                // featureText = gouvernorat/région
                state = featureText;
                break;
            default:
                street = featureText;
        }

        // ── Enrichir avec le contexte (éléments parents) ────────────────────
        List<Map<String, Object>> context =
                (List<Map<String, Object>>) feature.get("context");
        if (context != null) {
            for (Map<String, Object> ctx : context) {
                String ctxId   = (String) ctx.getOrDefault("id", "");
                String ctxText = (String) ctx.getOrDefault("text", "");
                // extract the type prefix before the '.' (e.g. "place.abc" -> "place")
                String ctxType = ctxId.contains(".")
                        ? ctxId.substring(0, ctxId.indexOf('.'))
                        : ctxId;

                switch (ctxType) {
                    case "postcode" -> postalCode = ctxText;
                    case "locality", "place", "district" -> { if (city.isBlank()) city = ctxText; }
                    case "region" -> { if (state.isBlank()) state = ctxText; }
                    case "country" -> country = ctxText;
                    default -> { /* ignore unknown context types */ }
                }
            }
        }

        // ── Construire une adresse formatée lisible ──────────────────────────
        // Exemple: "Avenue Bourguiba, Sousse 4000, Tunisie"
        List<String> parts = new ArrayList<>();
        if (!street.isBlank()) parts.add(street);

        String cityPart = city;
        if (!postalCode.isBlank() && !cityPart.isBlank()) {
            cityPart = cityPart + " " + postalCode;
        } else if (!postalCode.isBlank()) {
            cityPart = postalCode;
        }
        if (!cityPart.isBlank()) parts.add(cityPart);
        if (!country.isBlank())  parts.add(country);

        String formattedAddress = String.join(", ", parts);

        // Si aucun composant exploitable, utiliser place_name de Mapbox
        if (formattedAddress.isBlank() || formattedAddress.equals(country)) {
            formattedAddress = (String) feature.getOrDefault("place_name", formattedAddress);
        }

        log.debug("Parsed: formatted='{}' street='{}' city='{}' state='{}' postal='{}'",
                formattedAddress, street, city, state, postalCode);

        return ReverseGeocodeResponse.builder()
                .formattedAddress(formattedAddress)
                .street(street)
                .city(city)
                .state(state)
                .postalCode(postalCode)
                .country(country)
                .latitude(lat)
                .longitude(lon)
                .placeId(placeId)
                .confidence(relevance)
                .build();
    }

    private ReverseGeocodeResponse buildFallbackResponse(BigDecimal latitude, BigDecimal longitude) {
        return ReverseGeocodeResponse.builder()
                .formattedAddress("")   // vide → le client Flutter utilisera Nominatim
                .latitude(latitude)
                .longitude(longitude)
                .country("Tunisie")
                .confidence(0.0)
                .build();
    }
}
