package com.speedline.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CourierScheduleDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long courierId;
        private Long templateId;
        private String templateName;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate templateEffectiveFrom;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate weekStartDate;
        private Boolean isPermanent;
        private Boolean isActive;
        private Map<DayOfWeek, List<ShiftDTO>> days;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveRequest {
        /** Optionnel : appliquer un template comme base */
        private Long templateId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate weekStartDate;
        private Boolean isPermanent;
        /** key = MONDAY…SUNDAY, value = shifts */
        private Map<DayOfWeek, List<ScheduleTemplateDTO.ShiftInput>> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopyDayRequest {
        private DayOfWeek sourceDay;
        private List<DayOfWeek> targetDays;
    }

    /**
     * Planning effectif pour une date : soit le planning hebdo normal,
     * soit une exception qui l'écrase.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EffectiveScheduleResponse {
        /** REGULAR | EXCEPTION_REST | EXCEPTION_SPECIAL */
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private DayOfWeek dayOfWeek;
        /** true = livreur ne travaille pas */
        private Boolean isRestDay;
        /** Rempli si status=REGULAR → les shifts du jour */
        private List<ShiftDTO> shifts;
        /** Rempli si status commence par EXCEPTION_ */
        private ExceptionInfo exception;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExceptionInfo {
        private Long id;
        private String exceptionType;
        private String label;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        private String reason;
        private Boolean isRestPeriod;
    }
}
