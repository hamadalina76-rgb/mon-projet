package com.speedline.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

        private String reason;

        @Builder.Default
        private Boolean isRestPeriod = true;
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
