package com.speedline.user.service;

import com.speedline.user.dto.SettingsAuditLogDTO;

public interface SettingsAuditLogService {

    /**
     * Enregistre une action d'audit.
     *
     * @param action  WORKING_HOURS_UPDATED | APP_ENABLED | APP_DISABLED | GENERAL_SETTINGS_UPDATED
     * @param adminId userId (auth-service) de l'administrateur, peut être null
     * @param details description lisible du changement
     */
    void log(String action, Long adminId, String details);

    /** Retourne les 50 dernières entrées d'audit. */
    SettingsAuditLogDTO.LogListResponse getRecentLogs();

    /**
     * Recherche filtrée paginée côté backend.
     *
     * @param action    type exact (optionnel)
     * @param adminName sous-chaîne insensible à la casse (optionnel)
     * @param date      YYYY-MM-DD — jour exact (optionnel)
     * @param page      numéro de page 0-basé
     * @param size      taille de page
     */
    SettingsAuditLogDTO.PagedLogResponse getFilteredLogs(
            String action, String adminName, String date, int page, int size);
}
