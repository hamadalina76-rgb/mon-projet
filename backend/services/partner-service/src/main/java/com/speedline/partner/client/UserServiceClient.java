package com.speedline.partner.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign Client pour récupérer les informations des admins depuis user-service.
 */
@FeignClient(
    name = "user-service",
    url  = "${feign.client.user-service.url}"
)
public interface UserServiceClient {

    /**
     * Récupère les informations d'un admin par son userId (ID de l'utilisateur auth).
     * Le adminId stocké dans partner_change_logs est le userId JWT, pas l'ID primaire de la table admins.
     * Correspond à GET /v1/admins/by-user/{userId} dans user-service.
     *
     * @param userId ID de l'utilisateur (référence auth)
     * @return Map contenant au minimum la clé "fullName"
     */
    @GetMapping("/v1/admins/by-user/{userId}")
    Map<String, Object> getAdminByUserId(@PathVariable("userId") Long userId);
}
