package com.speedline.user.service;

import com.speedline.user.dto.CourierChangeLogDTO;
import org.springframework.data.domain.Page;

/**
 * Service de traçabilité pour les actions admin sur les livreurs.
 */
public interface CourierChangeLogService {

    /**
     * Enregistre une action admin sur un livreur.
     * L'adminId et adminName sont résolus depuis le contexte de la requête HTTP.
     */
    void log(String action, Long courierId,
             String statusBefore, String statusAfter,
             String courierTypeBefore, String courierTypeAfter,
             String zoneIdsBefore, String zoneIdsAfter,
             String description, String reason);

    /**
     * Retourne l'historique paginé des modifications d'un livreur.
     */
    Page<CourierChangeLogDTO> getChangeLogs(Long courierId, int page, int size);
}
