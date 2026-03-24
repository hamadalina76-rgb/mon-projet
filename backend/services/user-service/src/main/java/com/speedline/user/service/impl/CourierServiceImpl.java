package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.client.NotificationServiceClient;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@Slf4j
@Transactional
public class CourierServiceImpl implements CourierService {

    private final CourierRepository courierRepository;
    private final AuthServiceClient authServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final String uploadBaseDir;

    public CourierServiceImpl(CourierRepository courierRepository,
                             AuthServiceClient authServiceClient,
                             NotificationServiceClient notificationServiceClient,
                             @Value("${file.upload.dir}") String uploadBaseDir) {
        this.courierRepository = courierRepository;
        this.authServiceClient = authServiceClient;
        this.notificationServiceClient = notificationServiceClient;

        // Convert relative path to absolute path
        java.io.File uploadDir = new java.io.File(uploadBaseDir);
        this.uploadBaseDir = uploadDir.getAbsolutePath();
        log.info("📂 Upload directory configured: {}", this.uploadBaseDir);

        // Create base directory if it doesn't exist
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (created) {
                log.info("✅ Base upload directory created: {}", this.uploadBaseDir);
            } else {
                log.warn("⚠️ Could not create base upload directory: {}", this.uploadBaseDir);
            }
        }
    }

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

        try {
            notificationServiceClient.sendAdminBroadcast(NotificationServiceClient.AdminBroadcastRequest.builder()
                    .type("COURIER")
                    .title("Nouveau livreur inscrit")
                    .message("Un nouveau livreur a soumis son inscription.")
                    .data(Map.of("action", "REVIEW_COURIER", "courierId", courier.getId()))
                    .build());
        } catch (Exception e) {
            log.warn("Could not send admin notification for new courier: {}", e.getMessage());
        }

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

        // Mettre à jour le type de livreur si fourni
        if (request.getCourierType() != null) {
            courier.setCourierType(request.getCourierType());
        }

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

        // Mettre à jour les documents d'identité
        if (request.getIdentityNumber() != null) {
            courier.setIdentityNumber(request.getIdentityNumber());
        }
        if (request.getIdentityDocumentFrontImage() != null) {
            courier.setIdentityDocumentFrontImage(request.getIdentityDocumentFrontImage());
        }
        if (request.getIdentityDocumentBackImage() != null) {
            courier.setIdentityDocumentBackImage(request.getIdentityDocumentBackImage());
        }

        // Mettre à jour le permis de conduire
        if (request.getDrivingLicenseNumber() != null) {
            courier.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        }
        if (request.getDrivingLicenseImage() != null) {
            courier.setDrivingLicenseImage(request.getDrivingLicenseImage());
        }
        if (request.getDrivingLicenseExpiry() != null) {
            courier.setDrivingLicenseExpiry(request.getDrivingLicenseExpiry().atStartOfDay());
        }

        courier = courierRepository.save(courier);
        log.info("UPDATE - Livreur {} mis à jour avec succès", courierId);

        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public CourierDTO updateCourierByUserId(Long userId, CourierUpdateRequest request) {
        log.info("Mise à jour du livreur par userId: {}", userId);

        // Trouver le livreur par userId (auth-service ID)
        final Courier courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> CourierNotFoundException.byUserId(userId));

        // Mettre à jour les champs véhicule si fournis
        Optional.ofNullable(request.getVehicleType())
                .ifPresent(courier::setVehicleType);
        Optional.ofNullable(request.getVehicleNumber())
                .ifPresent(courier::setVehicleNumber);
        Optional.ofNullable(request.getVehicleModel())
                .ifPresent(courier::setVehicleModel);
        Optional.ofNullable(request.getVehicleColor())
                .ifPresent(courier::setVehicleColor);

        // Mettre à jour les préférences de livraison
        Optional.ofNullable(request.getPreferredDeliveryZone())
                .ifPresent(courier::setPreferredDeliveryZone);
        Optional.ofNullable(request.getMaxDeliveryRadius())
                .ifPresent(courier::setMaxDeliveryRadius);

        // Mettre à jour les coordonnées bancaires
        Optional.ofNullable(request.getBankIban())
                .ifPresent(courier::setBankIban);
        Optional.ofNullable(request.getBankAccountHolder())
                .ifPresent(courier::setBankAccountHolder);

        // Mettre à jour les documents d'identité
        Optional.ofNullable(request.getIdentityNumber())
                .ifPresent(courier::setIdentityNumber);
        Optional.ofNullable(request.getIdentityDocumentFrontImage())
                .ifPresent(courier::setIdentityDocumentFrontImage);
        Optional.ofNullable(request.getIdentityDocumentBackImage())
                .ifPresent(courier::setIdentityDocumentBackImage);

        // Mettre à jour le permis de conduire
        Optional.ofNullable(request.getDrivingLicenseNumber())
                .ifPresent(courier::setDrivingLicenseNumber);
        Optional.ofNullable(request.getDrivingLicenseImage())
                .ifPresent(courier::setDrivingLicenseImage);
        Optional.ofNullable(request.getDrivingLicenseExpiry())
                .ifPresent(expiry -> courier.setDrivingLicenseExpiry(expiry.atStartOfDay()));

        // Marquer la documentation comme complète si tous les documents requis sont fournis
        if (courier.getIdentityNumber() != null &&
            courier.getIdentityDocumentFrontImage() != null &&
            courier.getIdentityDocumentBackImage() != null &&
            courier.getDrivingLicenseNumber() != null &&
            courier.getDrivingLicenseImage() != null) {
            courier.setDocumentsVerified(true);
        }

        Courier savedCourier = courierRepository.save(courier);
        log.info("UPDATE - Livreur avec userId {} mis à jour avec succès", userId);

        return mapToDTO(savedCourier);
    }

    @Override
    @Transactional
    public CourierDTO updateCourierDocumentationByUserId(
            Long userId, String vehicleType, String vehicleModel, String vehicleColor, String plateNumber,
            String idNumber, String licenseNumber, String licenseExpiryDate,
            String accountHolder, String accountNumber,
            org.springframework.web.multipart.MultipartFile idCardFront,
            org.springframework.web.multipart.MultipartFile idCardBack,
            org.springframework.web.multipart.MultipartFile licenseFront,
            org.springframework.web.multipart.MultipartFile licenseBack) {

        log.info("Mise à jour de la documentation complète pour userId: {}", userId);

        // Trouver le livreur par userId
        Courier courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> CourierNotFoundException.byUserId(userId));

        // Mettre à jour les informations du véhicule
        if (vehicleType != null && !vehicleType.isBlank()) {
            try {
                courier.setVehicleType(VehicleType.valueOf(vehicleType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Type de véhicule invalide: {}, utilisation de BICYCLE par défaut", vehicleType);
                courier.setVehicleType(VehicleType.BICYCLE);
            }
        }
        if (vehicleModel != null && !vehicleModel.isBlank()) {
            courier.setVehicleModel(vehicleModel);
        }
        if (vehicleColor != null && !vehicleColor.isBlank()) {
            courier.setVehicleColor(vehicleColor);
        }
        if (plateNumber != null && !plateNumber.isBlank()) {
            courier.setVehicleNumber(plateNumber);
        }

        // Mettre à jour les informations d'identité
        if (idNumber != null && !idNumber.isBlank()) {
            courier.setIdentityNumber(idNumber);
        }

        // Mettre à jour les informations de permis
        if (licenseNumber != null && !licenseNumber.isBlank()) {
            courier.setDrivingLicenseNumber(licenseNumber);
        }
        if (licenseExpiryDate != null && !licenseExpiryDate.isBlank()) {
            try {
                // Parse format MM/DD/YYYY
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                courier.setDrivingLicenseExpiry(LocalDateTime.parse(licenseExpiryDate + " 00:00:00",
                        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")));
            } catch (Exception e) {
                log.warn("Format de date invalide pour l'expiration du permis: {}", licenseExpiryDate);
            }
        }

        // Mettre à jour les informations bancaires
        if (accountHolder != null && !accountHolder.isBlank()) {
            courier.setBankAccountHolder(accountHolder);
        }
        if (accountNumber != null && !accountNumber.isBlank()) {
            courier.setBankIban(accountNumber);
        }

        // Traiter les fichiers uploadés et les sauvegarder sur le disque
        String uploadDir = "couriers/" + userId + "/";

        if (idCardFront != null && !idCardFront.isEmpty()) {
            String savedPath = saveUploadedFile(idCardFront, uploadDir, "id_front_");
            courier.setIdentityDocumentFrontImage(savedPath);
            log.info("✅ Photo recto CIN sauvegardée: {}", savedPath);
        }
        if (idCardBack != null && !idCardBack.isEmpty()) {
            String savedPath = saveUploadedFile(idCardBack, uploadDir, "id_back_");
            courier.setIdentityDocumentBackImage(savedPath);
            log.info("✅ Photo verso CIN sauvegardée: {}", savedPath);
        }
        if (licenseFront != null && !licenseFront.isEmpty()) {
            String savedPath = saveUploadedFile(licenseFront, uploadDir, "license_front_");
            courier.setDrivingLicenseImage(savedPath);
            log.info("✅ Photo permis recto sauvegardée: {}", savedPath);
        }
        if (licenseBack != null && !licenseBack.isEmpty()) {
            String savedPath = saveUploadedFile(licenseBack, uploadDir, "license_back_");
            // Le modèle actuel n'a pas de champ pour le verso du permis
            log.info("✅ Photo permis verso sauvegardée: {}", savedPath);
        }

        // Log current documentation state
        log.info("📋 Documentation state for userId {}:", userId);
        log.info("   - identityNumber: {}", courier.getIdentityNumber() != null ? "✅" : "❌");
        log.info("   - identityDocumentFrontImage: {}", courier.getIdentityDocumentFrontImage() != null ? "✅" : "❌");
        log.info("   - identityDocumentBackImage: {}", courier.getIdentityDocumentBackImage() != null ? "✅" : "❌");
        log.info("   - drivingLicenseNumber: {}", courier.getDrivingLicenseNumber() != null ? "✅" : "❌");
        log.info("   - drivingLicenseImage: {}", courier.getDrivingLicenseImage() != null ? "✅" : "❌");
        log.info("   - bankAccountHolder: {}", courier.getBankAccountHolder() != null ? "✅" : "❌");
        log.info("   - bankIban: {}", courier.getBankIban() != null ? "✅" : "❌");

        // Mark documentation as verified if at least identity doc front and driving license image are present
        if (courier.getIdentityDocumentFrontImage() != null && courier.getDrivingLicenseImage() != null) {
            courier.setDocumentsVerified(true);
            log.info("✅ Documentation verified for userId {} (critical docs present)", userId);
        } else {
            log.warn("⚠️ Documentation incomplete for userId {} - missing critical documents", userId);
        }

        courier = courierRepository.save(courier);
        log.info("UPDATE - Documentation updated for userId {}, documentsVerified={}", userId, courier.getDocumentsVerified());

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
    public CourierDTO submitDocuments(Long courierId, String drivingLicenseImage, String identityDocumentFrontImage, String identityDocumentBackImage, String profilePhoto) {
        log.info("Soumission de documents pour le livreur ID: {}", courierId);

        Courier courier = findCourierById(courierId);

        if (drivingLicenseImage != null && !drivingLicenseImage.isBlank()) {
            courier.setDrivingLicenseImage(drivingLicenseImage);
        }
        if (identityDocumentFrontImage != null && !identityDocumentFrontImage.isBlank()) {
            courier.setIdentityDocumentFrontImage(identityDocumentFrontImage);
        }
        if (identityDocumentBackImage != null && !identityDocumentBackImage.isBlank()) {
            courier.setIdentityDocumentBackImage(identityDocumentBackImage);
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
            case CIN_FRONT:
                courier.setIdentityDocumentFrontImage(request.getDocumentUrl());
                // Enregistrer le numéro CIN si fourni
                if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()) {
                    courier.setIdentityNumber(request.getDocumentNumber());
                }
                break;
            case CIN_BACK:
                courier.setIdentityDocumentBackImage(request.getDocumentUrl());
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
    public CourierDTO verifyDocuments(Long courierId, com.speedline.user.domain.CourierType courierType) {
        log.info("Admin: validation des documents pour livreur {} - type: {}", courierId, courierType);
        Courier courier = findCourierById(courierId);
        if (courier.getStatus() != CourierStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Seul un livreur en attente peut être approuvé. Statut actuel: " + courier.getStatus());
        }
        courier.setDocumentsVerified(true);
        courier.setStatus(CourierStatus.ACTIVE);
        courier.setRejectionReason(null);
        courier.setCourierType(courierType);
        courier = courierRepository.save(courier);
        log.info("Livreur {} approuvé (ACTIVE)", courierId);
        try {
            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title("Votre compte est activé")
                    .message("Félicitations ! Votre inscription a été approuvée. Vous pouvez maintenant accéder à l'application.")
                    .data(Map.of("action", "COURIER_APPROVED", "courierId", courierId))
                    .channel("IN_APP")
                    .build());
        } catch (Exception e) {
            log.warn("Could not notify courier of approval: {}", e.getMessage());
        }
        return mapToDTO(courier);
    }

    @Override
    @Transactional
    public void rejectDocuments(Long courierId, String reason) {
        log.info("Admin: rejet des documents pour livreur {}, raison: {}", courierId, reason);
        Courier courier = findCourierById(courierId);
        courier.setStatus(CourierStatus.REJECTED);
        courier.setRejectionReason(reason != null ? reason : "");
        courier = courierRepository.save(courier);
        log.info("Livreur {} rejeté", courierId);
        try {
            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title("Inscription refusée")
                    .message(reason != null && !reason.isBlank() ? "Votre inscription a été refusée. Raison : " + reason : "Votre inscription a été refusée.")
                    .data(Map.of("action", "COURIER_REJECTED", "courierId", courierId, "reason", reason != null ? reason : ""))
                    .channel("IN_APP")
                    .build());
        } catch (Exception e) {
            log.warn("Could not notify courier of rejection: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void requestMoreInfo(Long courierId, String message) {
        log.info("Admin: demande d'informations complémentaires pour livreur {}", courierId);
        Courier courier = findCourierById(courierId);
        courier.setRequestMoreInfoMessage(message != null ? message : "");
        courier = courierRepository.save(courier);
        log.info("Message enregistré pour livreur {}", courierId);
        try {
            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title("Informations complémentaires demandées")
                    .message(message != null && !message.isBlank() ? message : "L'équipe demande des informations complémentaires pour votre dossier.")
                    .data(Map.of("action", "COURIER_INFO_REQUESTED", "courierId", courierId))
                    .channel("IN_APP")
                    .build());
        } catch (Exception e) {
            log.warn("Could not notify courier of info request: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getCouriersAwaitingApproval(Pageable pageable) {
        return courierRepository.findByStatus(CourierStatus.PENDING_APPROVAL, pageable)
                .map(this::mapToDTO);
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
        return courierRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getCouriersByStatus(CourierStatus status, Pageable pageable) {
        return courierRepository.findByStatus(status, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> searchCouriers(String search, CourierStatus status, com.speedline.user.domain.CourierType courierType, Pageable pageable) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        return courierRepository.searchCouriers(status, courierType, term, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getOnlineCouriers(Pageable pageable) {
        return courierRepository.findByIsAvailableTrueAndIsOnlineTrue(pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourierDTO> getTopRatedCouriers(BigDecimal minRating, Pageable pageable) {
        return courierRepository.findTopRatedCouriers(minRating, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional
    public void deactivateCourier(Long courierId, String reason) {
        log.info("Admin: désactivation (block) du livreur {}, raison: {}", courierId, reason);
        Courier courier = findCourierById(courierId);
        courier.setStatus(CourierStatus.DEACTIVATED);
        courier.setSuspensionReason(reason != null ? reason : "");
        courier.setIsAvailable(false);
        courier.setIsOnline(false);
        courierRepository.save(courier);
        log.info("Livreur {} désactivé (DEACTIVATED)", courierId);
        try {
            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title("Compte désactivé")
                    .message(reason != null && !reason.isBlank() ? "Votre compte a été désactivé. Raison : " + reason : "Votre compte a été désactivé. Contactez le support.")
                    .data(Map.of("action", "COURIER_DEACTIVATED", "courierId", courierId, "reason", reason != null ? reason : ""))
                    .channel("IN_APP")
                    .build());
        } catch (Exception e) {
            log.warn("Could not notify courier of deactivation: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void suspendCourier(Long courierId, String reason) {
        log.info("Admin: suspension du livreur {}, raison: {}", courierId, reason);
        Courier courier = findCourierById(courierId);
        courier.setStatus(CourierStatus.SUSPENDED);
        courier.setSuspensionReason(reason != null ? reason : "");
        courier.setIsAvailable(false);
        courier.setIsOnline(false);
        courierRepository.save(courier);
        log.info("Livreur {} suspendu", courierId);
        try {
            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title("Compte suspendu")
                    .message(reason != null && !reason.isBlank() ? "Votre compte a été suspendu. Raison : " + reason : "Votre compte a été suspendu. Contactez le support.")
                    .data(Map.of("action", "COURIER_SUSPENDED", "courierId", courierId, "reason", reason != null ? reason : ""))
                    .channel("IN_APP")
                    .build());
        } catch (Exception e) {
            log.warn("Could not notify courier of suspension: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void reactivateCourier(Long courierId) {
        log.info("Admin: réactivation du livreur {}", courierId);
        Courier courier = findCourierById(courierId);
        CourierStatus current = courier.getStatus();
        if (current != CourierStatus.SUSPENDED && current != CourierStatus.DEACTIVATED && current != CourierStatus.REJECTED) {
            throw new IllegalStateException("Seul un livreur suspendu, désactivé ou rejeté peut être réactivé. Statut actuel: " + current);
        }
        if (current == CourierStatus.REJECTED) {
            courier.setStatus(CourierStatus.PENDING_APPROVAL);
            courier.setRejectionReason(null);
            log.info("Livreur {} remis en attente d'approbation (REJECTED → PENDING_APPROVAL)", courierId);
        } else {
            courier.setStatus(CourierStatus.ACTIVE);
            courier.setSuspensionReason(null);
            log.info("Livreur {} réactivé (ACTIVE)", courierId);
        }
        courierRepository.save(courier);
        try {
            String message = current == CourierStatus.REJECTED
                    ? "Votre dossier a été réouvert. Vous serez notifié après réexamen."
                    : "Votre compte a été réactivé. Vous pouvez à nouveau utiliser l'application.";
            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title("Compte réactivé")
                    .message(message)
                    .data(Map.of("action", "COURIER_REACTIVATED", "courierId", courierId))
                    .channel("IN_APP")
                    .build());
        } catch (Exception e) {
            log.warn("Could not notify courier of reactivation: {}", e.getMessage());
        }
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
        if (status == CourierStatus.REJECTED) {
            throw CourierNotAvailableException.notApproved(courier.getId());
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
                // Documentation
                .identityNumber(courier.getIdentityNumber())
                .identityDocumentFrontImage(courier.getIdentityDocumentFrontImage())
                .identityDocumentBackImage(courier.getIdentityDocumentBackImage())
                .drivingLicenseNumber(courier.getDrivingLicenseNumber())
                .drivingLicenseImage(courier.getDrivingLicenseImage())
                .drivingLicenseExpiry(courier.getDrivingLicenseExpiry())
                .bankAccountHolder(courier.getBankAccountHolder())
                .bankIban(courier.getBankIban())
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
                // Raisons admin
                .rejectionReason(courier.getRejectionReason())
                .requestMoreInfoMessage(courier.getRequestMoreInfoMessage())
                .suspensionReason(courier.getSuspensionReason())
                // Type livreur
                .courierType(courier.getCourierType())
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
            if (userInfo.getIsEmailVerified() != null) {
                dto.setIsEmailVerified(userInfo.getIsEmailVerified());
            }
        } catch (Exception e) {
            log.error("❌ FAILED to fetch user info from auth-service for userId: {}", courier.getUserId());
            log.error("❌ Error type: {}", e.getClass().getName());
            log.error("❌ Error message: {}", e.getMessage());
            log.error("❌ Full stack trace:", e);
            // Continue sans les infos utilisateur
        }

        return dto;
    }

    // ==================== GESTION DES FICHIERS ====================

    /**
     * Sauvegarde un fichier uploadé sur le disque
     *
     * @param file Le fichier à sauvegarder
     * @param directory Le répertoire de destination relatif (ex: "couriers/123/")
     * @param prefix Le préfixe du nom de fichier (ex: "id_front_")
     * @return Le chemin relatif du fichier sauvegardé (pour stockage en DB et accès via URL)
     */
    private String saveUploadedFile(org.springframework.web.multipart.MultipartFile file,
                                    String directory, String prefix) {
        try {
            // Construire le chemin absolu complet
            java.io.File uploadBaseDirFile = new java.io.File(uploadBaseDir);
            java.io.File targetDir = new java.io.File(uploadBaseDirFile, directory);

            // Créer le répertoire s'il n'existe pas
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (created) {
                    log.info("📁 Répertoire créé: {} (chemin absolu: {})", directory, targetDir.getAbsolutePath());
                } else {
                    log.error("❌ Impossible de créer le répertoire: {}", targetDir.getAbsolutePath());
                }
            }

            // Générer un nom de fichier déterministe par livreur et type de document
            // directory attendu: "couriers/{userId}/"
            String userIdPart = "unknown";
            try {
                String tmp = directory.replaceAll("\\\\", "/");
                if (tmp.endsWith("/")) tmp = tmp.substring(0, tmp.length() - 1);
                String[] parts = tmp.split("/");
                if (parts.length > 0) {
                    userIdPart = parts[parts.length - 1];
                }
            } catch (Exception e) {
                log.warn("Could not parse userId from directory '{}': {}", directory, e.getMessage());
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Exemple de nom: id_front_123.jpg => prefix + userId
            String filename = prefix + userIdPart + extension;

            // Créer le fichier de destination
            java.io.File destinationFile = new java.io.File(targetDir, filename);

            // Sauvegarder le fichier
            file.transferTo(destinationFile);

            // Retourner le chemin pour accès via URL (avec préfixe /uploads/)
            // Les fichiers seront accessibles via: http://localhost:8082/uploads/couriers/123/file.jpg
            String urlPath = "/uploads/" + directory + filename;
            log.info("💾 Fichier sauvegardé avec succès!");
            log.info("   📍 Chemin absolu: {}", destinationFile.getAbsolutePath());
            log.info("   🌐 URL d'accès: {}", urlPath);
            log.info("   📊 Taille: {} bytes", file.getSize());

            return urlPath;

        } catch (java.io.IOException e) {
            log.error("❌ Erreur lors de la sauvegarde du fichier: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sauvegarde du fichier: " + e.getMessage(), e);
        }
    }
}
