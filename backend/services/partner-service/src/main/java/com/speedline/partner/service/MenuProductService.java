package com.speedline.partner.service;

import com.speedline.partner.dto.request.*;
import com.speedline.partner.dto.response.OptionGroupResponse;
import com.speedline.partner.dto.response.OptionResponse;
import com.speedline.partner.dto.response.ProductResponse;
import com.speedline.partner.dto.response.PromotionLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Page de produits avec filtres et pagination côté serveur.
     * @param status "all" | "available" | "unavailable" | "low_stock"
     */
    @Transactional(readOnly = true)
    Page<ProductResponse> getProductsPage(Long partnerId, String search, Long categoryId, String status, Pageable pageable);

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
     * Duplique un produit (TC-38) : copie le produit et ses groupes d'options/options.
     *
     * @return le nouveau produit créé (nom préfixé "Copy of ")
     * @throws com.speedline.partner.exception.ResourceNotFoundException si introuvable
     */
    @Transactional
    ProductResponse duplicateProduct(Long partnerId, Long productId);

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

    /**
     * Définit ou supprime le label et la date de fin de promotion pour une liste de produits.
     * Seuls les produits appartenant au partenaire sont mis à jour.
     *
     * @return liste des produits mis à jour
     */
    @Transactional
    List<ProductResponse> setPromotion(Long partnerId, List<Long> productIds, String promotionLabel,
                                      java.time.LocalDate promotionStartDate, java.time.LocalDate promotionEndDate, Integer discountPercentage);

    /**
     * Historique des promotions du partenaire (logs paginés et filtrés).
     *
     * @param partnerId identifiant du partenaire
     * @param pageable  pagination et tri (recommandé : Sort.by("appliedAt").descending())
     * @param search   recherche optionnelle sur nom produit ou label promo (ignoré si null/blank)
     * @param dateFrom date d'application au plus tôt (inclus, ignoré si null)
     * @param dateTo   date d'application au plus tard (inclus, ignoré si null)
     * @param productId filtre optionnel par id produit (ignoré si null)
     * @return page de {@link PromotionLogResponse}
     */
    Page<PromotionLogResponse> getPromotionLogs(Long partnerId, Pageable pageable,
                                                String search, java.time.LocalDate dateFrom, java.time.LocalDate dateTo,
                                                Long productId);

    /**
     * Exporte le menu en CSV (id, name, category, price, isAvailable, stock, description). TC-57.
     */
    @Transactional(readOnly = true)
    byte[] exportMenuCsv(Long partnerId);

    /**
     * TC-58 : Preview import CSV menu — parse le fichier et retourne les modifications détectées.
     */
    @Transactional(readOnly = true)
    com.speedline.partner.dto.response.ImportPreviewResponse importPreview(Long partnerId, org.springframework.web.multipart.MultipartFile file);

    /**
     * TC-59 : Confirm import CSV menu — applique les mises à jour, continue en cas d'erreur ligne, retourne rapport.
     */
    @Transactional
    com.speedline.partner.dto.response.ImportConfirmResult importConfirm(Long partnerId, org.springframework.web.multipart.MultipartFile file);

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
