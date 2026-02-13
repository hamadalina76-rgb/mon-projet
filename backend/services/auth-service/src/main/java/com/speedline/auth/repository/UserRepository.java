package com.speedline.auth.repository;

import com.speedline.auth.domain.AuthProvider;
import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Optional<User> findByVerificationToken(String token);
    
    Optional<User> findByResetPasswordToken(String token);
    
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    // OAuth2 queries
    Optional<User> findByAuthProviderAndProviderUserId(AuthProvider authProvider, String providerUserId);
    
    boolean existsByAuthProviderAndProviderUserId(AuthProvider authProvider, String providerUserId);
    
    // Count by role
    long countByRole(Role role);
}
