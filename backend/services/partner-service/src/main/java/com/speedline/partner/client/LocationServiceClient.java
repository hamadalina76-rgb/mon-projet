package com.speedline.partner.client;

import com.speedline.partner.dto.ZoneInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign Client pour Location Service
 */
@FeignClient(name = "location-service", url = "${location.service.url:http://localhost:8088}")
public interface LocationServiceClient {

    /** Récupère toutes les zones actives depuis le location-service */
    @GetMapping("/zones/active")
    List<ZoneInfoDTO> getActiveZones();

    /** Récupère une zone par son ID (actif ou inactif) */
    @GetMapping("/zones/{id}")
    ZoneInfoDTO getZoneById(@PathVariable("id") Long id);
}
