package com.speedline.partner.repository;

import com.speedline.partner.domain.OptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour OptionValue (options unitaires à l'intérieur d'un groupe).
 */
@Repository
public interface OptionValueRepository extends JpaRepository<OptionValue, Long> {

    /** Valeurs d'un groupe triées par displayOrder. */
    List<OptionValue> findByOptionIdOrderByDisplayOrderAsc(Long optionId);

    /**
     * Dernière option d'un groupe (position la plus haute).
     * Utilisé pour l'auto-incrémentation de displayOrder (TC-18).
     */
    Optional<OptionValue> findTopByOptionIdOrderByDisplayOrderDesc(Long optionId);
}
