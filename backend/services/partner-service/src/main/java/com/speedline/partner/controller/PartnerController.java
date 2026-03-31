package com.speedline.partner.controller;

import com.speedline.partner.dto.CompletePartnerProfileRequest;
import com.speedline.partner.dto.CreatePartnerRequest;
import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.dto.StaffMemberDTO;
import com.speedline.partner.dto.UpdatePartnerStatusRequest;
import com.speedline.partner.dto.request.FavoriteCreateRequest;
import com.speedline.partner.dto.request.PartnerFilterRequest;
import com.speedline.partner.dto.response.FavoriteUpsertResult;
import com.speedline.partner.service.FileStorageService;
import com.speedline.partner.service.FavoriteService;
import com.speedline.partner.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.speedline.partner.domain.Partner;
import com.speedline.partner.repository.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller pour Partner
 * 
 * Endpoints:
 * POST   /partners/internal - Créer profil partner initial (appelé par auth-service)
 * GET    /partners/by-user/{userId} - Récupérer partner par userId
 * GET    /partners/{id} - Détails partenaire
 * PUT    /partners/{id} - Modifier partenaire
 * PUT    /partners/{id}/complete-profile - Compléter le profil (Phase 2)
 * GET    /partners - Liste partenaires
 * PUT    /partners/{id}/status - Changer statut
 * GET    /partners/search - Recherche
 *
 * @see PartnerApi for OpenAPI/Swagger documentation
 */
@RestController
@RequestMapping("/partners")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Partners", description = "Gestion des établissements partenaires")
public class PartnerController implements PartnerApi {
    
