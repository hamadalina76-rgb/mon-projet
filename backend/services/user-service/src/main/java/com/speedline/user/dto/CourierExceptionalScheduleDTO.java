package com.speedline.user.dto;

import com.speedline.user.domain.CourierType;
import com.speedline.user.domain.UnavailabilityReason;
import com.speedline.user.domain.UnavailabilityValidationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour les plannings exceptionnels (ponctuels) des livreurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierExceptionalScheduleDTO {

    private Long id;
    private Long courierId;

    /** Prénom + Nom du livreur (résolu depuis CourierService) */
    private String courierName;

    /** JOUR_FERIE | EVENEMENT_SPECIAL | CONGE | FERMETURE | FORMATION */
    private String exceptionType;

    /** Libellé humain : "Aïd El Fitr", "Congé annuel"… */
    private String label;

    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private UnavailabilityReason unavailabilityReason;
    private Integer estimatedDurationMinutes;
    private CourierType courierType;
    private UnavailabilityValidationStatus validationStatus;
    private Long validatorAdminId;
    private String validatorAdminName;
    private String validationComment;
    private LocalDateTime validatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    /** true = pas de travail ; false = shifts spéciaux */
    private Boolean isRestPeriod;
    private Boolean isActive;

    private String adminName;
    private LocalDateTime createdAt;

    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotNull(message = "courierId obligatoire")
        private Long courierId;

        @NotBlank(message = "exceptionType obligatoire")
        private String exceptionType;

        @NotBlank(message = "label obligatoire")
        private String label;

        @NotNull(message = "startDate obligatoire")
        private LocalDate startDate;

        @NotNull(message = "endDate obligatoire")
        private LocalDate endDate;

        /** Heures précises (optionnel — si absent, l'exception couvre la journée entière) */
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;

        private String reason;

        @Builder.Default
        private Boolean isRestPeriod = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierDeclarationRequest {

        @NotNull(message = "unavailabilityReason obligatoire")
        private UnavailabilityReason unavailabilityReason;

        @Positive(message = "estimatedDurationMinutes doit etre positif")
        private Integer estimatedDurationMinutes;

        private String comment;

        private LocalDateTime startsAt;

        private LocalDateTime endsAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerDecisionRequest {

        private String comment;

        /** L'admin peut ajuster le type d'exception lors de l'approbation */
        private String exceptionType;

        /** Label ajusté */
        private String label;

        /** Dates ajustées */
        private LocalDate startDate;
        private LocalDate endDate;

        /** Heures précises (demi-journée / heures spécifiques) */
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;

        private String reason;
        private Boolean isRestPeriod;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverlapResult {
        private boolean hasOverlap;
        private List<CourierExceptionalScheduleDTO> overlapping;
    }
}
