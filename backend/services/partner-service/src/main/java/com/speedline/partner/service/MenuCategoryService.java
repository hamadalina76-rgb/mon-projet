package com.speedline.partner.service;

import com.speedline.partner.dto.request.CreateMenuCategoryRequest;
import com.speedline.partner.dto.request.ReorderRequest;
import com.speedline.partner.dto.request.UpdateMenuCategoryRequest;
import com.speedline.partner.dto.response.FullMenuResponse;
import com.speedline.partner.dto.response.MenuCategoryResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des catégories de menu d'un partenaire.
 *
 * Couvre les APIs :
 *   GET  /api/partners/{id}/menu/categories
 *   POST /api/partners/{id}/menu/categories
 *   PUT  /api/partners/{id}/menu/categories/{catId}
 *   DEL  /api/partners/{id}/menu/categories/{catId}
 *   PAT  /api/partners/{id}/menu/categories/reorder
 *   GET  /api/partners/{id}/menu  (menu complet)
 */
public interface MenuCategoryService {

    /**
     * Retourne les catégories d'un partenaire, triées par position.
     * TC-12 : ordre respecté après réordonnancement.
     */
    @Transactional(readOnly = true)
    List<MenuCategoryResponse> getCategories(Long partnerId);

    /**
     * Crée une nouvelle catégorie.
     * TC-09 : HTTP 201, id retourné, position auto = dernière.
     */
    @Transactional
    MenuCategoryResponse createCategory(Long partnerId, CreateMenuCategoryRequest request);

    /**
     * Modifie une catégorie existante.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    MenuCategoryResponse updateCategory(Long partnerId, Long categoryId, UpdateMenuCategoryRequest request);

    /**
     * Supprime ou masque une catégorie.
     * TC-11 : si des produits sont liés → soft-delete (isVisible=false).
     *          Si aucun produit → suppression physique.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    void deleteCategory(Long partnerId, Long categoryId);

    /**
     * Réordonne les catégories selon la liste fournie.
     * TC-12 : [3,1,2] → ordre mis à jour, retourné dans le bon ordre.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si un id est inconnu
     */
    @Transactional
    List<MenuCategoryResponse> reorderCategories(Long partnerId, ReorderRequest request);

    /**
     * Bascule la visibilité d'une catégorie (isVisible toggle).
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    MenuCategoryResponse toggleVisibility(Long partnerId, Long categoryId);

    /**
     * Construit le menu complet structuré, consommé par l'app client.
     * TC-16 : structure imbriquée complète.
     */
    @Transactional(readOnly = true)
    FullMenuResponse buildFullMenu(Long partnerId);
}
