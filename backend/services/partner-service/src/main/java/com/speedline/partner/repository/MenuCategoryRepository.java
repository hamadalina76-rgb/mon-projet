package com.speedline.partner.repository;

import com.speedline.partner.domain.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour MenuCategory.
 */
@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    /** Toutes les catégories d'un partenaire triées par position (TC-12). */
    List<MenuCategory> findByPartnerIdOrderByPositionAsc(Long partnerId);

    /** Récupération sécurisée : vérifie que la catégorie appartient au partenaire. */
    Optional<MenuCategory> findByIdAndPartnerId(Long id, Long partnerId);

    /**
     * Dernière position utilisée pour un partenaire.
     * Utilisé pour l'auto-incrémentation de position (TC-09, TC-18).
     */
    Optional<MenuCategory> findTopByPartnerIdOrderByPositionDesc(Long partnerId);

    /** Nombre de catégories visibles pour un partenaire. */
    long countByPartnerIdAndIsVisibleTrue(Long partnerId);
}
