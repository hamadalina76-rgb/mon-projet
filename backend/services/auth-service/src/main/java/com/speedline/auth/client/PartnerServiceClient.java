package com.speedline.auth.client;

import com.speedline.auth.config.FeignConfig;
import com.speedline.auth.dto.request.CreatePartnerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client pour communiquer avec le Partner Service
 * Permet de créer des profils partenaires lors de l'inscription
 */
@FeignClient(name = "partner-service", configuration = FeignConfig.class)
public interface PartnerServiceClient {

    /**
     * Créer un profil partenaire initial dans le partner-service
     * 
     * @param request CreatePartnerRequest contenant userId
     * @return ResponseEntity avec le profil créé
     */
    @PostMapping("/partners/internal")
    ResponseEntity<?> createPartner(@RequestBody CreatePartnerRequest request);
}
