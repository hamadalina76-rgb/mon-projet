package com.speedline.user.dto;

/**
 * DTOs pour les entrées du journal d'audit des plannings de livreurs.
 */
public class CourierScheduleAuditLogDTO {

    public record Response(
            Long   id,
            Long   courierId,
            Long   scheduleId,
            String action,
            Long   templateId,
            String templateName,
            String details,
            Long   adminId,
            String adminName,
            String createdAt
    ) {}
}
