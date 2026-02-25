package com.speedline.auth.controller;

import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import com.speedline.auth.dto.request.UpdateUserRequest;
import com.speedline.auth.dto.response.UserInfoResponse;
import com.speedline.auth.repository.UserRepository;
import com.speedline.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur pour les opérations relatives aux utilisateurs
 * Endpoints internes pour communication inter-services
 */
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Recherche d'utilisateurs par nom, email ou téléphone (pour admin - recherche clients).
     * GET /api/v1/auth/users/search?q=...&role=CUSTOMER
     */
    @GetMapping("/search")
    public ResponseEntity<List<Long>> searchUserIds(
            @RequestParam String q,
            @RequestParam(defaultValue = "CUSTOMER") String role) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        Role r = Role.valueOf(role);
        List<Long> ids = userRepository.findUserIdsBySearchAndRole(q.trim(), r);
        return ResponseEntity.ok(ids);
    }

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
     * Supprime un utilisateur (appelé par user-service lors de la suppression d'un admin)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("DELETE /users/{} - Suppression du compte utilisateur", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found for deletion with id: {}", userId);
                    return new RuntimeException("User not found with id: " + userId);
                });
        
        userRepository.delete(user);
        log.info("User deleted successfully: {} ({})", user.getEmail(), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change le statut d'un utilisateur (ACTIVE, SUSPENDED, DELETED)
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        log.info("PUT /users/{}/status?status={}", userId, status);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setStatus(com.speedline.auth.domain.UserStatus.valueOf(status));
        userRepository.save(user);
        log.info("User status changed to {} for user {}", status, userId);
        return ResponseEntity.ok().build();
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
    
    /**
     * Met à jour le profil utilisateur (endpoint interne pour Feign)
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("PUT /users/{} - Mise à jour du profil (internal)", userId);
        UserInfoResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
