package com.speedline.partner.service.impl;

import com.speedline.partner.domain.MenuCategory;
import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductOption;
import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.dto.request.CreateMenuCategoryRequest;
import com.speedline.partner.dto.request.ReorderRequest;
import com.speedline.partner.dto.request.UpdateMenuCategoryRequest;
import com.speedline.partner.dto.response.*;
import com.speedline.partner.exception.ResourceNotFoundException;
import com.speedline.partner.repository.MenuCategoryRepository;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.repository.ProductOptionRepository;
import com.speedline.partner.repository.ProductRepository;
import com.speedline.partner.service.MenuCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implémentation de {@link MenuCategoryService}.
 *
 * Stratégie de cache :
 *   - Menu complet mis en cache avec la clé partnerId.
 *   - Toute écriture (create/update/delete/reorder) évince le cache.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MenuCategoryServiceImpl implements MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final PartnerRepository partnerRepository;

    // ========================= READ =========================

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getCategories(Long partnerId) {
        log.debug("getCategories partnerId={}", partnerId);
        return menuCategoryRepository.findByPartnerIdOrderByPositionAsc(partnerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========================= CREATE =======================

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public MenuCategoryResponse createCategory(Long partnerId, CreateMenuCategoryRequest request) {
        log.info("createCategory partnerId={} name={}", partnerId, request.getName());

        // Auto-position : max(position) + 1 si non fournie (TC-09)
        int nextPosition = resolveNextCategoryPosition(partnerId, request.getPosition());

        MenuCategory category = MenuCategory.builder()
                .partnerId(partnerId)
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .position(nextPosition)
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : Boolean.TRUE)
                .build();

        MenuCategory saved = menuCategoryRepository.save(category);
        log.info("MenuCategory created id={} position={}", saved.getId(), saved.getPosition());
        return toResponse(saved);
    }

    // ========================= UPDATE =======================

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public MenuCategoryResponse updateCategory(Long partnerId, Long categoryId,
                                               UpdateMenuCategoryRequest request) {
        log.info("updateCategory partnerId={} categoryId={}", partnerId, categoryId);

        MenuCategory category = findCategoryOrThrow(partnerId, categoryId);

        Optional.ofNullable(request.getName()).ifPresent(category::setName);
        Optional.ofNullable(request.getDescription()).ifPresent(category::setDescription);
        Optional.ofNullable(request.getImageUrl()).ifPresent(category::setImageUrl);
        Optional.ofNullable(request.getPosition()).ifPresent(category::setPosition);
        Optional.ofNullable(request.getIsVisible()).ifPresent(category::setIsVisible);

        return toResponse(menuCategoryRepository.save(category));
    }

    // ========================= DELETE =======================

    /**
     * TC-11 — Catégorie avec produits liés : soft-delete (isVisible=false).
     *         Catégorie sans produits : suppression physique.
     */
    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public void deleteCategory(Long partnerId, Long categoryId) {
        log.info("deleteCategory partnerId={} categoryId={}", partnerId, categoryId);

        MenuCategory category = findCategoryOrThrow(partnerId, categoryId);

        long linkedProducts = productRepository
                .countByPartnerIdAndCategoryIdAndStatusNot(partnerId, categoryId, ProductStatus.DELETED);

        if (linkedProducts > 0) {
            // Soft-delete — catégorie masquée mais produits intacts
            log.info("Soft-deleting category {} ({} products linked)", categoryId, linkedProducts);
            category.setIsVisible(false);
            menuCategoryRepository.save(category);
        } else {
            // Hard-delete — aucun produit lié
            log.info("Hard-deleting category {} (no products)", categoryId);
            menuCategoryRepository.delete(category);
        }
    }

    // ========================= REORDER ======================

    /**
     * TC-12 — Réordonne en mettant à jour chaque position puis retourne la liste triée.
     */
    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public List<MenuCategoryResponse> reorderCategories(Long partnerId, ReorderRequest request) {
        log.info("reorderCategories partnerId={} count={}", partnerId, request.getItems().size());

        request.getItems().forEach(item -> {
            MenuCategory cat = findCategoryOrThrow(partnerId, item.getId());
            cat.setPosition(item.getPosition());
            menuCategoryRepository.save(cat);
        });

        return menuCategoryRepository.findByPartnerIdOrderByPositionAsc(partnerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ========================= TOGGLE VISIBILITY ============

    /**
     * Bascule isVisible d'une catégorie.
     */
    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public MenuCategoryResponse toggleVisibility(Long partnerId, Long categoryId) {
        log.info("toggleVisibility partnerId={} categoryId={}", partnerId, categoryId);
        MenuCategory category = findCategoryOrThrow(partnerId, categoryId);
        category.setIsVisible(!Boolean.TRUE.equals(category.getIsVisible()));
        return toResponse(menuCategoryRepository.save(category));
    }

    // ========================= FULL MENU ====================

    /**
     * TC-16 — Menu complet : catégories visibles avec leurs produits actifs et groupes d'options.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "menus:full", key = "#partnerId")
    public FullMenuResponse buildFullMenu(Long partnerId) {
        log.debug("buildFullMenu partnerId={}", partnerId);

        String partnerName = partnerRepository.findById(partnerId)
                .map(p -> p.getBusinessName())
                .orElseThrow(() -> new ResourceNotFoundException("Partner", partnerId));

        List<MenuCategory> visibleCategories = menuCategoryRepository
                .findByPartnerIdOrderByPositionAsc(partnerId)
                .stream()
                .filter(MenuCategory::getIsVisible)
                .collect(Collectors.toList());

        List<FullMenuResponse.CategorySection> sections = visibleCategories.stream()
                .map(cat -> {
                    List<Product> activeProducts = productRepository
                            .findByPartnerIdAndCategoryIdAndStatusNotOrderByDisplayOrderAsc(
                                    partnerId, cat.getId(), ProductStatus.DELETED)
                            .stream()
                            .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                            .collect(Collectors.toList());

                    List<ProductResponse> productResponses = activeProducts.stream()
                            .map(this::toProductResponse)
                            .collect(Collectors.toList());

                    return FullMenuResponse.CategorySection.builder()
                            .category(toResponse(cat))
                            .products(productResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return FullMenuResponse.builder()
                .partnerId(partnerId)
                .partnerName(partnerName)
                .categories(sections)
                .build();
    }

    // ========================= MAPPING ======================

    private MenuCategoryResponse toResponse(MenuCategory cat) {
        return MenuCategoryResponse.builder()
                .id(cat.getId())
                .partnerId(cat.getPartnerId())
                .name(cat.getName())
                .description(cat.getDescription())
                .imageUrl(cat.getImageUrl())
                .position(cat.getPosition())
                .isVisible(cat.getIsVisible())
                .createdAt(cat.getCreatedAt())
                .updatedAt(cat.getUpdatedAt())
                .build();
    }

    private ProductResponse toProductResponse(Product p) {
        List<OptionGroupResponse> optionGroups = productOptionRepository
                .findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(p.getId())
                .stream()
                .map(og -> {
                    List<OptionResponse> opts = og.getValues().stream()
                            .filter(v -> Boolean.TRUE.equals(v.getIsAvailable()))
                            .map(v -> OptionResponse.builder()
                                    .id(v.getId())
                                    .groupId(og.getId())
                                    .name(v.getName())
                                    .priceModifier(v.getPriceModifier())
                                    .isDefault(v.getIsDefault())
                                    .isAvailable(v.getIsAvailable())
                                    .position(v.getDisplayOrder())
                                    .build())
                            .collect(Collectors.toList());

                    return OptionGroupResponse.builder()
                            .id(og.getId())
                            .productId(p.getId())
                            .name(og.getName())
                            .type(og.getType())
                            .isRequired(og.getIsRequired())
                            .minSelection(og.getMinSelection())
                            .maxSelection(og.getMaxSelection())
                            .position(og.getDisplayOrder())
                            .options(opts)
                            .build();
                })
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategoryId())
                .partnerId(p.getPartnerId())
                .name(p.getName())
                .description(p.getDescription())
                .imageUrl(p.getImage())
                .price(p.getPrice())
                .isAvailable(p.getIsAvailable())
                .isPopular(p.getIsPopular())
                .preparationTimeMin(p.getPreparationTime())
                .position(p.getDisplayOrder())
                .tags(p.getTags())
                .optionGroups(optionGroups)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    // ========================= HELPERS ======================

    private MenuCategory findCategoryOrThrow(Long partnerId, Long categoryId) {
        return menuCategoryRepository.findByIdAndPartnerId(categoryId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MenuCategory introuvable (id=" + categoryId + ", partnerId=" + partnerId + ")"));
    }

    /**
     * Calcule la prochaine position à utiliser.
     * Si la position est fournie → on l'utilise telle quelle.
     * Sinon → max(position) + 1, ou 1 s'il n'existe aucune catégorie. (TC-09/TC-18 pattern)
     */
    private int resolveNextCategoryPosition(Long partnerId, Integer requestedPosition) {
        if (requestedPosition != null) {
            return requestedPosition;
        }
        return menuCategoryRepository
                .findTopByPartnerIdOrderByPositionDesc(partnerId)
                .map(cat -> cat.getPosition() + 1)
                .orElse(1);
    }
}
