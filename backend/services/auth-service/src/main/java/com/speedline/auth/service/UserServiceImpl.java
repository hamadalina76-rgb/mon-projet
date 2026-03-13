package com.speedline.auth.service;

import com.speedline.auth.domain.User;
import com.speedline.auth.dto.request.UpdateUserRequest;
import com.speedline.auth.dto.response.UserInfoResponse;
import com.speedline.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service utilisateur
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserInfoResponse updateUserProfile(Long userId, UpdateUserRequest request) {
        log.info("Updating user profile for userId: {}", userId);
        log.debug("Received UpdateUserRequest: {}", request);
        log.info("Request fields - firstName: {}, lastName: {}, email: {}, phoneNumber: {}, profilePicture: {}", 
                request.getFirstName(), request.getLastName(), request.getEmail(), 
                request.getPhoneNumber(), request.getProfilePicture());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        log.info("Current user profile picture before update: {}", user.getProfilePicture());

        // Update only provided fields
        if (request.getFirstName() != null) {
            log.debug("Updating firstName: {} -> {}", user.getFirstName(), request.getFirstName());
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            log.debug("Updating lastName: {} -> {}", user.getLastName(), request.getLastName());
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            log.debug("Updating email: {} -> {}", user.getEmail(), request.getEmail());
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            log.debug("Updating phoneNumber: {} -> {}", user.getPhoneNumber(), request.getPhoneNumber());
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getProfilePicture() != null) {
            log.info("Updating profilePicture: {} -> {}", user.getProfilePicture(), request.getProfilePicture());
            user.setProfilePicture(request.getProfilePicture());
        } else {
            log.info("ProfilePicture field is NULL in request, keeping existing value: {}", user.getProfilePicture());
        }

        user = userRepository.save(user);
        log.info("User profile updated successfully for userId: {}, final profilePicture: {}", userId, user.getProfilePicture());

        UserInfoResponse response = buildUserInfoResponse(user);
        log.debug("Returning UserInfoResponse: {}", response);
        
        return response;
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        log.info("Getting user info for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return buildUserInfoResponse(user);
    }

    private UserInfoResponse buildUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole().name())
                .isEmailVerified(Boolean.TRUE.equals(user.getIsEmailVerified()))
                .build();
    }
}
