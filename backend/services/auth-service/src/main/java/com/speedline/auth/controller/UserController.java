package com.speedline.auth.controller;

import com.speedline.auth.domain.User;
import com.speedline.auth.dto.response.UserInfoResponse;
import com.speedline.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les opérations relatives aux utilisateurs
 * Endpoints internes pour communication inter-services
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    /**
     * Récupérer les informations d'un utilisateur par son ID
     * Endpoint interne pour les autres services
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserById(@PathVariable Long userId) {
        log.info("GET /users/{} - Récupération des infos utilisateur", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", userId);
                    return new RuntimeException("User not found with id: " + userId);
                });
        
        log.info("User found: {} {} ({})", user.getFirstName(), user.getLastName(), user.getEmail());
        
        UserInfoResponse response = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole().name())
                .build();
        
        log.info("Returning user info response: {}", response);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Liste tous les utilisateurs (pour debug)
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        log.info("GET /users - Liste de tous les utilisateurs");
        var users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return ResponseEntity.ok(users.stream()
                .map(u -> String.format("ID: %d, Email: %s, Name: %s %s, Role: %s", 
                        u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole()))
                .toList());
    }
}
