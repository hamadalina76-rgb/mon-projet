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
}
