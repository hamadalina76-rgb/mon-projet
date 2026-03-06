package com.speedline.partner.repository;

import com.speedline.partner.domain.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour ProductOption (groupes d'options d'un produit).
 */
@Repository
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    /** Groupes d'options actifs d'un produit, triés par displayOrder. */
    List<ProductOption> findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(Long productId);

    /**
     * Dernier groupe d'options d'un produit (position la plus haute).
     * Utilisé pour l'auto-incrémentation de displayOrder (TC-18).
     */
    Optional<ProductOption> findTopByProductIdOrderByDisplayOrderDesc(Long productId);
}
