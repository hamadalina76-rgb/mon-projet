package com.speedline.auth.repository;

import com.speedline.auth.domain.AuthProvider;
import com.speedline.auth.domain.Role;
import com.speedline.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Recherche d'utilisateurs par nom, email ou téléphone (utilisé par admin pour recherche clients).
     * @param q Terme de recherche (insensible à la casse pour nom/email)
     * @param role Rôle filtré (ex: CUSTOMER)
     * @return Liste des IDs utilisateur correspondants
     */
    @Query("SELECT u.id FROM User u WHERE u.role = :role AND (" +
           "LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "COALESCE(u.phoneNumber, '') LIKE CONCAT('%', :q, '%'))")
    List<Long> findUserIdsBySearchAndRole(@Param("q") String q, @Param("role") Role role);
}
