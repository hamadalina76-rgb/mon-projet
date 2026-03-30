package com.speedline.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class SettingsAuditLogDTO {

    @Getter
    @Builder
    public static class LogEntry {
        private Long   id;
        private String action;
        private Long   adminId;
        private String adminName;
        private String details;
        /** ISO-8601 datetime string */
        private String createdAt;
    }

    /** Réponse simple (legacy) */
    @Getter
    @Builder
    public static class LogListResponse {
        private List<LogEntry> logs;
    }

    /** Réponse paginée avec métadonnées */
    @Getter
    @Builder
    public static class PagedLogResponse {
        private List<LogEntry> logs;
        private long   totalElements;
        private int    totalPages;
        private int    page;
        private int    size;
    }
}
