package com.speedline.user.service;

import com.speedline.user.dto.CourierScheduleAuditLogDTO;
import org.springframework.data.domain.Page;

/**
 * Service de traçabilité pour les plannings de livreurs.
 */
public interface CourierScheduleAuditLogService {

    /**
     * Enregistre une modification de planning.
     *
     * @param courierId    livreur concerné
     * @param action       type d'action (CREATED, UPDATED, TEMPLATE_APPLIED, DAY_COPIED, DELETED)
     * @param details      description textuelle de la modification
     * @param scheduleId   identifiant du planning (peut être null pour DELETED)
     * @param templateId   identifiant du template (si applicable)
     * @param templateName nom du template (si applicable)
     */
    void log(Long courierId, String action, String details,
             Long scheduleId, Long templateId, String templateName);

    /**
     * Retourne le journal d'audit paginé et filtré d'un livreur.
     *
     * @param courierId livreur concerné
     * @param page      numéro de page (0-based)
     * @param size      éléments par page
     * @param action    filtre sur le type d'action (null = tous)
     * @param dateFrom  date début inclusive au format yyyy-MM-dd (null = aucune borne)
     * @param dateTo    date fin inclusive au format yyyy-MM-dd (null = aucune borne)
     */
    Page<CourierScheduleAuditLogDTO.Response> getAuditLogs(
            Long courierId, int page, int size, String action, String dateFrom, String dateTo);
}
