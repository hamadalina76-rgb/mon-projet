package com.speedline.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AppWorkingHoursDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {
        private String dayOfWeek;   // MONDAY..SUNDAY
        @JsonProperty("isOpen")
        private boolean isOpen;
        private String openTime;    // "HH:mm" or null
        private String closeTime;   // "HH:mm" or null
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyScheduleResponse {
        private List<DaySchedule> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateWeeklyScheduleRequest {
        private List<DaySchedule> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayStatusResponse {
        /** e.g. "MONDAY" */
        private String currentDay;
        /** true if today is configured as an open day */
        private boolean todayIsOpen;
        /** opening time "HH:mm" or null */
        private String openTime;
        /** closing time "HH:mm" or null */
        private String closeTime;
        /** true when now is within [openTime, closeTime) */
        private boolean currentlyOpen;
    }
}
