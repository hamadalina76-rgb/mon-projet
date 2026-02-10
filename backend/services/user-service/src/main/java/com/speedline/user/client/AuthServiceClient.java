package com.speedline.user.client;

import com.speedline.user.config.FeignConfig;
import com.speedline.user.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client pour communiquer avec Auth Service
 * Permet de récupérer les informations utilisateur
 */
@FeignClient(name = "auth-service", configuration = FeignConfig.class)
public interface AuthServiceClient {

    /**
     * Récupérer les informations d'un utilisateur par son ID
     * 
     * @param userId ID de l'utilisateur
     * @return UserInfoDTO avec les données utilisateur
     */
    @GetMapping("/users/{userId}")
    UserInfoDTO getUserById(@PathVariable("userId") Long userId);
}
