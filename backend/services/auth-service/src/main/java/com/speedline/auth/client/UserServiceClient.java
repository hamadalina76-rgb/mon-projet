package com.speedline.auth.client;

import com.speedline.auth.config.FeignConfig;
import com.speedline.auth.dto.request.CreateCourierRequest;
import com.speedline.auth.dto.request.CreateCustomerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client pour communiquer avec le User Service
 * Permet de créer des profils clients et livreurs lors de l'inscription
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserServiceClient {

    /**
     * Créer un profil client dans le user-service
     * 
     * @param request CreateCustomerRequest contenant userId
     * @return ResponseEntity avec le profil créé
     */
    @PostMapping("/customers/internal")
    ResponseEntity<?> createCustomer(@RequestBody CreateCustomerRequest request);

    /**
     * Créer un profil livreur dans le user-service
     * 
     * @param request CreateCourierRequest contenant userId et vehicleType
     * @return ResponseEntity avec le profil créé
     */
    @PostMapping("/couriers/internal")
    ResponseEntity<?> createCourier(@RequestBody CreateCourierRequest request);
}
