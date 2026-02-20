package com.speedline.partner.controller;

import com.speedline.partner.dto.CompletePartnerProfileRequest;
import com.speedline.partner.dto.CreatePartnerRequest;
import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.service.FileStorageService;
import com.speedline.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.speedline.partner.domain.Partner;
import com.speedline.partner.repository.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 */
@RestController
@RequestMapping("/partners")
@RequiredArgsConstructor
@Slf4j
public class PartnerController {
    
    private final PartnerService partnerService;
    private final FileStorageService fileStorageService;
    private final PartnerRepository partnerRepository;
    private final ObjectMapper objectMapper;

    /**
     * Endpoint INTERNE: Créer un profil partner initial lors de l'inscription
     * Appelé par auth-service via Feign Client
     * POST /partners/internal
     */
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

    /**
     * Compléter le profil partner (Phase 2 - après login)
     * PUT /partners/{id}/complete-profile
     */
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

    /**
     * Mettre à jour un partner
     * PUT /partners/{id}
     */
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

    /**
     * Upload images (logo, cover, photos) for a partner
     * POST /partners/{id}/images
     * Accepts multipart/form-data with optional fields: logo, cover, photos (multiple)
     */
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

    /**
     * Upload documents (kbis, idCard, insurance, rib) for a partner
     * POST /partners/{id}/documents
     */
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
     * Health check
     * GET /partners/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "partner-service"));
    }
}
