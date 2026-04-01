package com.speedline.user.service.impl;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.client.NotificationServiceClient;
import com.speedline.user.domain.Admin;
import com.speedline.user.domain.Courier;
import com.speedline.user.domain.CourierExceptionalSchedule;
import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.CourierType;
import com.speedline.user.domain.UnavailabilityReason;
import com.speedline.user.domain.UnavailabilityValidationStatus;
import com.speedline.user.dto.CourierExceptionalScheduleDTO;
import com.speedline.user.repository.AdminRepository;
import com.speedline.user.repository.CourierExceptionalScheduleRepository;
import com.speedline.user.repository.CourierRepository;
import com.speedline.user.service.CourierExceptionalScheduleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierExceptionalScheduleServiceImpl implements CourierExceptionalScheduleService {

    private static final DateTimeFormatter NOTIF_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CourierExceptionalScheduleRepository repository;
    private final AdminRepository                      adminRepository;
    private final CourierRepository                    courierRepository;
    private final AuthServiceClient                    authServiceClient;
    private final NotificationServiceClient            notificationServiceClient;

    // ── getAll ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<CourierExceptionalScheduleDTO> getAll(
            int page, int size,
            String search, String exceptionType,
            LocalDate dateFrom, LocalDate dateTo,
            Long courierId) {

        return getDeclarationsForAdmin(
            page,
            size,
            search,
            exceptionType,
            dateFrom,
            dateTo,
            courierId,
            null,
            null,
            null);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<CourierExceptionalScheduleDTO> getDeclarationsForAdmin(
            int page,
            int size,
            String search,
            String exceptionType,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long courierId,
            UnavailabilityReason unavailabilityReason,
            UnavailabilityValidationStatus validationStatus,
            CourierType courierType) {

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "startDate"));

        return repository.findAll(
            buildSpec(search, exceptionType, dateFrom, dateTo, courierId, unavailabilityReason, validationStatus, courierType),
                pageable
        ).map(this::toDTO);
    }

    private Specification<CourierExceptionalSchedule> buildSpec(
            String search, String exceptionType,
            LocalDate dateFrom, LocalDate dateTo,
            Long courierId,
            UnavailabilityReason unavailabilityReason,
            UnavailabilityValidationStatus validationStatus,
            CourierType courierType) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("label")),       like),
                        cb.like(cb.lower(root.get("courierName")), like),
                        cb.like(cb.lower(root.get("reason")),      like)
                ));
            }
            if (exceptionType != null && !exceptionType.isBlank()) {
                predicates.add(cb.equal(root.get("exceptionType"), exceptionType));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), dateTo));
            }
            if (courierId != null) {
                predicates.add(cb.equal(root.get("courierId"), courierId));
            }
            if (unavailabilityReason != null) {
                predicates.add(cb.equal(root.get("unavailabilityReason"), unavailabilityReason));
            }
            if (validationStatus != null) {
                predicates.add(cb.equal(root.get("validationStatus"), validationStatus));
            }
            if (courierType != null) {
                predicates.add(cb.equal(root.get("courierType"), courierType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── getByCourierInRange ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CourierExceptionalScheduleDTO> getByCourierInRange(
            Long courierId, LocalDate from, LocalDate to) {
        return repository.findActiveInRange(courierId, from, to)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── checkOverlap ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CourierExceptionalScheduleDTO.OverlapResult checkOverlap(
            Long courierId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<CourierExceptionalScheduleDTO> overlapping = repository
                .findOverlapping(courierId, startDate, endDate, excludeId)
                .stream().map(this::toDTO).collect(Collectors.toList());
        return CourierExceptionalScheduleDTO.OverlapResult.builder()
                .hasOverlap(!overlapping.isEmpty())
                .overlapping(overlapping)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO create(CourierExceptionalScheduleDTO.CreateRequest req) {
        Courier courier = courierRepository.findById(req.getCourierId())
            .orElseThrow(() -> new RuntimeException("Livreur introuvable avec l'ID: " + req.getCourierId()));

        String courierName = resolveCourierName(req.getCourierId());
        Long   adminId     = resolveCurrentAdminUserId();
        String adminName   = resolveAdminName(adminId);

        CourierExceptionalSchedule entity = CourierExceptionalSchedule.builder()
                .courierId(req.getCourierId())
                .courierType(courier.getCourierType())
                .courierName(courierName)
                .exceptionType(req.getExceptionType())
                .label(req.getLabel())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .reason(req.getReason())
                .isRestPeriod(req.getIsRestPeriod() != null ? req.getIsRestPeriod() : true)
                .validationStatus(UnavailabilityValidationStatus.APPROVED_ACTIVE)
                .isActive(true)
                .adminId(adminId)
                .adminName(adminName)
                .build();

        CourierExceptionalSchedule saved = repository.save(entity);
        
        // Notify courier that a new exceptional schedule was added by admin
        notifyCourierScheduleUpdated(courier, saved);
        
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO update(Long id, CourierExceptionalScheduleDTO.CreateRequest req) {
        CourierExceptionalSchedule entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planning exceptionnel non trouvé avec l'ID: " + id));

        Courier courier = courierRepository.findById(req.getCourierId())
            .orElseThrow(() -> new RuntimeException("Livreur introuvable avec l'ID: " + req.getCourierId()));

        entity.setCourierId(req.getCourierId());
        entity.setCourierType(courier.getCourierType());
        entity.setCourierName(resolveCourierName(req.getCourierId()));
        entity.setExceptionType(req.getExceptionType());
        entity.setLabel(req.getLabel());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setStartsAt(req.getStartsAt());
        entity.setEndsAt(req.getEndsAt());
        entity.setReason(req.getReason());
        if (req.getIsRestPeriod() != null) entity.setIsRestPeriod(req.getIsRestPeriod());

        CourierExceptionalSchedule saved = repository.save(entity);
        
        // Notify courier about the update
        notifyCourierScheduleUpdated(courier, saved);

        return toDTO(saved);
    }

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO declareUnavailability(
            Long userId,
            CourierExceptionalScheduleDTO.CourierDeclarationRequest req) {

        Courier courier = resolveCourierByUserId(userId);
        UnavailabilityValidationStatus status = courier.getCourierType() == CourierType.INTERNAL
                ? UnavailabilityValidationStatus.PENDING_VALIDATION
                : UnavailabilityValidationStatus.APPROVED_ACTIVE;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = req.getStartsAt() == null ? now : req.getStartsAt();
        LocalDateTime endsAt;
        if (req.getEndsAt() != null) {
            endsAt = req.getEndsAt();
        } else if (req.getEstimatedDurationMinutes() != null) {
            endsAt = startsAt.plusMinutes(req.getEstimatedDurationMinutes());
        } else {
            endsAt = startsAt;
        }

        LocalDate startDate = startsAt.toLocalDate();
        LocalDate endDate = endsAt.toLocalDate();

        String mappedExceptionType = mapReasonToExceptionType(req.getUnavailabilityReason());

        CourierExceptionalSchedule entity = CourierExceptionalSchedule.builder()
                .courierId(courier.getId())
                .courierType(courier.getCourierType())
                .courierName(resolveCourierName(courier.getId()))
                .exceptionType(mappedExceptionType)
                .label(req.getUnavailabilityReason().name())
                .startDate(startDate)
                .endDate(endDate)
                .reason(req.getComment())
                .unavailabilityReason(req.getUnavailabilityReason())
                .estimatedDurationMinutes(req.getEstimatedDurationMinutes())
                .validationStatus(status)
                .isRestPeriod(true)
                .isActive(status == UnavailabilityValidationStatus.APPROVED_ACTIVE)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        CourierExceptionalSchedule saved = repository.save(entity);

        notifyCourierDeclarationSubmitted(courier, saved);

        if (status == UnavailabilityValidationStatus.PENDING_VALIDATION) {
            notifyAdminsForInternalProblem(saved);
        }

        if (status == UnavailabilityValidationStatus.APPROVED_ACTIVE && isCurrentlyActive(saved)) {
            applyUnavailable(courier);
            courierRepository.save(courier);
        }

        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierExceptionalScheduleDTO> getMyDeclarations(Long userId) {
        return getMyDeclarations(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierExceptionalScheduleDTO> getMyDeclarations(Long userId, UnavailabilityValidationStatus validationStatus) {
        Courier courier = resolveCourierByUserId(userId);
        return repository.findByCourierIdOrderByCreatedAtDesc(courier.getId())
                .stream()
                .filter(item -> validationStatus == null || item.getValidationStatus() == validationStatus)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO markAsAvailable(Long userId) {
        Courier courier = resolveCourierByUserId(userId);

        CourierExceptionalSchedule declaration = repository.findByCourierIdOrderByCreatedAtDesc(courier.getId())
                .stream()
                .filter(item -> item.getValidationStatus() == UnavailabilityValidationStatus.APPROVED_ACTIVE
                        || item.getValidationStatus() == UnavailabilityValidationStatus.PENDING_VALIDATION)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Aucune declaration active a cloturer"));

        declaration.setValidationStatus(UnavailabilityValidationStatus.RESOLVED_AVAILABLE);
        declaration.setResolvedAt(LocalDateTime.now());
        declaration.setIsActive(false);
        CourierExceptionalSchedule saved = repository.save(declaration);

        courier.setIsOnline(true);
        courier.setIsAvailable(true);
        if (courier.getCurrentDeliveryId() == null) {
            courier.setStatus(CourierStatus.AVAILABLE);
        }
        courierRepository.save(courier);

        return toDTO(saved);
    }

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO approveDeclaration(Long declarationId, CourierExceptionalScheduleDTO.ManagerDecisionRequest req) {
        CourierExceptionalSchedule declaration = repository.findById(declarationId)
                .orElseThrow(() -> new RuntimeException("Declaration introuvable avec l'ID: " + declarationId));

        if (declaration.getValidationStatus() != UnavailabilityValidationStatus.PENDING_VALIDATION) {
            throw new RuntimeException("Seules les declarations en attente peuvent etre approuvees");
        }

        // L'admin peut ajuster les champs de l'exception lors de l'approbation
        if (req != null) {
            if (req.getExceptionType() != null && !req.getExceptionType().isBlank()) {
                declaration.setExceptionType(req.getExceptionType());
            }
            if (req.getLabel() != null && !req.getLabel().isBlank()) {
                declaration.setLabel(req.getLabel());
            }
            if (req.getStartDate() != null) {
                declaration.setStartDate(req.getStartDate());
            }
            if (req.getEndDate() != null) {
                declaration.setEndDate(req.getEndDate());
            }
            if (req.getStartsAt() != null) {
                declaration.setStartsAt(req.getStartsAt());
            }
            if (req.getEndsAt() != null) {
                declaration.setEndsAt(req.getEndsAt());
            }
            if (req.getReason() != null) {
                declaration.setReason(req.getReason());
            }
            if (req.getIsRestPeriod() != null) {
                declaration.setIsRestPeriod(req.getIsRestPeriod());
            }
        }

        Long adminUserId = resolveCurrentAdminUserId();
        declaration.setValidationStatus(UnavailabilityValidationStatus.APPROVED_ACTIVE);
        declaration.setValidatorAdminId(adminUserId);
        declaration.setValidatorAdminName(resolveAdminName(adminUserId));
        declaration.setValidationComment(req != null ? req.getComment() : null);
        declaration.setValidatedAt(LocalDateTime.now());
        declaration.setIsActive(true);

        Courier courier = courierRepository.findById(declaration.getCourierId())
                .orElseThrow(() -> new RuntimeException("Livreur introuvable avec l'ID: " + declaration.getCourierId()));

        // Mettre offline seulement si l'exception couvre l'instant actuel
        if (isCurrentlyActive(declaration)) {
            applyUnavailable(courier);
            courierRepository.save(courier);
        }

        CourierExceptionalSchedule saved = repository.save(declaration);
        notifyCourierDeclarationDecision(courier, saved, true);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public CourierExceptionalScheduleDTO rejectDeclaration(Long declarationId, String managerComment) {
        CourierExceptionalSchedule declaration = repository.findById(declarationId)
                .orElseThrow(() -> new RuntimeException("Declaration introuvable avec l'ID: " + declarationId));

        if (declaration.getValidationStatus() != UnavailabilityValidationStatus.PENDING_VALIDATION) {
            throw new RuntimeException("Seules les declarations en attente peuvent etre rejetees");
        }

        Long adminUserId = resolveCurrentAdminUserId();
        declaration.setValidationStatus(UnavailabilityValidationStatus.REJECTED);
        declaration.setValidatorAdminId(adminUserId);
        declaration.setValidatorAdminName(resolveAdminName(adminUserId));
        declaration.setValidationComment(managerComment);
        declaration.setValidatedAt(LocalDateTime.now());
        declaration.setIsActive(false);

        Courier courier = courierRepository.findById(declaration.getCourierId())
            .orElseThrow(() -> new RuntimeException("Livreur introuvable avec l'ID: " + declaration.getCourierId()));
        CourierExceptionalSchedule saved = repository.save(declaration);
        notifyCourierDeclarationDecision(courier, saved, false);
        return toDTO(saved);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String mapReasonToExceptionType(UnavailabilityReason reason) {
        return switch (reason) {
            case PANNE   -> "PANNE";
            case CONGE   -> "CONGE";
            case ABSENT  -> "ABSENT";
            case RETARD  -> "RETARD";
            case NE_TRAVAILLE_PAS -> "NE_TRAVAILLE_PAS";
        };
    }

    private String resolveCourierName(Long courierId) {
        try {
            var userInfo = authServiceClient.getUserById(
                courierRepository.findById(courierId)
                    .map(c -> c.getUserId())
                    .orElse(null)
            );
            if (userInfo != null) {
                String name = ((userInfo.getFirstName() != null ? userInfo.getFirstName() : "") + " "
                             + (userInfo.getLastName()  != null ? userInfo.getLastName()  : "")).trim();
                if (!name.isEmpty()) return name;
            }
        } catch (Exception e) {
            log.warn("Cannot resolve courier name for id={}: {}", courierId, e.getMessage());
        }
        return "Livreur #" + courierId;
    }

    private Courier resolveCourierByUserId(Long userId) {
        return courierRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Livreur introuvable pour userId: " + userId));
    }

    /**
     * Vérifie si l'exception couvre l'instant actuel.
     * Si startsAt/endsAt sont définis → vérification horaire précise.
     * Sinon → vérification par date seulement (journée entière).
     */
    private boolean isCurrentlyActive(CourierExceptionalSchedule ex) {
        LocalDateTime now = LocalDateTime.now();
        if (ex.getStartsAt() != null && ex.getEndsAt() != null) {
            return !now.isBefore(ex.getStartsAt()) && !now.isAfter(ex.getEndsAt());
        }
        LocalDate today = now.toLocalDate();
        return !today.isBefore(ex.getStartDate()) && !today.isAfter(ex.getEndDate());
    }

    private void applyUnavailable(Courier courier) {
        courier.setIsAvailable(false);
        if (courier.getCurrentDeliveryId() == null) {
            courier.setStatus(CourierStatus.OFFLINE);
        }
    }

    private void notifyAdminsForInternalProblem(CourierExceptionalSchedule declaration) {
        try {
            notificationServiceClient.sendAdminBroadcast(
                    NotificationServiceClient.AdminBroadcastRequest.builder()
                            .type("INTERNAL_COURIER_PROBLEM_REPORTED")
                            .title("Signalement livreur interne")
                            .message("Un livreur interne a signale un probleme et attend votre confirmation.")
                            .data(Map.of(
                                    "declarationId", declaration.getId(),
                                    "courierId", declaration.getCourierId(),
                                    "courierName", declaration.getCourierName() == null ? "-" : declaration.getCourierName(),
                                    "reason", declaration.getUnavailabilityReason() == null ? "-" : declaration.getUnavailabilityReason().name(),
                                    "validationStatus", declaration.getValidationStatus() == null ? "-" : declaration.getValidationStatus().name()))
                            .build());
        } catch (Exception ex) {
            log.warn("Could not notify admins for internal courier declaration {}: {}", declaration.getId(), ex.getMessage());
        }
    }

    private void notifyCourierScheduleUpdated(Courier courier, CourierExceptionalSchedule declaration) {
        try {
            if (courier.getUserId() == null) return;
            String title = "Planning mis à jour";
            String message = "L'administration a ajouté ou modifié une exception sur votre planning : " + declaration.getLabel();

            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title(title)
                    .message(message)
                    .data(Map.of(
                        "action", "UNAVAILABILITY_DECLARATION_APPROVED", // Re-use approved action to trigger refresh
                        "declarationId", declaration.getId(),
                        "isApproved", true
                    ))
                    .channel("IN_APP")
                    .build());
        } catch (Exception ex) {
            log.warn("Could not notify courier {} for schedule update: {}", courier.getId(), ex.getMessage());
        }
    }

    private void notifyCourierDeclarationSubmitted(Courier courier, CourierExceptionalSchedule declaration) {
        try {
            if (courier.getUserId() == null) return;
            String submittedAtText = formatDateTime(declaration.getCreatedAt());
            String statusText = declaration.getValidationStatus() != null
                    ? declaration.getValidationStatus().name()
                    : "PENDING_VALIDATION";
            String title = "Signalement envoyé";
            String message = "Votre signalement a été soumis le " + submittedAtText + " (" + statusText + ").";

            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title(title)
                    .message(message)
                    .data(buildDeclarationData("UNAVAILABILITY_DECLARATION_SUBMITTED", declaration))
                    .channel("IN_APP")
                    .build());
        } catch (Exception ex) {
            log.warn("Could not notify courier {} for submitted declaration {}: {}",
                    courier.getId(), declaration.getId(), ex.getMessage());
        }
    }

    private void notifyCourierDeclarationDecision(Courier courier, CourierExceptionalSchedule declaration, boolean approved) {
        try {
            if (courier.getUserId() == null) return;
            String submittedAtText = formatDateTime(declaration.getCreatedAt());
            String decidedAtText = formatDateTime(declaration.getValidatedAt());
            String title = approved ? "Signalement accepté" : "Signalement refusé";
            String message = approved
                    ? "Votre signalement soumis le " + submittedAtText + " a été accepté le " + decidedAtText + "."
                    : "Votre signalement soumis le " + submittedAtText + " a été refusé le " + decidedAtText + ".";

            Map<String, Object> data = buildDeclarationData(
                    approved ? "UNAVAILABILITY_DECLARATION_APPROVED" : "UNAVAILABILITY_DECLARATION_REJECTED",
                    declaration);
            data.put("isApproved", approved);

            notificationServiceClient.sendNotification(NotificationServiceClient.SendNotificationRequest.builder()
                    .userId(courier.getUserId())
                    .type("COURIER")
                    .title(title)
                    .message(message)
                    .data(data)
                    .channel("IN_APP")
                    .build());
        } catch (Exception ex) {
            log.warn("Could not notify courier {} for decision on declaration {}: {}",
                    courier.getId(), declaration.getId(), ex.getMessage());
        }
    }

    private Map<String, Object> buildDeclarationData(String action, CourierExceptionalSchedule declaration) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("declarationId", declaration.getId());
        data.put("courierId", declaration.getCourierId());
        data.put("courierName", declaration.getCourierName());
        data.put("validationStatus", declaration.getValidationStatus() == null ? null : declaration.getValidationStatus().name());
        data.put("unavailabilityReason", declaration.getUnavailabilityReason() == null ? null : declaration.getUnavailabilityReason().name());
        data.put("comment", declaration.getReason());
        data.put("submittedAt", declaration.getCreatedAt() == null ? null : declaration.getCreatedAt().toString());
        data.put("startsAt", declaration.getStartsAt() == null ? null : declaration.getStartsAt().toString());
        data.put("endsAt", declaration.getEndsAt() == null ? null : declaration.getEndsAt().toString());
        data.put("validatedAt", declaration.getValidatedAt() == null ? null : declaration.getValidatedAt().toString());
        data.put("resolvedAt", declaration.getResolvedAt() == null ? null : declaration.getResolvedAt().toString());
        data.put("validationComment", declaration.getValidationComment());
        return data;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return formatDateTime(LocalDateTime.now());
        }
        return dateTime.format(NOTIF_TIME_FORMAT);
    }

    private Long resolveCurrentAdminUserId() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sa) {
                String header = sa.getRequest().getHeader("X-User-Id");
                if (header != null && !header.isBlank()) return Long.parseLong(header.trim());
            }
        } catch (Exception e) {
            log.warn("Cannot resolve admin user: {}", e.getMessage());
        }
        return null;
    }

    private String resolveAdminName(Long adminUserId) {
        if (adminUserId == null) return "Système";
        return adminRepository.findByUserId(adminUserId)
                .map(Admin::getFullName)
                .orElse("Admin #" + adminUserId);
    }

    private CourierExceptionalScheduleDTO toDTO(CourierExceptionalSchedule e) {
        return CourierExceptionalScheduleDTO.builder()
                .id(e.getId())
                .courierId(e.getCourierId())
                .courierName(e.getCourierName())
                .exceptionType(e.getExceptionType())
                .label(e.getLabel())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .reason(e.getReason())
                .unavailabilityReason(e.getUnavailabilityReason())
                .estimatedDurationMinutes(e.getEstimatedDurationMinutes())
                .courierType(e.getCourierType())
                .validationStatus(e.getValidationStatus())
                .validatorAdminId(e.getValidatorAdminId())
                .validatorAdminName(e.getValidatorAdminName())
                .validationComment(e.getValidationComment())
                .validatedAt(e.getValidatedAt())
                .resolvedAt(e.getResolvedAt())
                .startsAt(e.getStartsAt())
                .endsAt(e.getEndsAt())
                .isRestPeriod(e.getIsRestPeriod())
                .isActive(e.getIsActive())
                .adminName(e.getAdminName())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
