package com.speedline.partner.service;

import com.speedline.partner.dto.request.*;
import com.speedline.partner.dto.response.OptionGroupResponse;
import com.speedline.partner.dto.response.OptionResponse;
import com.speedline.partner.dto.response.ProductResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des produits, groupes d'options et options du menu d'un partenaire.
 *
 * Couvre les APIs :
 *   GET   /api/partners/{id}/menu/categories/{catId}/products
 *   POST  /api/partners/{id}/menu/products
 *   PUT   /api/partners/{id}/menu/products/{productId}
 *   DEL   /api/partners/{id}/menu/products/{productId}
 *   PATCH /api/partners/{id}/menu/products/{productId}/availability
 *   PATCH /api/partners/{id}/menu/products/reorder
 *   POST  /api/partners/{id}/menu/products/{productId}/option-groups
 *   PUT   .../option-groups/{groupId}
 *   DEL   .../option-groups/{groupId}
 *   POST  .../option-groups/{groupId}/options
 *   PUT   /api/partners/{id}/menu/options/{optionId}
 *   DEL   /api/partners/{id}/menu/options/{optionId}
 */
public interface MenuProductService {

    // ===================== PRODUCTS =====================

    /**
     * Tous les produits d'un partenaire, filtrés optionnellement par catégorie.
     * Si {@code categoryId} est null, retourne tous les produits actifs.
     */
    @Transactional(readOnly = true)
    List<ProductResponse> getAllProducts(Long partnerId, Long categoryId);

    /**
     * Produits d'une catégorie, triés par position.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si la catégorie n'existe pas
     */
    @Transactional(readOnly = true)
    List<ProductResponse> getProductsByCategory(Long partnerId, Long categoryId);

    /**
     * Détail d'un produit par id (vérifie partnerId).
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional(readOnly = true)
    ProductResponse getProduct(Long partnerId, Long productId);

    /**
     * Crée un produit.
     * TC-10 : HTTP 201, lié à la catégorie, price retourné.
     */
    @Transactional
    ProductResponse createProduct(Long partnerId, CreateProductRequest request);

    /**
     * Modifie un produit.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    ProductResponse updateProduct(Long partnerId, Long productId, UpdateProductRequest request);

    /**
     * Soft-delete d'un produit (status = DELETED).
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    void deleteProduct(Long partnerId, Long productId);

    /**
     * Toggle disponibilité d'un produit.
     * TC-13 : isAvailable=false → produit non commandable côté client.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    ProductResponse updateAvailability(Long partnerId, Long productId, UpdateAvailabilityRequest request);

    /**
     * Réordonne les produits du partenaire.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si un id est inconnu
     */
    @Transactional
    List<ProductResponse> reorderProducts(Long partnerId, ReorderRequest request);

    // ===================== OPTION GROUPS =====================

    /**
     * Retourne tous les groupes d'options actifs d'un produit, triés par position.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si le produit n'existe pas
     */
    @Transactional(readOnly = true)
    List<OptionGroupResponse> getOptionGroups(Long partnerId, Long productId);

    /**
     * Crée un groupe d'options pour un produit.
     * TC-14 : SINGLE minSelection=1 maxSelection=1.
     * TC-15 : MULTIPLE minSelection=0 maxSelection=3.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si le produit n'existe pas
     */
    @Transactional
    OptionGroupResponse createOptionGroup(Long partnerId, Long productId, CreateOptionGroupRequest request);

    /**
     * Modifie un groupe d'options.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    OptionGroupResponse updateOptionGroup(Long partnerId, Long productId, Long groupId, UpdateOptionGroupRequest request);

    /**
     * Supprime un groupe d'options et toutes ses options en cascade.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    void deleteOptionGroup(Long partnerId, Long productId, Long groupId);

    // ===================== OPTIONS =====================

    /**
     * Ajoute une option à un groupe.
     * TC-17 : priceModifier=0 accepté sans erreur.
     * TC-18 : position auto = max_position + 1.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si le groupe n'existe pas
     */
    @Transactional
    OptionResponse createOption(Long partnerId, Long productId, Long groupId, CreateOptionRequest request);

    /**
     * Modifie une option.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    OptionResponse updateOption(Long partnerId, Long optionId, UpdateOptionRequest request);

    /**
     * Supprime une option.
     *
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    void deleteOption(Long partnerId, Long optionId);
}