    private final PartnerService partnerService;
    private final FavoriteService favoriteService;
    private final FileStorageService fileStorageService;
    private final PartnerRepository partnerRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/internal")
    public ResponseEntity<?> createPartnerInternal(@RequestBody CreatePartnerRequest request) {
        log.info("Internal endpoint called to create partner for userId: {}", request.getUserId());
        
        try {
            PartnerDTO partner = partnerService.createPartnerFromAuth(
                request.getUserId(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
            );
            
            log.info("Partner profile created successfully with id: {}", partner.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(partner);
        } catch (Exception e) {
            log.error("Failed to create partner profile for userId: {}. Error: {}", 
                request.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create partner profile: " + e.getMessage()));
        }
    }

    /**
     * Récupérer un partner par userId (pour le frontend après login)
     * GET /partners/by-user/{userId}
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getPartnerByUserId(@PathVariable Long userId) {
        log.info("Getting partner by userId: {}", userId);
        
        try {
            PartnerDTO partner = partnerService.getPartnerByUserId(userId);
            return ResponseEntity.ok(partner);
        } catch (Exception e) {
            log.error("Partner not found for userId: {}. Error: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Partner not found for userId: " + userId));
        }
    }

    /**
     * Récupérer un partner par ID
     * GET /partners/{id}
     */
    @Operation(summary = "Détail partenaire", description = "Retourne le PartnerDTO complet par id")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartnerById(@PathVariable Long id) {
        log.info("Getting partner by id: {}", id);
        
        try {
            PartnerDTO partner = partnerService.getPartnerById(id);
            return ResponseEntity.ok(partner);
        } catch (Exception e) {
            log.error("Partner not found with id: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Partner not found with id: " + id));
        }
    }

    @PutMapping("/{id}/complete-profile")
    public ResponseEntity<?> completeProfile(
            @PathVariable Long id,
            @RequestBody CompletePartnerProfileRequest request) {
        log.info("Completing profile for partner id: {}", id);
        log.debug("Request data: partnerType={}, businessName={}, address={}", 
            request.getPartnerType(), request.getBusinessName(), request.getAddress());
        
        try {
            PartnerDTO partner = partnerService.completeProfile(id, request);
            log.info("Partner profile completed successfully for id: {}", id);
            return ResponseEntity.ok(partner);
        } catch (Exception e) {
            log.error("Failed to complete profile for partner id: {}. Error: {}", 
                id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to complete profile: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartner(
            @PathVariable Long id,
            @RequestBody CompletePartnerProfileRequest request) {
        log.info("Updating partner id: {}", id);
        
        try {
            PartnerDTO partner = partnerService.completeProfile(id, request);
            return ResponseEntity.ok(partner);
        } catch (Exception e) {
            log.error("Failed to update partner id: {}. Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update partner: " + e.getMessage()));
        }
    }

    @Operation(summary = "Changer statut ouvert/fermé", description = "Met à jour acceptsOrders (visible dans l'app client). OWNER ou ADMIN.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdatePartnerStatusRequest request) {
        log.info("Updating open status for partner id: {}, isOpen={}", id, request.getIsOpen());
        try {
            boolean isOpen = request.getIsOpen() != null && request.getIsOpen();
            PartnerDTO partner = partnerService.updateOpenStatus(id, isOpen);
            return ResponseEntity.ok(partner);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Partner not found with id: " + id));
            }
            throw e;
        }
    }

    @Operation(summary = "Horaires d'ouverture", description = "Retourne la liste des horaires (format JSON stocké)")
    @GetMapping("/{id}/opening-hours")
    public ResponseEntity<?> getOpeningHours(@PathVariable Long id) {
        try {
            List<?> hours = partnerService.getOpeningHours(id);
            return ResponseEntity.ok(hours);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Partner not found with id: " + id));
            }
            throw e;
        }
    }

    @Operation(summary = "Modifier horaires", description = "Met à jour les horaires (body = liste day/isClosed/slots)")
    @PutMapping("/{id}/opening-hours")
    public ResponseEntity<?> putOpeningHours(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            partnerService.updateOpeningHours(id, json);
            List<?> hours = partnerService.getOpeningHours(id);
            return ResponseEntity.ok(hours);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Partner not found with id: " + id));
            }
            throw e;
        } catch (Exception e) {
            log.error("Failed to update opening hours for partner {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid opening hours format: " + e.getMessage()));
        }
    }

    @Operation(summary = "Liste du staff", description = "Retourne les membres du staff (OWNER, MANAGER, STAFF)")
    @GetMapping("/{id}/staff")
    public ResponseEntity<?> getStaff(@PathVariable Long id) {
        try {
            List<StaffMemberDTO> staff = partnerService.getStaff(id);
            return ResponseEntity.ok(staff);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Partner not found with id: " + id));
            }
            throw e;
        }
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<?> uploadImages(
            @PathVariable Long id,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "photos", required = false) MultipartFile[] photos) {
        log.info("Uploading images for partner id: {}", id);
        
        try {
            Map<String, Object> urls = new HashMap<>();
            
            if (logo != null && !logo.isEmpty()) {
                String logoUrl = fileStorageService.storeImage(logo, id, "logos");
                partnerService.updateImages(id, logoUrl, null);
                urls.put("logo", logoUrl);
                log.info("Logo uploaded for partner {}: {}", id, logoUrl);
            }
            
            if (cover != null && !cover.isEmpty()) {
                String coverUrl = fileStorageService.storeImage(cover, id, "covers");
                partnerService.updateImages(id, null, coverUrl);
                urls.put("cover", coverUrl);
                log.info("Cover uploaded for partner {}: {}", id, coverUrl);
            }
            
            if (photos != null && photos.length > 0) {
                List<String> photoUrls = new ArrayList<>();
                for (MultipartFile photo : photos) {
                    if (!photo.isEmpty()) {
                        String photoUrl = fileStorageService.storeImage(photo, id, "photos");
                        photoUrls.add(photoUrl);
                    }
                }
                // Persist photo URLs in partner entity
                Partner partner = partnerRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Partner not found: " + id));
                try {
                    partner.setPhotosJson(objectMapper.writeValueAsString(photoUrls));
                    partnerRepository.save(partner);
                } catch (Exception e) {
                    log.warn("Failed to serialize photo URLs for partner {}: {}", id, e.getMessage());
                }
                urls.put("photos", photoUrls);
                log.info("Photos uploaded for partner {}: {} files", id, photoUrls.size());
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Images uploaded successfully",
                "urls", urls
            ));
        } catch (Exception e) {
            log.error("Failed to upload images for partner {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload images: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<?> uploadDocuments(
            @PathVariable Long id,
            @RequestParam(value = "kbis", required = false) MultipartFile kbis,
            @RequestParam(value = "idCard", required = false) MultipartFile idCard,
            @RequestParam(value = "insurance", required = false) MultipartFile insurance,
            @RequestParam(value = "rib", required = false) MultipartFile rib) {
        log.info("Uploading documents for partner id: {}", id);
        
        try {
            Map<String, String> urls = new HashMap<>();
            
            if (kbis != null && !kbis.isEmpty()) {
                urls.put("kbis", fileStorageService.storeDocument(kbis, id, "kbis"));
            }
            if (idCard != null && !idCard.isEmpty()) {
                urls.put("idCard", fileStorageService.storeDocument(idCard, id, "idCard"));
            }
            if (insurance != null && !insurance.isEmpty()) {
                urls.put("insurance", fileStorageService.storeDocument(insurance, id, "insurance"));
            }
            if (rib != null && !rib.isEmpty()) {
                urls.put("rib", fileStorageService.storeDocument(rib, id, "rib"));
            }
            
            // Persist document URLs in partner entity
            if (!urls.isEmpty()) {
                Partner partner = partnerRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Partner not found: " + id));
                if (urls.containsKey("kbis")) partner.setKbisUrl(urls.get("kbis"));
                if (urls.containsKey("idCard")) partner.setIdCardUrl(urls.get("idCard"));
                if (urls.containsKey("insurance")) partner.setInsuranceUrl(urls.get("insurance"));
                if (urls.containsKey("rib")) partner.setRibUrl(urls.get("rib"));
                partnerRepository.save(partner);
                log.info("Document URLs persisted for partner {}", id);
            }
            
            log.info("Documents uploaded for partner {}: {}", id, urls.keySet());
            return ResponseEntity.ok(Map.of(
                "message", "Documents uploaded successfully",
                "urls", urls
            ));
        } catch (Exception e) {
            log.error("Failed to upload documents for partner {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload documents: " + e.getMessage()));
        }
    }

    /**
     * POST /partners/{id}/upload/logo
     * TC-19 : Upload logo valide → HTTP 200, logoUrl retournée.
     */
    @Operation(summary = "Upload logo partenaire",
               description = "multipart/form-data, champ 'file', max 5 Mo, JPG/PNG/WEBP")
    @PostMapping(value = "/{id}/upload/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("uploadLogo partnerId={} size={}", id, file != null ? file.getSize() : 0);
        String logoUrl = fileStorageService.storeImage(file, id, "logos");
        partnerService.updateImages(id, logoUrl, null);
        return ResponseEntity.ok(Map.of("url", logoUrl, "logoUrl", logoUrl));
    }

    /**
     * POST /partners/{id}/upload/cover
     * Upload image de couverture.
     */
    @Operation(summary = "Upload cover partenaire",
               description = "multipart/form-data, champ 'file', max 5 Mo, JPG/PNG/WEBP")
    @PostMapping(value = "/{id}/upload/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("uploadCover partnerId={} size={}", id, file != null ? file.getSize() : 0);
        String coverUrl = fileStorageService.storeImage(file, id, "covers");
        partnerService.updateImages(id, null, coverUrl);
        return ResponseEntity.ok(Map.of("url", coverUrl, "coverUrl", coverUrl));
    }

    /**
     * GET /partners/nearby?lat=..&lng=..&page=..&size=..
     * Récupérer les partenaires à proximité d'une position GPS, avec pagination.
     */
    @GetMapping("/nearby")
    public ResponseEntity<Page<PartnerDTO>> getNearbyPartners(
            @RequestParam @NotNull
            @DecimalMin(value = "-90.0",  message = "Latitude invalide : doit être entre -90 et 90")
            @DecimalMax(value = "90.0",   message = "Latitude invalide : doit être entre -90 et 90") BigDecimal lat,
            @RequestParam @NotNull
            @DecimalMin(value = "-180.0", message = "Longitude invalide : doit être entre -180 et 180")
            @DecimalMax(value = "180.0",  message = "Longitude invalide : doit être entre -180 et 180") BigDecimal lng,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Boolean isOpen,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer maxDeliveryTime,
            @RequestParam(required = false) Boolean freeDelivery,
            @RequestParam(defaultValue = "distance") String sortBy) {

        PartnerFilterRequest filterRequest = PartnerFilterRequest.builder()
                .lat(lat)
                .lng(lng)
                .page(page)
                .size(size)
                .isOpen(isOpen)
                .categoryId(categoryId)
                .minRating(minRating)
                .maxDeliveryTime(maxDeliveryTime)
                .freeDelivery(freeDelivery)
                .sortBy(sortBy)
                .build();

        return ResponseEntity.ok(partnerService.getNearbyPartners(filterRequest));
    }

    /**
     * GET /partners/search?query=...&lat=...&lng=...&page=...&size=...
     * Recherche textuelle des partenaires dans la zone à proximité du client.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PartnerDTO>> searchPartners(
            @RequestParam @NotNull String query,
            @RequestParam @NotNull
            @DecimalMin(value = "-90.0", message = "Latitude invalide : doit être entre -90 et 90")
            @DecimalMax(value = "90.0", message = "Latitude invalide : doit être entre -90 et 90") BigDecimal lat,
            @RequestParam @NotNull
            @DecimalMin(value = "-180.0", message = "Longitude invalide : doit être entre -180 et 180")
            @DecimalMax(value = "180.0", message = "Longitude invalide : doit être entre -180 et 180") BigDecimal lng,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        final String normalizedQuery = query.trim();
        if (normalizedQuery.length() < 2) {
            return ResponseEntity.ok(new PageImpl<>(List.of(), PageRequest.of(page, size), 0));
        }

        // Pull a wider nearby window, then rank/filter by query.
        final int candidateSize = Math.min(Math.max(size * 4, 60), 200);
        PartnerFilterRequest filterRequest = PartnerFilterRequest.builder()
                .lat(lat)
                .lng(lng)
                .page(0)
                .size(candidateSize)
                .sortBy("distance")
                .build();

        final List<PartnerDTO> candidates = partnerService.getNearbyPartners(filterRequest).getContent();

        final List<PartnerDTO> matched = candidates.stream()
                .filter(p -> matchesQuery(p, normalizedQuery))
                .sorted(Comparator
                        .comparingInt((PartnerDTO p) -> relevanceScore(p, normalizedQuery))
                        .thenComparing(p -> p.getDistanceKm() != null ? p.getDistanceKm() : Double.MAX_VALUE)
                        .thenComparing(p -> p.getRating() != null ? p.getRating() : BigDecimal.ZERO, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        final int fromIndex = Math.min(page * size, matched.size());
        final int toIndex = Math.min(fromIndex + size, matched.size());
        final List<PartnerDTO> slice = matched.subList(fromIndex, toIndex);

        return ResponseEntity.ok(new PageImpl<>(slice, PageRequest.of(page, size), matched.size()));
    }

    /**
     * GET /partners/search/trending?lat=...&lng=...&limit=5
     * Renvoie des termes tendances (noms de partenaires) selon la zone utilisateur.
     */
    @GetMapping("/search/trending")
    public ResponseEntity<List<String>> getTrendingSearches(
            @RequestParam @NotNull
            @DecimalMin(value = "-90.0", message = "Latitude invalide : doit être entre -90 et 90")
            @DecimalMax(value = "90.0", message = "Latitude invalide : doit être entre -90 et 90") BigDecimal lat,
            @RequestParam @NotNull
            @DecimalMin(value = "-180.0", message = "Longitude invalide : doit être entre -180 et 180")
            @DecimalMax(value = "180.0", message = "Longitude invalide : doit être entre -180 et 180") BigDecimal lng,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {

        PartnerFilterRequest filterRequest = PartnerFilterRequest.builder()
                .lat(lat)
                .lng(lng)
                .page(0)
                .size(Math.max(limit * 5, 25))
                .sortBy("popularity")
                .build();

        final List<String> trending = partnerService.getNearbyPartners(filterRequest).getContent().stream()
                .map(this::displayName)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(trending);
    }

    @Override
    @GetMapping("/favorites")
    public ResponseEntity<?> getFavorites(
            @RequestParam("userId") Long userId,
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId) {

        final Long customerId = parseAuthenticatedUserId(authenticatedUserId);
        if (customerId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT missing or invalid");
        }
        if (!customerId.equals(userId)) {
            return errorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only access your own favorites");
        }

        return ResponseEntity.ok(favoriteService.getFavorites(customerId));
    }

    @Override
    @PostMapping(path = "/favorites", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addFavorite(
            @RequestBody FavoriteCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId) {

        final Long customerId = parseAuthenticatedUserId(authenticatedUserId);
        if (customerId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT missing or invalid");
        }

        final String partnerIdRaw = request != null ? request.getPartnerId() : null;
        final Long partnerId = parsePartnerId(partnerIdRaw);
        if (partnerId == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "partnerId must be a numeric identifier");
        }

        final FavoriteUpsertResult result = favoriteService.addFavorite(customerId, partnerId);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.favorite());
        }
        return ResponseEntity.ok(result.favorite());
    }

    @Override
    @DeleteMapping("/favorites/{partnerId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable String partnerId,
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId) {

        final Long customerId = parseAuthenticatedUserId(authenticatedUserId);
        if (customerId == null) {
            return errorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "JWT missing or invalid");
        }

        final Long partnerIdValue = parsePartnerId(partnerId);
        if (partnerIdValue == null) {
            return errorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "partnerId must be a numeric identifier");
        }

        favoriteService.removeFavorite(customerId, partnerIdValue);
        return ResponseEntity.noContent().build();
    }

    private Long parseAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(authenticatedUserId.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parsePartnerId(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(partnerId.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String error, String message) {
        final Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private boolean matchesQuery(PartnerDTO partner, String query) {
        final String q = query.toLowerCase(Locale.ROOT);
        final String business = safeLower(partner.getBusinessName());
        final String brand = safeLower(partner.getBrandName());
        final String type = partner.getType() != null ? partner.getType().name().toLowerCase(Locale.ROOT) : "";
        final String description = safeLower(partner.getDescription());

        return business.contains(q) || brand.contains(q) || type.contains(q) || description.contains(q);
    }

    private int relevanceScore(PartnerDTO partner, String query) {
        final String q = query.toLowerCase(Locale.ROOT);
        final String brand = safeLower(partner.getBrandName());
        final String business = safeLower(partner.getBusinessName());

        if (brand.startsWith(q) || business.startsWith(q)) return 0;
        if (brand.contains(q) || business.contains(q)) return 1;
        if (safeLower(partner.getDescription()).contains(q)) return 2;
        return 3;
    }

    private String displayName(PartnerDTO partner) {
        if (partner.getBrandName() != null && !partner.getBrandName().isBlank()) {
            return partner.getBrandName();
        }
        return partner.getBusinessName() != null ? partner.getBusinessName() : "";
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

/**
     * Health check
     * GET /partners/health
     */

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "partner-service"));
    }
}
