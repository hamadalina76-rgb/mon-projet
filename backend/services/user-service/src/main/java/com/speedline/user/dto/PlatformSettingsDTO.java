package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public class PlatformSettingsDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralSettingsResponse {
        private String platformName;
        private String contactEmail;
        private String contactPhone;
        private String defaultLanguage;
        private String defaultCurrency;
        private boolean maintenanceMode;
        private boolean appEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneralSettingsRequest {
        private String platformName;
        private String contactEmail;
        private String contactPhone;
        private String defaultLanguage;
        private String defaultCurrency;
        private Boolean maintenanceMode;
        private Boolean appEnabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppStatusResponse {
        private boolean appEnabled;
        private boolean maintenanceMode;
        /** true when current time is within today's working hours */
        private boolean currentlyOpen;
        /** e.g. "MONDAY" */
        private String  currentDay;
        /** opening time "HH:mm" for today, or null */
        private String  todayOpenTime;
        /** closing time "HH:mm" for today, or null */
        private String  todayCloseTime;
        /** true if today is configured as a working day */
        private boolean todayIsOpen;
    }
}
