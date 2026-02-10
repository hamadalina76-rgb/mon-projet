package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.domain.Courier;
import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.VehicleType;
import com.speedline.user.dto.*;
import com.speedline.user.dto.CourierDocumentUploadRequest.DocumentType;
import com.speedline.user.exception.*;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.CourierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des livreurs
 * 
 * Gère les opérations liées aux profils livreurs :
 * - Consultation et mise à jour de profil
 * - Gestion de la disponibilité
 * - Upload de documents
 * - Statistiques
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierServiceImpl implements CourierService {

    private final CourierRepository courierRepository;
    private final AuthServiceClient authServiceClient;

    // Constantes de validation
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    // ==================== OPÉRATIONS CRUD ====================

    @Override
    @Transactional
    public CourierDTO createCourier(CourierCreateRequest request) {
        log.info("Création d'un nouveau profil livreur pour userId: {}", request.getUserId());

        // Vérifier qu'un profil n'existe pas déjà pour ce userId
        if (courierRepository.existsByUserId(request.getUserId())) {
            throw new UserAlreadyExistsException("Un profil livreur existe déjà pour userId: " + request.getUserId());
        }

        // Créer le nouveau livreur avec statut PENDING_APPROVAL
        Courier courier = Courier.builder()
                .userId(request.getUserId())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .vehicleModel(request.getVehicleModel())
                .vehicleColor(request.getVehicleColor())
                .drivingLicenseNumber(request.getDrivingLicenseNumber())
                .status(CourierStatus.PENDING_APPROVAL)
                .isAvailable(false)
                .isOnline(false)
                .documentsVerified(false)
                .build();

        courier = courierRepository.save(courier);
        log.info("Profil livreur créé avec succès. ID: {}, userId: {}", courier.getId(), courier.getUserId());

        return mapToDTO(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierDTO getCourierById(Long courierId) {
        log.debug("Récupération du livreur ID: {}", courierId);
        Courier courier = findCourierById(courierId);
        log.info("READ - Livreur {} récupéré", courierId);
        return mapToDTO(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierDTO getCourierByUserId(Long userId) {
        log.debug("Récupération du livreur par userId: {}", userId);
        Courier courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> CourierNotFoundException.byUserId(userId));
        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public CourierDTO updateCourier(Long courierId, CourierUpdateRequest request) {
        log.info("Mise à jour du livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);

        // Mettre à jour les champs véhicule si fournis
        if (request.getVehicleType() != null) {
            courier.setVehicleType(request.getVehicleType());
        }
        if (request.getVehicleNumber() != null) {
            courier.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getVehicleModel() != null) {
            courier.setVehicleModel(request.getVehicleModel());
        }
        if (request.getVehicleColor() != null) {
            courier.setVehicleColor(request.getVehicleColor());
        }

        // Mettre à jour les préférences de livraison
        if (request.getPreferredDeliveryZone() != null) {
            courier.setPreferredDeliveryZone(request.getPreferredDeliveryZone());
        }
        if (request.getMaxDeliveryRadius() != null) {
            courier.setMaxDeliveryRadius(request.getMaxDeliveryRadius());
        }

        // Mettre à jour les coordonnées bancaires
        if (request.getBankIban() != null) {
            courier.setBankIban(request.getBankIban());
        }
        if (request.getBankAccountHolder() != null) {
            courier.setBankAccountHolder(request.getBankAccountHolder());
        }

        courier = courierRepository.save(courier);
        log.info("UPDATE - Livreur {} mis à jour avec succès", courierId);

        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public void deleteCourier(Long courierId) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("La suppression de livreur n'est pas gérée par ce service");
    }

    // ==================== GESTION DE LA DISPONIBILITÉ ====================

    @Override
    @Transactional
    public CourierDTO updateAvailability(Long courierId, CourierAvailabilityRequest request) {
        log.info("Mise à jour de la disponibilité du livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);

        // Vérifier que le livreur peut être mis en disponibilité
        validateCanBeAvailable(courier);

        courier.setIsAvailable(request.getIsAvailable());
        courier.setIsOnline(request.getIsOnline());

        // Mettre à jour le statut selon la disponibilité
        if (Boolean.TRUE.equals(request.getIsOnline()) && Boolean.TRUE.equals(request.getIsAvailable())) {
            if (courier.getCurrentDeliveryId() == null) {
                courier.setStatus(CourierStatus.AVAILABLE);
            }
            courier.setLastLoginAt(LocalDateTime.now());
        } else if (Boolean.FALSE.equals(request.getIsOnline())) {
            if (courier.getCurrentDeliveryId() == null) {
                courier.setStatus(CourierStatus.OFFLINE);
            }
        }

        courier = courierRepository.save(courier);
        log.info("UPDATE - Disponibilité livreur {} mise à jour: online={}, available={}", 
                courierId, request.getIsOnline(), request.getIsAvailable());

        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public CourierDTO goOnline(Long courierId) {
        log.info("Passage en ligne du livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);
        validateCanBeAvailable(courier);

        courier.goOnline();
        courier.setStatus(CourierStatus.AVAILABLE);
        courier = courierRepository.save(courier);

        log.info("UPDATE - Livreur {} passé en ligne", courierId);
        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public CourierDTO goOffline(Long courierId) {
        log.info("Passage hors ligne du livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);
        courier.goOffline();
        
        if (courier.getCurrentDeliveryId() == null) {
            courier.setStatus(CourierStatus.OFFLINE);
        }
        
        courier = courierRepository.save(courier);
        log.info("UPDATE - Livreur {} passé hors ligne", courierId);

        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public void markAsBusy(Long courierId, Long deliveryId) {
        log.info("Marquage du livreur {} comme occupé (livraison {})", courierId, deliveryId);
        
        Courier courier = findCourierById(courierId);
        courier.startDelivery(deliveryId);
        courierRepository.save(courier);
        
        log.info("UPDATE - Livreur {} marqué comme occupé pour livraison {}", courierId, deliveryId);
    }

    @Override
    @Transactional
    public void markAsAvailable(Long courierId) {
        log.info("Marquage du livreur {} comme disponible", courierId);
        
        Courier courier = findCourierById(courierId);
        courier.setCurrentDeliveryId(null);
        courier.setIsAvailable(true);
        
        if (Boolean.TRUE.equals(courier.getIsOnline())) {
            courier.setStatus(CourierStatus.AVAILABLE);
        }
        
        courierRepository.save(courier);
        log.info("UPDATE - Livreur {} marqué comme disponible", courierId);
    }

    // ==================== GESTION DE LA LOCALISATION ====================

    @Override
    @Transactional
    public void updateLocation(Long courierId, CourierLocationUpdateRequest request) {
        log.debug("Mise à jour de la position du livreur ID: {}", courierId);

        validateCoordinates(request.getLatitude(), request.getLongitude());

        Courier courier = findCourierById(courierId);
        courier.updateLocation(request.getLatitude(), request.getLongitude());
        courierRepository.save(courier);

        log.debug("Position livreur {} mise à jour: {}, {}", courierId, request.getLatitude(), request.getLongitude());
    }

    @Override
    @Transactional(readOnly = true)
    public CourierDTO getCurrentLocation(Long courierId) {
        Courier courier = findCourierById(courierId);
        return mapToDTO(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierDTO> findAvailableCouriersNearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Recherche géospatiale non implémentée");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierDTO> findAvailableCouriersByVehicle(VehicleType vehicleType, BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Recherche par véhicule non implémentée");
    }

    // ==================== GESTION DES DOCUMENTS ====================

    @Override
    @Transactional
    public CourierDTO submitDocuments(Long courierId, String drivingLicenseImage, String identityDocumentImage, String profilePhoto) {
        log.info("Soumission de documents pour le livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);

        if (drivingLicenseImage != null && !drivingLicenseImage.isBlank()) {
            courier.setDrivingLicenseImage(drivingLicenseImage);
        }
        if (identityDocumentImage != null && !identityDocumentImage.isBlank()) {
            courier.setIdentityDocumentImage(identityDocumentImage);
        }
        if (profilePhoto != null && !profilePhoto.isBlank()) {
            courier.setProfilePhoto(profilePhoto);
        }

        courier = courierRepository.save(courier);
        log.info("UPLOAD - Documents soumis pour livreur {}", courierId);

        return mapToDTO(courier);
    }

    /**
     * Upload d'un document spécifique
     */
    @Transactional
    public CourierDTO uploadDocument(Long courierId, CourierDocumentUploadRequest request) {
        log.info("Upload de document {} pour le livreur ID: {}", request.getDocumentType(), courierId);

        if (request.getDocumentUrl() == null || request.getDocumentUrl().isBlank()) {
            throw DocumentUploadException.missingDocumentUrl();
        }

        Courier courier = findCourierById(courierId);

        switch (request.getDocumentType()) {
            case CIN:
                courier.setIdentityDocumentImage(request.getDocumentUrl());
                break;
            case LICENSE:
                courier.setDrivingLicenseImage(request.getDocumentUrl());
                courier.setDrivingLicenseNumber(request.getDocumentNumber());
                if (request.getExpiryDate() != null) {
                    courier.setDrivingLicenseExpiry(LocalDateTime.parse(request.getExpiryDate() + "T00:00:00"));
                }
                break;
            case PROFILE_PHOTO:
                courier.setProfilePhoto(request.getDocumentUrl());
                break;
            case INSURANCE:
            case VEHICLE:
                // Ces documents seraient stockés dans une table séparée en production
                log.info("Document {} enregistré pour livreur {}", request.getDocumentType(), courierId);
                break;
            default:
                throw DocumentUploadException.invalidDocumentType(request.getDocumentType().name());
        }

        courier = courierRepository.save(courier);
        log.info("UPLOAD - Document {} uploadé pour livreur {}", request.getDocumentType(), courierId);

        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public CourierDTO verifyDocuments(Long courierId) {
        // Non implémenté - admin only, hors scope
        throw new UnsupportedOperationException("Validation admin non implémentée");
    }

    @Override
    @Transactional
    public void rejectDocuments(Long courierId, String reason) {
        // Non implémenté - admin only, hors scope
        throw new UnsupportedOperationException("Rejet admin non implémenté");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getCouriersAwaitingApproval(Pageable pageable) {
        // Non implémenté - admin only, hors scope
        throw new UnsupportedOperationException("Liste admin non implémentée");
    }

    // ==================== STATISTIQUES ET PERFORMANCES ====================

    @Override
    @Transactional(readOnly = true)
    public CourierStatisticsDTO getCourierStatistics(Long courierId) {
        log.debug("Récupération des statistiques du livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);

        CourierStatisticsDTO stats = CourierStatisticsDTO.builder()
                .courierId(courierId)
                // Livraisons
                .totalDeliveries(courier.getTotalDeliveries())
                .successfulDeliveries(courier.getSuccessfulDeliveries())
                .cancelledDeliveries(courier.getCancelledDeliveries())
                .successRate(courier.getSuccessRate())
                .averageDeliveryTime(courier.getAverageDeliveryTime())
                // Notes
                .rating(courier.getRating())
                .totalRatings(courier.getTotalRatings())
                // Finances
                .totalEarnings(courier.getTotalEarnings())
                .weeklyEarnings(courier.getWeeklyEarnings())
                .availableBalance(courier.getAvailableBalance())
                // Distance
                .totalDistanceTravelled(courier.getTotalDistanceTravelled())
                .build();

        log.info("READ - Statistiques livreur {} récupérées", courierId);
        return stats;
    }

    @Override
    @Transactional
    public void updateRating(Long courierId, BigDecimal rating) {
        log.info("Mise à jour de la note du livreur ID: {} avec note {}", courierId, rating);

        if (rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(new BigDecimal("5")) > 0) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        }

        Courier courier = findCourierById(courierId);
        courier.updateRating(rating);
        courierRepository.save(courier);

        log.info("UPDATE - Note livreur {} mise à jour: {}", courierId, courier.getRating());
    }

    @Override
    @Transactional
    public void recordCompletedDelivery(Long courierId, BigDecimal earnings, BigDecimal distanceKm, int deliveryTimeMinutes) {
        log.info("Enregistrement livraison complétée pour livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);
        courier.completeDelivery(earnings, distanceKm);

        // Mise à jour du temps moyen de livraison
        int totalDeliveries = courier.getTotalDeliveries();
        int currentAvg = courier.getAverageDeliveryTime();
        int newAvg = ((currentAvg * (totalDeliveries - 1)) + deliveryTimeMinutes) / totalDeliveries;
        courier.setAverageDeliveryTime(newAvg);

        courierRepository.save(courier);
        log.info("UPDATE - Livraison complétée enregistrée pour livreur {}", courierId);
    }

    @Override
    @Transactional
    public void recordCancelledDelivery(Long courierId) {
        log.info("Enregistrement livraison annulée pour livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);
        courier.setCancelledDeliveries(courier.getCancelledDeliveries() + 1);
        courier.setTotalDeliveries(courier.getTotalDeliveries() + 1);
        courier.setCurrentDeliveryId(null);
        courier.setIsAvailable(true);

        courierRepository.save(courier);
        log.info("UPDATE - Livraison annulée enregistrée pour livreur {}", courierId);
    }

    // ==================== GESTION DES GAINS ====================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(Long courierId) {
        Courier courier = findCourierById(courierId);
        return courier.getAvailableBalance();
    }

    @Override
    @Transactional
    public BigDecimal requestWithdrawal(Long courierId, BigDecimal amount) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Retrait non implémenté");
    }

    // ==================== RECHERCHE ET LISTE ====================

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getAllCouriers(Pageable pageable) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Liste non implémentée");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getCouriersByStatus(CourierStatus status, Pageable pageable) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Liste par statut non implémentée");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getOnlineCouriers(Pageable pageable) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Liste en ligne non implémentée");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getTopRatedCouriers(BigDecimal minRating, Pageable pageable) {
        // Non implémenté - hors scope
        throw new UnsupportedOperationException("Top livreurs non implémenté");
    }

    @Override
    @Transactional
    public void suspendCourier(Long courierId, String reason) {
        // Non implémenté - admin only, hors scope
        throw new UnsupportedOperationException("Suspension admin non implémentée");
    }

    @Override
    @Transactional
    public void reactivateCourier(Long courierId) {
        // Non implémenté - admin only, hors scope
        throw new UnsupportedOperationException("Réactivation admin non implémentée");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        return courierRepository.existsByUserId(userId);
    }

    // ==================== MÉTHODES UTILITAIRES PRIVÉES ====================

    /**
     * Trouve un livreur par son ID ou lève une exception
     */
    private Courier findCourierById(Long courierId) {
        return courierRepository.findById(courierId)
                .orElseThrow(() -> CourierNotFoundException.byId(courierId));
    }

    /**
     * Vérifie que le livreur peut être mis en disponibilité
     */
    private void validateCanBeAvailable(Courier courier) {
        CourierStatus status = courier.getStatus();

        if (status == CourierStatus.PENDING_APPROVAL) {
            throw CourierNotAvailableException.notApproved(courier.getId());
        }
        if (status == CourierStatus.SUSPENDED) {
            throw CourierNotAvailableException.suspended(courier.getId());
        }
        if (status == CourierStatus.DEACTIVATED) {
            throw CourierNotAvailableException.inactive(courier.getId());
        }
    }

    /**
     * Valide les coordonnées GPS
     */
    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null) {
            if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
                throw InvalidCoordinatesException.invalidLatitude(latitude);
            }
        }
        if (longitude != null) {
            if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
                throw InvalidCoordinatesException.invalidLongitude(longitude);
            }
        }
    }

    /**
     * Mappe une entité Courier vers un DTO
     */
    private CourierDTO mapToDTO(Courier courier) {
        CourierDTO dto = CourierDTO.builder()
                .id(courier.getId())
                .userId(courier.getUserId())
                // Véhicule
                .vehicleType(courier.getVehicleType())
                .vehicleNumber(courier.getVehicleNumber())
                .vehicleModel(courier.getVehicleModel())
                .vehicleColor(courier.getVehicleColor())
                // Statut
                .status(courier.getStatus())
                .isAvailable(courier.getIsAvailable())
                .isOnline(courier.getIsOnline())
                .documentsVerified(courier.getDocumentsVerified())
                // Position
                .currentLatitude(courier.getCurrentLatitude())
                .currentLongitude(courier.getCurrentLongitude())
                .lastLocationUpdate(courier.getLastLocationUpdate())
                // Statistiques
                .rating(courier.getRating())
                .totalRatings(courier.getTotalRatings())
                .totalDeliveries(courier.getTotalDeliveries())
                .successfulDeliveries(courier.getSuccessfulDeliveries())
                .averageDeliveryTime(courier.getAverageDeliveryTime())
                .successRate(courier.getSuccessRate())
                // Finances
                .totalEarnings(courier.getTotalEarnings())
                .weeklyEarnings(courier.getWeeklyEarnings())
                .availableBalance(courier.getAvailableBalance())
                // Zone
                .preferredDeliveryZone(courier.getPreferredDeliveryZone())
                .maxDeliveryRadius(courier.getMaxDeliveryRadius())
                // Photo
                .profilePhoto(courier.getProfilePhoto())
                // Timestamps
                .createdAt(courier.getCreatedAt())
                .lastLoginAt(courier.getLastLoginAt())
                .build();

        // Enrichir avec les données utilisateur depuis auth-service
        try {
            log.info("🔍 Fetching user info from auth-service for userId: {}", courier.getUserId());
            UserInfoDTO userInfo = authServiceClient.getUserById(courier.getUserId());
            log.info("✅ User info retrieved: {} {}", userInfo.getFirstName(), userInfo.getLastName());
            dto.setEmail(userInfo.getEmail());
            dto.setFirstName(userInfo.getFirstName());
            dto.setLastName(userInfo.getLastName());
            dto.setPhoneNumber(userInfo.getPhoneNumber());
        } catch (Exception e) {
            log.error("❌ FAILED to fetch user info from auth-service for userId: {}", courier.getUserId());
            log.error("❌ Error type: {}", e.getClass().getName());
            log.error("❌ Error message: {}", e.getMessage());
            log.error("❌ Full stack trace:", e);
            // Continue sans les infos utilisateur
        }

        return dto;
    }
}
