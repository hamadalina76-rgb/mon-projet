package com.speedline.location.repository;

import com.speedline.location.domain.CustomerLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les adresses clients.
 */
@Repository
public interface CustomerLocationRepository extends JpaRepository<CustomerLocation, Long> {

    /** Toutes les adresses d'un utilisateur, les plus récentes en premier */
    List<CustomerLocation> findByUserIdOrderBySavedAtDesc(String userId);

    /** Adresse par défaut d'un utilisateur */
    Optional<CustomerLocation> findByUserIdAndIsDefaultTrue(String userId);

    /**
     * Avant d'en définir une nouvelle comme défaut, on enlève le flag
     * sur les adresses précédentes du même utilisateur.
     */
    @Modifying
    @Transactional
    @Query("UPDATE CustomerLocation cl SET cl.isDefault = false WHERE cl.userId = :userId")
    void clearDefaultForUser(@Param("userId") String userId);
}
