package com.speedline.partner.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign Client pour communiquer avec auth-service
 * Communication synchrone pour les opérations critiques
 */
@FeignClient(
    name = "auth-service",
    path = "/internal/users"
)
public interface AuthServiceClient {

    /**
     * Met à jour le partner ID dans auth-service
     * 
     * @param userId ID de l'utilisateur
     * @param body Map contenant le partnerId
     * @return Response avec message de confirmation
     */
    @PutMapping("/{userId}/partner-id")
    ResponseEntity<Map<String, Object>> updatePartnerId(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Long> body
    );
}
