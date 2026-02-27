package com.speedline.location.controller;

import com.speedline.location.dto.GeocodeRequest;
import com.speedline.location.dto.ReverseGeocodeResponse;
import com.speedline.location.dto.SaveAddressRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * API contract for Location operations. This interface contains the
 * request/response signatures and OpenAPI/Swagger annotations so the
 * documentation is generated from the contract rather than the controller
 * implementation.
 */
@RequestMapping(path = "/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Location", description = "Géolocalisation, géocodage et calcul de distances")
public interface LocationApi {

    @PostMapping(path = "/reverse-geocode", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Géocodage inverse",
            description = "Convertit des coordonnées GPS en adresse textuelle via Mapbox",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Adresse retournée",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ReverseGeocodeResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Requête invalide"),
                    @ApiResponse(responseCode = "500", description = "Erreur serveur")
            }
    )
    ResponseEntity<ReverseGeocodeResponse> reverseGeocode(
            @Parameter(description = "Latitude et longitude GPS", required = true)
            @Valid @RequestBody GeocodeRequest request
    );

    @GetMapping(path = "/nearby-partners")
    @Operation(summary = "Partenaires proches",
            description = "Retourne une liste de partenaires (restaurants/livraison) proches d'une position donnée")
    ResponseEntity<List<Map<String, Object>>> getNearbyPartners(
            @Parameter(description = "Latitude du centre", required = true) @RequestParam("lat") BigDecimal latitude,
            @Parameter(description = "Longitude du centre", required = true) @RequestParam("lon") BigDecimal longitude,
            @Parameter(description = "Rayon en mètres (optionnel)") @RequestParam(value = "radius", required = false) Integer radiusMeters
    );

    @PostMapping(path = "/calculate-distance", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calculer distance",
            description = "Calcule la distance et durée estimée entre deux points")
    ResponseEntity<Map<String, Object>> calculateDistance(
            @Valid @RequestBody Map<String, Object> payload
    );

    @GetMapping(path = "/route")
    @Operation(summary = "Calculer itinéraire",
            description = "Retourne l'itinéraire (polyline/GeoJSON) entre deux points ou plusieurs étapes")
    ResponseEntity<Map<String, Object>> getRoute(
            @RequestParam("fromLat") BigDecimal fromLat,
            @RequestParam("fromLon") BigDecimal fromLon,
            @RequestParam("toLat") BigDecimal toLat,
            @RequestParam("toLon") BigDecimal toLon
    );

    @PostMapping(path = "/customer-address", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Sauvegarder adresse client",
            description = "Persiste l'adresse GPS ou manuelle confirmée par le client dans la base de données"
    )
    ResponseEntity<Map<String, Object>> saveCustomerAddress(
            @RequestBody SaveAddressRequest request
    );
}
