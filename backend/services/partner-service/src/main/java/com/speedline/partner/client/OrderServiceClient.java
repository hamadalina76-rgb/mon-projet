package com.speedline.partner.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign Client pour communiquer avec order-service.
 * Récupère les statistiques de commandes réelles pour les catégories.
 *
 * Priorité de résolution :
 * 1. Si ${feign.client.order-service.url} est défini → URL directe (pratique en dev local)
 * 2. Sinon → service discovery Eureka via name = "order-service"
 */
@FeignClient(
    name = "order-service",
    url  = "${feign.client.order-service.url:http://localhost:8084}"
)
public interface OrderServiceClient {

    /**
     * Stats journalières de commandes pour une liste de partenaires.
     * Correspond à GET /orders/internal/stats/daily dans order-service.
     *
     * @param partnerIds IDs des partenaires appartenant à la catégorie
     * @param days       Fenêtre temporelle en jours (défaut : 30)
     * @return Map date "yyyy-MM-dd" → nombre de commandes
     */
    @GetMapping("/orders/internal/stats/daily")
    Map<String, Long> getDailyStatsByPartners(
            @RequestParam("partnerIds") List<Long> partnerIds,
            @RequestParam(value = "days", defaultValue = "30") int days);
}
