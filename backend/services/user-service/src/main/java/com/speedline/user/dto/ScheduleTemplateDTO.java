package com.speedline.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class ScheduleTemplateDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private Boolean isActive;
        /** key = day name (MONDAY…SUNDAY), value = ordered list of shifts */
        private Map<DayOfWeek, List<ShiftDTO>> days;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        @Size(max = 100)
        private String name;

        @Size(max = 500)
        private String description;

        /** key = MONDAY…SUNDAY, value = list of shifts for that day */
        @NotNull
        private Map<DayOfWeek, List<ShiftInput>> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftInput {
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime breakStart;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime breakEnd;
    }
}
