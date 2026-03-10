package com.speedline.partner.service.impl;

import com.speedline.partner.domain.Partner;
import com.speedline.partner.domain.PartnerStatus;
import com.speedline.partner.domain.PartnerType;
import com.speedline.partner.dto.CompletePartnerProfileRequest;
import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.event.PartnerEvent;
import com.speedline.partner.event.PartnerEventPublisher;
import com.speedline.partner.domain.StaffMember;
import com.speedline.partner.domain.StaffRole;
import com.speedline.partner.dto.StaffMemberDTO;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.repository.StaffMemberRepository;
import com.speedline.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des partenaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final PartnerEventPublisher partnerEventPublisher;
    private final com.speedline.partner.client.AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${nearby.default-radius-km:5.0}")
    private double defaultRadiusKm;

    // ==================== SYNC AUTH-SERVICE ====================

    @Override
    @Transactional
    public PartnerDTO createPartnerFromAuth(Long userId, String email, String firstName, 
                                           String lastName, String phoneNumber) {
        log.info("Creating partner profile from auth-service for userId: {}", userId);
        
        // Vérifier si l'utilisateur a déjà un partner
        Optional<Partner> existing = partnerRepository.findByUserId(userId);
        if (existing.isPresent()) {
            log.warn("Partner profile already exists for userId: {}", userId);
            return convertToDTO(existing.get());
        }
        
        // Créer un profil partner minimal
        Partner partner = Partner.builder()
                .userId(userId)
                .email(email)
                .phoneNumber(phoneNumber)
                .businessName(firstName + " " + lastName) // Temporaire
                .status(PartnerStatus.PENDING)
                .isActive(false)
                .acceptsOrders(false)
                .isVerified(false)
                .isPremium(false)
                .isFeatured(false)
                .country("Tunisie")
                .build();
        
        partner = partnerRepository.save(partner);
        log.info("Partner profile created successfully with id: {} for userId: {}", 
                partner.getId(), userId);

        StaffMember owner = StaffMember.builder()
                .partnerId(partner.getId())
                .userId(userId)
                .role(StaffRole.OWNER)
                .build();
        staffMemberRepository.save(owner);
        log.info("StaffMember OWNER created for partner {} userId {}", partner.getId(), userId);
        
        return convertToDTO(partner);
    }

    @Override
    @Transactional
    public PartnerDTO completeProfile(Long partnerId, CompletePartnerProfileRequest request) {
        log.info("Completing profile for partner id: {}", partnerId);
        
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        
        // ======== Business Information ========
        if (request.getBusinessName() != null) {
            partner.setBusinessName(request.getBusinessName());
        }
        if (request.getBrandName() != null) {
            partner.setBrandName(request.getBrandName());
        }
        if (request.getPartnerType() != null) {
            partner.setType(request.getPartnerType());
        }
        if (request.getDescription() != null) {
            partner.setDescription(request.getDescription());
        }
        if (request.getShortDescription() != null) {
            partner.setShortDescription(request.getShortDescription());
        }
        if (request.getPhoneNumber() != null) {
            partner.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            partner.setEmail(request.getEmail());
        }

        // ======== Address ========
        if (request.getAddress() != null) {
            partner.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            partner.setCity(request.getCity());
        }
        if (request.getPostalCode() != null) {
            partner.setPostalCode(request.getPostalCode());
        }
        if (request.getState() != null) {
            partner.setState(request.getState());
        }
        if (request.getCountry() != null) {
            partner.setCountry(request.getCountry());
        }
        if (request.getLatitude() != null) {
            partner.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            partner.setLongitude(request.getLongitude());
        }
        
        // ======== Legal Information ========
        if (request.getLegalStatus() != null) {
            partner.setLegalStatus(request.getLegalStatus());
        }
        if (request.getTva() != null) {
            partner.setTva(request.getTva());
        }
        if (request.getLegalRepFirstName() != null) {
            partner.setLegalRepFirstName(request.getLegalRepFirstName());
        }
        if (request.getLegalRepLastName() != null) {
            partner.setLegalRepLastName(request.getLegalRepLastName());
        }
        if (request.getPosition() != null) {
            partner.setPosition(request.getPosition());
        }
        
        // ======== Bank Information ========
        if (request.getAccountHolderName() != null) {
            partner.setAccountHolderName(request.getAccountHolderName());
        }
        if (request.getIban() != null) {
            partner.setIban(request.getIban());
        }
        if (request.getBankName() != null) {
            partner.setBankName(request.getBankName());
        }
        if (request.getCurrency() != null) {
            partner.setCurrency(request.getCurrency());
        }
        
        // ======== Operational Configuration ========
        if (request.getPreparationTime() != null) {
            partner.setPreparationTime(request.getPreparationTime());
        }
        if (request.getMinimumOrder() != null) {
            // Handle noMinimum flag
            if (request.getNoMinimum() != null && request.getNoMinimum()) {
                partner.setMinimumOrder(BigDecimal.ZERO);
            } else {
                partner.setMinimumOrder(request.getMinimumOrder());
            }
        }
        if (request.getOpeningHoursJson() != null) {
            partner.setOpeningHoursJson(request.getOpeningHoursJson());
        }
        if (request.getScheduleExceptionsJson() != null) {
            partner.setScheduleExceptionsJson(request.getScheduleExceptionsJson());
        }
        if (request.getAcceptOnlinePayment() != null) {
            partner.setAcceptOnlinePayment(request.getAcceptOnlinePayment());
        }
        if (request.getAcceptCashPayment() != null) {
            partner.setAcceptCashPayment(request.getAcceptCashPayment());
        }
        
        // ======== Presentation ========
        if (request.getFullDescription() != null) {
            partner.setDescription(request.getFullDescription());
        }
        if (request.getTags() != null) {
            partner.setTags(request.getTags());
        }
        
        // Gérer le statut selon le cas :
        // - Si statut null : mettre PENDING (première soumission)
        // - Si DOCUMENTS_MISSING : remettre PENDING pour réexamen après correction
        // - Si ACTIVE, INACTIVE, SUSPENDED, REJECTED : conserver le statut (mise à jour simple)
        PartnerStatus currentStatus = partner.getStatus();
        if (currentStatus == null) {
            partner.setStatus(PartnerStatus.PENDING);
        } else if (currentStatus == PartnerStatus.DOCUMENTS_MISSING) {
            // Remettre en PENDING pour réexamen après correction des documents/informations
            partner.setStatus(PartnerStatus.PENDING);
            log.info("Partner status changed from DOCUMENTS_MISSING to PENDING for re-review, partner id: {}", partnerId);
        }
        // Si déjà ACTIVE, INACTIVE, SUSPENDED, REJECTED : on conserve le statut (simple mise à jour du profil).

        partner = partnerRepository.save(partner);
        log.info("Partner profile updated successfully for id: {}, status unchanged: {}", partnerId, partner.getStatus());

        // Notifier les admins (PARTNER_REQUEST_SUBMITTED) lors de :
        // - La première soumission (statut null)
        // - La réouverture après DOCUMENTS_MISSING (pour réexamen)
        // Une simple modification de profil ACTIVE/INACTIVE/etc ne déclenche pas de notification.
        if (currentStatus == null || currentStatus == PartnerStatus.DOCUMENTS_MISSING || currentStatus == PartnerStatus.PENDING) {
            try {
                partnerEventPublisher.publish(PartnerEvent.builder()
                        .eventType(PartnerEvent.EventType.PARTNER_REQUEST_SUBMITTED)
                        .partnerId(partner.getId())
                        .userId(partner.getUserId())
                        .businessName(partner.getBusinessName())
                        .brandName(partner.getBrandName())
                        .email(partner.getEmail())
                        .status(partner.getStatus().name())
                        .timestamp(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to publish partner event for id {}: {}", partnerId, e.getMessage());
            }
        }

        // Update auth-service with partner ID using Feign Client
        try {
            log.info("========== UPDATING AUTH-SERVICE WITH PARTNER ID ==========");
            log.info("PartnerId: {}, UserId: {}", partner.getId(), partner.getUserId());
            
            var body = Map.of("partnerId", partner.getId());
            var response = authServiceClient.updatePartnerId(partner.getUserId(), body);
            
            log.info("✅ Successfully updated auth-service. Status: {}, Response: {}", 
                     response.getStatusCode(), response.getBody());
            log.info("========== AUTH-SERVICE UPDATE COMPLETED ==========");
        } catch (feign.FeignException e) {
            log.error("========== FEIGN ERROR UPDATING AUTH-SERVICE ==========");
            log.error("Status: {}, Reason: {}, Body: {}", 
                     e.status(), e.getMessage(), e.contentUTF8());
            log.error("PartnerId: {}, UserId: {}", partner.getId(), partner.getUserId());
            // Don't throw exception - partner profile is already saved
        } catch (Exception e) {
            log.error("========== ERROR UPDATING AUTH-SERVICE ==========");
            log.error("Error type: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("PartnerId: {}, UserId: {}", partner.getId(), partner.getUserId());
            // Don't throw exception - partner profile is already saved
        }

        return convertToDTO(partner);
    }

    // ==================== OPÉRATIONS CRUD ====================

    @Override
    @Transactional
    public PartnerDTO createPartner(Long userId, String businessName, PartnerType type,
                                   String description, String address, String city,
                                   BigDecimal latitude, BigDecimal longitude) {
        // TODO: Implémenter la création d'un partenaire
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerDTO getPartnerById(Long partnerId) {
        log.info("Getting partner by id: {}", partnerId);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        return convertToDTO(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerDTO getPartnerBySlug(String slug) {
        // TODO: Implémenter la récupération par slug
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerDTO getPartnerByUserId(Long userId) {
        log.info("Getting partner by userId: {}", userId);
        Partner partner = partnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Partner not found for userId: " + userId));
        return convertToDTO(partner);
    }

    @Override
    @Transactional
    public PartnerDTO updatePartner(Long partnerId, String businessName, String description,
                                   String phoneNumber, String email) {
        // TODO: Implémenter la mise à jour
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public PartnerDTO updateLocation(Long partnerId, String address, String city,
                                    String postalCode, BigDecimal latitude, BigDecimal longitude) {
        // TODO: Implémenter la mise à jour de la localisation
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public PartnerDTO updateImages(Long partnerId, String logo, String coverImage) {
        log.info("Updating images for partner id: {}", partnerId);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        
        if (logo != null) {
            partner.setLogo(logo);
        }
        if (coverImage != null) {
            partner.setCoverImage(coverImage);
        }
        
        partner = partnerRepository.save(partner);
        log.info("Images updated for partner id: {}", partnerId);
        return convertToDTO(partner);
    }

    @Override
    @Transactional
    public void deletePartner(Long partnerId) {
        // TODO: Implémenter la suppression
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== GESTION DU STATUT ====================

    @Override
    @Transactional
    public PartnerDTO setAcceptsOrders(Long partnerId, boolean acceptsOrders) {
        // TODO: Implémenter la mise à jour de l'acceptation de commandes
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public PartnerDTO updateStatus(Long partnerId, PartnerStatus status) {
        // TODO: Implémenter la mise à jour du statut
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public PartnerDTO approvePartner(Long partnerId) {
        log.info("Approving partner id: {}", partnerId);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        // Allow approval from PENDING or DOCUMENTS_MISSING status
        if (partner.getStatus() != PartnerStatus.PENDING && partner.getStatus() != PartnerStatus.DOCUMENTS_MISSING) {
            throw new RuntimeException("Partner cannot be approved from current status: " + partner.getStatus() + ". Only PENDING or DOCUMENTS_MISSING status can be approved.");
        }

        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setIsActive(true);
        partner.setIsVerified(true);
        partner.setAcceptsOrders(true);

        partner = partnerRepository.save(partner);
        log.info("Partner {} approved successfully", partnerId);

        // Publish Pub/Sub event to notify partner
        try {
            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PARTNER_APPROVED)
                    .partnerId(partner.getId())
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(PartnerStatus.ACTIVE.name())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish approval event for partner {}: {}", partnerId, e.getMessage());
        }

        return convertToDTO(partner);
    }

    @Override
    @Transactional
    public void rejectPartner(Long partnerId, String reason) {
        log.info("Rejecting partner id: {} with reason: {}", partnerId, reason);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        partner.setStatus(PartnerStatus.REJECTED);
        partner.setIsActive(false);
        partner.setAcceptsOrders(false);

        partnerRepository.save(partner);
        log.info("Partner {} rejected", partnerId);

        // Publish Pub/Sub event to notify partner
        try {
            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PARTNER_REJECTED)
                    .partnerId(partner.getId())
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(PartnerStatus.REJECTED.name())
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish rejection event for partner {}: {}", partnerId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public PartnerDTO requestMoreInfo(Long partnerId, String message) {
        log.info("Requesting more info for partner id: {} with message: {}", partnerId, message);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        partner.setStatus(PartnerStatus.DOCUMENTS_MISSING);
        partner.setIsActive(false);
        partner.setAcceptsOrders(false);

        partner = partnerRepository.save(partner);
        log.info("Partner {} status changed to DOCUMENTS_MISSING", partnerId);

        // Publish Pub/Sub event to notify partner
        try {
            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PARTNER_INFO_REQUESTED)
                    .partnerId(partner.getId())
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(PartnerStatus.DOCUMENTS_MISSING.name())
                    .reason(message)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish info request event for partner {}: {}", partnerId, e.getMessage());
        }

        return convertToDTO(partner);
    }

    @Override
    @Transactional
    public void suspendPartner(Long partnerId, String reason) {
        log.info("Suspending partner id: {} with reason: {}", partnerId, reason);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        partner.setStatus(PartnerStatus.SUSPENDED);
        partner.setIsActive(false);
        partner.setAcceptsOrders(false);

        partnerRepository.save(partner);
        log.info("Partner {} suspended", partnerId);
        
        // Publish Pub/Sub event
        try {
            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PARTNER_SUSPENDED)
                    .partnerId(partner.getId())
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(PartnerStatus.SUSPENDED.name())
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish suspension event for partner {}: {}", partnerId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public PartnerDTO activatePartner(Long partnerId) {
        log.info("Activating partner id: {}", partnerId);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        partner.setStatus(PartnerStatus.ACTIVE);
        partner.setIsActive(true);
        partner.setIsVerified(true);
        partner.setAcceptsOrders(true);

        partner = partnerRepository.save(partner);
        log.info("Partner {} activated successfully", partnerId);

        // Publish Pub/Sub event to notify partner (use PARTNER_ACTIVATED, not PARTNER_APPROVED)
        try {
            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PARTNER_ACTIVATED)
                    .partnerId(partner.getId())
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(PartnerStatus.ACTIVE.name())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish activation event for partner {}: {}", partnerId, e.getMessage());
        }

        return convertToDTO(partner);
    }

    @Override
    @Transactional
    public PartnerDTO deactivatePartner(Long partnerId, String reason) {
        log.info("Deactivating partner id: {} with reason: {}", partnerId, reason);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        partner.setStatus(PartnerStatus.INACTIVE);
        partner.setIsActive(false);
        partner.setAcceptsOrders(false);

        partner = partnerRepository.save(partner);
        log.info("Partner {} deactivated successfully", partnerId);

        // Publish Pub/Sub event to notify partner
        try {
            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PARTNER_DEACTIVATED)
                    .partnerId(partner.getId())
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(PartnerStatus.INACTIVE.name())
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish deactivation event for partner {}: {}", partnerId, e.getMessage());
        }

        return convertToDTO(partner);
    }

    // ==================== PARAMÈTRES DE LIVRAISON ====================

    @Override
    @Transactional
    public PartnerDTO updateDeliverySettings(Long partnerId, Integer preparationTime,
                                           BigDecimal deliveryFee, BigDecimal minimumOrder,
                                           Integer deliveryRadius) {
        // TODO: Implémenter la mise à jour des paramètres de livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public PartnerDTO setFreeDeliveryThreshold(Long partnerId, BigDecimal freeDeliveryThreshold) {
        // TODO: Implémenter la définition du seuil de livraison gratuite
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== HORAIRES ====================

    @Override
    @Transactional
    public PartnerDTO updateOpeningHours(Long partnerId, String openingHoursJson) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        partner.setOpeningHoursJson(openingHoursJson);
        partner = partnerRepository.save(partner);
        log.info("Opening hours updated for partner {}", partnerId);
        return convertToDTO(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<?> getOpeningHours(Long partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        String json = partner.getOpeningHoursJson();
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("Failed to parse opening hours JSON for partner {}: {}", partnerId, e.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurrentlyOpen(Long partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        if (!partner.getIsActive() || !partner.getAcceptsOrders()) return false;
        if (partner.getOpeningHoursJson() == null || partner.getOpeningHoursJson().isBlank()) return true;
        // Simple check: could be enhanced with current time vs opening hours
        return true;
    }

    @Override
    @Transactional
    public PartnerDTO updateOpenStatus(Long partnerId, boolean isOpen) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));
        partner.setAcceptsOrders(isOpen);
        partner = partnerRepository.save(partner);
        log.info("Partner {} open status set to acceptsOrders={}", partnerId, isOpen);
        return convertToDTO(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffMemberDTO> getStaff(Long partnerId) {
        if (!partnerRepository.existsById(partnerId)) {
            throw new RuntimeException("Partner not found with id: " + partnerId);
        }
        return staffMemberRepository.findByPartnerIdOrderByCreatedAtAsc(partnerId).stream()
                .map(sm -> StaffMemberDTO.builder()
                        .id(sm.getId())
                        .partnerId(sm.getPartnerId())
                        .userId(sm.getUserId())
                        .role(sm.getRole())
                        .build())
                .toList();
    }

    // ==================== CATÉGORIES ET TAGS ====================

    @Override
    @Transactional
    public PartnerDTO updateCategories(Long partnerId, List<Long> categoryIds) {
        // TODO: Implémenter la mise à jour des catégories
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public PartnerDTO updateTags(Long partnerId, List<String> tags) {
        // TODO: Implémenter la mise à jour des tags
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== STATISTIQUES ====================

    @Override
    @Transactional
    public void incrementOrderCount(Long partnerId, BigDecimal orderAmount) {
        // TODO: Implémenter l'incrémentation du compteur de commandes
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public void updateRating(Long partnerId, BigDecimal rating) {
        // TODO: Implémenter la mise à jour de la note
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== RECHERCHE ET LISTE ====================

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getAllActivePartners(Pageable pageable) {
        log.info("Getting all active partners");
        return partnerRepository.findByStatus(PartnerStatus.ACTIVE, pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getPartnersByStatus(PartnerStatus status, Pageable pageable) {
        log.info("Getting partners by status: {}", status);
        if (status == null) {
            return partnerRepository.findAll(pageable)
                    .map(this::convertToDTO);
        }
        return partnerRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getPartnersByStatusAndSearch(PartnerStatus status, String search, Pageable pageable) {
        log.info("Getting partners by status: {} and search: {}", status, search);
        if (search != null && !search.isBlank()) {
            return partnerRepository.findByStatusAndSearch(status, search.trim(), pageable)
                    .map(this::convertToDTO);
        }
        return getPartnersByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getPartnersByType(PartnerType type, Pageable pageable) {
        // TODO: Implémenter la récupération paginée par type
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getPartnersByCity(String city, Pageable pageable) {
        // TODO: Implémenter la récupération paginée par ville
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerDTO> getNearbyPartners(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        return getNearbyPartners(latitude, longitude, 0, Integer.MAX_VALUE / 2).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getNearbyPartners(BigDecimal latitude, BigDecimal longitude, int page, int size) {
        // 1. Read radius from Redis or use config fallback
        double radiusKm = defaultRadiusKm;
        try {
            String redisRadius = redisTemplate.opsForValue().get("config:nearby:radius_km");
            if (redisRadius != null) radiusKm = Double.parseDouble(redisRadius);
        } catch (Exception e) {
            log.warn("Could not read radius from Redis: {}", e.getMessage());
        }
        double radiusMeters = radiusKm * 1000.0;

        // 2. Check cache
        String cacheKey = String.format("partners:nearby:%.4f:%.4f:%d:%d",
                latitude.doubleValue(), longitude.doubleValue(), page, size);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                CachedPage cp = objectMapper.readValue(cached, CachedPage.class);
                return new PageImpl<>(cp.content(), PageRequest.of(cp.pageNumber(), cp.pageSize()), cp.totalElements());
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed: {}", e.getMessage());
        }

        // 3. Query DB with PostGIS ST_DWithin
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        long total = partnerRepository.countNearbyPartners(lat, lng, radiusMeters);
        List<Object[]> rows = total == 0 ? List.of() :
                partnerRepository.findNearbyPartnersSorted(lat, lng, radiusMeters, size, page * size);

        // 4. Bulk load entities and build DTOs
        List<Long> ids = rows.stream().map(r -> ((Number) r[0]).longValue()).toList();
        Map<Long, Partner> byId = partnerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Partner::getId, p -> p));
        List<PartnerDTO> dtos = rows.stream()
                .map(r -> {
                    Long id = ((Number) r[0]).longValue();
                    double distKm = ((Number) r[1]).doubleValue();
                    Partner p = byId.get(id);
                    if (p == null) return null;
                    PartnerDTO dto = convertToDTO(p);
                    dto.setDistanceKm(distKm);
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();

        Page<PartnerDTO> result = new PageImpl<>(dtos, PageRequest.of(page, size), total);

        // 5. Cache with 2-min TTL
        try {
            String json = objectMapper.writeValueAsString(
                    new CachedPage(dtos, page, size, total));
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(2));
        } catch (Exception e) {
            log.warn("Redis cache write failed: {}", e.getMessage());
        }

        return result;
    }

    private record CachedPage(List<PartnerDTO> content, int pageNumber, int pageSize, long totalElements) {}

    @Override
    @Transactional(readOnly = true)
    public List<PartnerDTO> getFeaturedPartners() {
        // TODO: Implémenter la récupération des partenaires en vedette
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerDTO> getTopRatedPartners(Pageable pageable) {
        // TODO: Implémenter la récupération paginée des meilleurs partenaires
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailableCities() {
        // TODO: Implémenter la récupération des villes disponibles
        throw new UnsupportedOperationException("À implémenter");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Convertir Partner entity en PartnerDTO
     */
    private PartnerDTO convertToDTO(Partner partner) {
        if (partner == null) {
            return null;
        }

        // Convertir categoryIds (String "1,2,3") en List<Long>
        List<Long> categoryIdsList = null;
        if (partner.getCategoryIds() != null && !partner.getCategoryIds().isEmpty()) {
            categoryIdsList = List.of(partner.getCategoryIds().split(","))
                    .stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
        }

        // Convertir tags (String "tag1,tag2") en List<String>
        List<String> tagsList = null;
        if (partner.getTags() != null && !partner.getTags().isEmpty()) {
            tagsList = List.of(partner.getTags().split(","))
                    .stream()
                    .map(String::trim)
                    .toList();
        }

        List<?> openingHoursList = null;
        if (partner.getOpeningHoursJson() != null && !partner.getOpeningHoursJson().isBlank()) {
            try {
                openingHoursList = objectMapper.readValue(partner.getOpeningHoursJson(), List.class);
            } catch (Exception e) {
                log.trace("Could not parse openingHoursJson for partner {}: {}", partner.getId(), e.getMessage());
            }
        }

        return PartnerDTO.builder()
                .id(partner.getId())
                .userId(partner.getUserId())
                .businessName(partner.getBusinessName())
                .name(partner.getBusinessName())
                .brandName(partner.getBrandName())
                .slug(partner.getSlug())
                .type(partner.getType())
                .description(partner.getDescription())
                .shortDescription(partner.getShortDescription())
                .logo(partner.getLogo())
                .logoUrl(partner.getLogo())
                .coverImage(partner.getCoverImage())
                .coverUrl(partner.getCoverImage())
                .phoneNumber(partner.getPhoneNumber())
                .email(partner.getEmail())
                // Legal Information
                .legalStatus(partner.getLegalStatus())
                .tva(partner.getTva())
                .legalRepFirstName(partner.getLegalRepFirstName())
                .legalRepLastName(partner.getLegalRepLastName())
                .position(partner.getPosition())
                // Bank Information
                .accountHolderName(partner.getAccountHolderName())
                .iban(partner.getIban())
                .bankName(partner.getBankName())
                .currency(partner.getCurrency())
                // Address
                .address(partner.getAddress())
                .city(partner.getCity())
                .postalCode(partner.getPostalCode())
                .state(partner.getState())
                .country(partner.getCountry())
                .latitude(partner.getLatitude())
                .longitude(partner.getLongitude())
                .deliveryRadius(partner.getDeliveryRadius())
                // Status
                .status(partner.getStatus())
                .isActive(partner.getIsActive())
                .acceptsOrders(partner.getAcceptsOrders())
                .isVerified(partner.getIsVerified())
                .isPremium(partner.getIsPremium())
                .isFeatured(partner.getIsFeatured())
                .isCurrentlyOpen(Boolean.TRUE.equals(partner.getIsActive()) && Boolean.TRUE.equals(partner.getAcceptsOrders()))
                .commissionRate(partner.getCommissionRate())
                // Delivery Settings
                .preparationTime(partner.getPreparationTime())
                .deliveryFee(partner.getDeliveryFee())
                .minimumOrder(partner.getMinimumOrder())
                .freeDeliveryThreshold(partner.getFreeDeliveryThreshold())
                // Payment Methods
                .acceptOnlinePayment(partner.getAcceptOnlinePayment())
                .acceptCashPayment(partner.getAcceptCashPayment())
                // Statistics
                .rating(partner.getRating())
                .totalRatings(partner.getTotalRatings())
                .reviewCount(partner.getTotalRatings() != null ? partner.getTotalRatings() : 0)
                .totalOrders(partner.getTotalOrders())
                .totalRevenue(partner.getTotalRevenue())
                // Documents
                .kbisUrl(partner.getKbisUrl())
                .idCardUrl(partner.getIdCardUrl())
                .insuranceUrl(partner.getInsuranceUrl())
                .ribUrl(partner.getRibUrl())
                .photosJson(partner.getPhotosJson())
                // Categories and Tags
                .categoryIds(categoryIdsList)
                .tags(tagsList)
                .openingHoursDisplay(partner.getOpeningHoursJson())
                .openingHours(openingHoursList)
                .scheduleExceptionsDisplay(partner.getScheduleExceptionsJson())
                .internalNotes(partner.getInternalNotes())
                .createdAt(partner.getCreatedAt())
                .build();
    }

    @Override
    public PartnerDTO updateInternalNotes(Long partnerId, String notes) {
        log.info("Updating internal notes for partner id: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found with id: " + partnerId));

        partner.setInternalNotes(notes);
        Partner saved = partnerRepository.save(partner);

        log.info("Internal notes updated for partner id: {}", partnerId);
        return convertToDTO(saved);
    }
}
