package com.speedline.user.controller;

import com.speedline.user.client.AuthServiceClient;
import com.speedline.user.dto.UserInfoDTO;
import com.speedline.user.dto.UserProfileUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Contrôleur pour la gestion des profils utilisateurs
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final AuthServiceClient authServiceClient;

    @Value("${file.upload.dir:c:/PFE2026/speedline/backend/services/user-service/uploads/profile-pictures}")
    private String uploadDir;

    @Value("${file.upload.base-url:http://10.0.2.2:8082/uploads}")
    private String baseUrl;

    /**
     * Upload d'une photo de profil
     */
    @PostMapping("/{userId}/profile-picture")
    public ResponseEntity<UserInfoDTO> uploadProfilePicture(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String requestUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("POST /users/{}/profile-picture - Upload photo de profil", userId);
        log.debug("Request headers: X-User-Id={}, X-User-Role={}", requestUserId, userRole);
        
        // Validate authorization: user can only update their own profile unless ADMIN
        validateUserAccess(userId, requestUserId, userRole);

        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("File must be an image");
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Save file
            Path targetLocation = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Build URL (use 10.0.2.2 for Android emulator)
            String fileUrl = baseUrl + "/" + uniqueFilename;

            log.info("Profile picture uploaded successfully: {}", fileUrl);

            // Update user profile picture in auth-service via Feign
            UserProfileUpdateRequest updateRequest = UserProfileUpdateRequest.builder()
                    .profilePicture(fileUrl)
                    .build();
            
            log.info("Calling auth-service to update profilePicture for userId={}, URL={}", userId, fileUrl);
            log.debug("Feign request payload: {}", updateRequest);
            
            UserInfoDTO response = authServiceClient.updateUserProfile(userId, updateRequest);
            log.info("Profile picture updated in auth-service: profilePicture={}", response.getProfilePicture());
            log.debug("Full response from auth-service: {}", response);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload profile picture", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Met à jour le profil utilisateur
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserInfoDTO> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("PUT /users/{} - Mise à jour du profil", userId);
        log.debug("Request headers: X-User-Id={}, X-User-Role={}", requestUserId, userRole);
        log.debug("Request body: {}", request);
        
        // Validate authorization: user can only update their own profile unless ADMIN
        validateUserAccess(userId, requestUserId, userRole);

        // Update user profile in auth-service via Feign
        log.info("Calling auth-service to update profile for userId={}, profilePicture in request={}", 
                userId, request.getProfilePicture());
        
        UserInfoDTO response = authServiceClient.updateUserProfile(userId, request);
        log.info("Profile updated in auth-service: profilePicture={}", response.getProfilePicture());
        log.debug("Full response from auth-service: {}", response);

        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les informations utilisateur
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoDTO> getUserInfo(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Id", required = false) String requestUserId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("GET /users/{} - Récupération des infos utilisateur", userId);
        log.debug("Request headers: X-User-Id={}, X-User-Role={}", requestUserId, userRole);
        
        // Validate authorization: user can only view their own profile unless ADMIN
        validateUserAccess(userId, requestUserId, userRole);
        
        UserInfoDTO userInfo = authServiceClient.getUserById(userId);
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * Validates that the requesting user has access to the target user's profile
     * Users can only access their own profile unless they have ADMIN role
     */
    private void validateUserAccess(Long targetUserId, String requestUserId, String userRole) {
        // Allow access if user is ADMIN
        if ("ADMIN".equals(userRole)) {
            log.debug("Access granted: User has ADMIN role");
            return;
        }
        
        // Check if requesting user matches target user
        if (requestUserId == null) {
            log.error("Access denied: X-User-Id header is missing");
            throw new RuntimeException("Unauthorized: User ID not found in request");
        }
        
        try {
            Long requestUserIdLong = Long.parseLong(requestUserId);
            if (!requestUserIdLong.equals(targetUserId)) {
                log.error("Access denied: User {} attempted to access user {}", requestUserId, targetUserId);
                throw new RuntimeException("Unauthorized: Cannot access another user's profile");
            }
            log.debug("Access granted: User {} accessing their own profile", requestUserId);
        } catch (NumberFormatException e) {
            log.error("Access denied: Invalid X-User-Id header value: {}", requestUserId);
            throw new RuntimeException("Unauthorized: Invalid user ID format");
        }
    }
}
