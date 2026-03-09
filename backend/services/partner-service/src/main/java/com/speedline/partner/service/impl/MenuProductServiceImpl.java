package com.speedline.partner.service.impl;

import com.speedline.partner.domain.OptionValue;
import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductOption;
import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.domain.ProductStock;
import com.speedline.partner.dto.request.*;
import com.speedline.partner.dto.response.OptionGroupResponse;
import com.speedline.partner.dto.response.OptionResponse;
import com.speedline.partner.dto.response.ProductResponse;
import com.speedline.partner.exception.ResourceNotFoundException;
import com.speedline.partner.repository.*;
import com.speedline.partner.service.MenuProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation de {@link MenuProductService}.
 *
 * All writes evict the partner's full-menu cache.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MenuProductServiceImpl implements MenuProductService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OptionValueRepository optionValueRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    // ========================= PRODUCTS — READ ==============

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(Long partnerId, Long categoryId) {
        log.debug("getAllProducts partnerId={} categoryId={}", partnerId, categoryId);
        if (categoryId != null) {
            return productRepository
                    .findByPartnerIdAndCategoryIdAndStatusNotOrderByDisplayOrderAsc(
                            partnerId, categoryId, ProductStatus.DELETED)
                    .stream()
                    .map(this::toProductResponse)
                    .collect(Collectors.toList());
        }
        return productRepository
                .findByPartnerIdAndStatusNot(partnerId, ProductStatus.DELETED)
                .stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsPage(Long partnerId, String search, Long categoryId, String status, Pageable pageable) {
        log.debug("getProductsPage partnerId={} search={} categoryId={} status={}", partnerId, search, categoryId, status);
        Boolean isAvailable = null;
        List<Long> lowStockIds = null;
        if (status != null) {
            switch (status) {
                case "available" -> isAvailable = true;
                case "unavailable" -> isAvailable = false;
                case "low_stock" -> {
                    List<Long> partnerProductIds = productRepository.findProductIdsByPartnerId(partnerId);
                    if (partnerProductIds.isEmpty()) {
                        return Page.empty(pageable);
                    }
                    lowStockIds = productStockRepository.findProductIdsInLowStock(partnerProductIds);
                    if (lowStockIds.isEmpty()) {
                        return Page.empty(pageable);
                    }
                }
                default -> { /* all */ }
            }
        }
        Page<Product> page = productRepository
                .findProductsPage(partnerId, categoryId, search != null ? search.trim() : null, isAvailable, lowStockIds, pageable);
        List<Long> productIds = page.getContent().stream().map(Product::getId).toList();
        Map<Long, String> stockStatusMap = buildStockStatusMap(productIds);
        return page.map(p -> toProductResponse(p, stockStatusMap.get(p.getId())));
    }

    private Map<Long, String> buildStockStatusMap(List<Long> productIds) {
        Map<Long, String> map = new HashMap<>();
        if (productIds.isEmpty()) return map;
        List<ProductStock> stocks = productStockRepository.findByProductIdIn(productIds);
        for (ProductStock ps : stocks) {
            if (!Boolean.TRUE.equals(ps.getIsTrackingEnabled())) continue;
            int qty = ps.getQuantity() != null ? ps.getQuantity() : 0;
            int threshold = ps.getLowStockThreshold() != null ? ps.getLowStockThreshold() : 0;
            String status = qty <= 0 ? "OUT_OF_STOCK" : (qty <= threshold ? "LOW_STOCK" : "IN_STOCK");
            map.put(ps.getProductId(), status);
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long partnerId, Long categoryId) {
        log.debug("getProductsByCategory partnerId={} categoryId={}", partnerId, categoryId);

        // Verify category belongs to this partner
        menuCategoryRepository.findByIdAndPartnerId(categoryId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MenuCategory introuvable (id=" + categoryId + ", partnerId=" + partnerId + ")"));

        return productRepository
                .findByPartnerIdAndCategoryIdAndStatusNotOrderByDisplayOrderAsc(
                        partnerId, categoryId, ProductStatus.DELETED)
                .stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long partnerId, Long productId) {
        Product product = findProductOrThrow(partnerId, productId);
        return toProductResponse(product);
    }

    // ========================= PRODUCTS — WRITE =============

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public ProductResponse createProduct(Long partnerId, CreateProductRequest req) {
        log.info("createProduct partnerId={} name={}", partnerId, req.getName());

        int nextPos = resolveNextProductPosition(partnerId, req.getPosition());

        Product product = Product.builder()
                .partnerId(partnerId)
                .categoryId(req.getCategoryId())
                .name(req.getName())
                .description(req.getDescription())
                .image(req.getImageUrl())
                .price(req.getPrice())
                .isAvailable(req.getIsAvailable() != null ? req.getIsAvailable() : Boolean.TRUE)
                .isPopular(req.getIsPopular() != null ? req.getIsPopular() : Boolean.FALSE)
                .preparationTime(req.getPreparationTimeMin())
                .displayOrder(nextPos)
                .tags(req.getTags())
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created id={}", saved.getId());
        return toProductResponse(saved);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public ProductResponse updateProduct(Long partnerId, Long productId, UpdateProductRequest req) {
        log.info("updateProduct partnerId={} productId={}", partnerId, productId);

        Product product = findProductOrThrow(partnerId, productId);

        if (req.getName() != null)             product.setName(req.getName());
        if (req.getPrice() != null)            product.setPrice(req.getPrice());
        if (req.getCategoryId() != null)       product.setCategoryId(req.getCategoryId());
        if (req.getDescription() != null)      product.setDescription(req.getDescription());
        if (req.getImageUrl() != null)         product.setImage(req.getImageUrl());
        if (req.getIsAvailable() != null)      product.setIsAvailable(req.getIsAvailable());
        if (req.getIsPopular() != null)        product.setIsPopular(req.getIsPopular());
        if (req.getPreparationTimeMin() != null) product.setPreparationTime(req.getPreparationTimeMin());
        if (req.getPosition() != null)         product.setDisplayOrder(req.getPosition());
        if (req.getTags() != null)             product.setTags(req.getTags());

        return toProductResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public ProductResponse duplicateProduct(Long partnerId, Long productId) {
        log.info("duplicateProduct partnerId={} productId={}", partnerId, productId);
        Product source = findProductOrThrow(partnerId, productId);
        int nextPos = resolveNextProductPosition(partnerId, null);

        Product copy = Product.builder()
                .partnerId(partnerId)
                .categoryId(source.getCategoryId())
                .name("Copy of " + source.getName())
                .description(source.getDescription())
                .image(source.getImage())
                .price(source.getPrice())
                .isAvailable(true)
                .isPopular(false)
                .preparationTime(source.getPreparationTime())
                .displayOrder(nextPos)
                .tags(source.getTags())
                .status(ProductStatus.ACTIVE)
                .build();
        Product newProduct = productRepository.save(copy);

        List<ProductOption> sourceGroups = productOptionRepository
                .findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(source.getId());
        for (ProductOption oldGroup : sourceGroups) {
            ProductOption newGroup = ProductOption.builder()
                    .productId(newProduct.getId())
                    .name(oldGroup.getName())
                    .type(oldGroup.getType())
                    .isRequired(oldGroup.getIsRequired())
                    .minSelection(oldGroup.getMinSelection())
                    .maxSelection(oldGroup.getMaxSelection())
                    .displayOrder(oldGroup.getDisplayOrder())
                    .isActive(true)
                    .build();
            ProductOption savedGroup = productOptionRepository.save(newGroup);

            List<OptionValue> oldValues = optionValueRepository.findByOptionIdOrderByDisplayOrderAsc(oldGroup.getId());
            for (OptionValue oldVal : oldValues) {
                OptionValue newVal = OptionValue.builder()
                        .optionId(savedGroup.getId())
                        .name(oldVal.getName())
                        .priceModifier(oldVal.getPriceModifier() != null ? oldVal.getPriceModifier() : BigDecimal.ZERO)
                        .isDefault(false)
                        .isAvailable(oldVal.getIsAvailable() != null ? oldVal.getIsAvailable() : true)
                        .displayOrder(oldVal.getDisplayOrder())
                        .build();
                optionValueRepository.save(newVal);
            }
        }

        return toProductResponse(newProduct);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public void deleteProduct(Long partnerId, Long productId) {
        log.info("deleteProduct (soft) partnerId={} productId={}", partnerId, productId);
        Product product = findProductOrThrow(partnerId, productId);
        product.setStatus(ProductStatus.DELETED);
        product.setIsAvailable(false);
        productRepository.save(product);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public ProductResponse updateAvailability(Long partnerId, Long productId,
                                              UpdateAvailabilityRequest req) {
        log.info("updateAvailability partnerId={} productId={} isAvailable={}",
                partnerId, productId, req.getIsAvailable());
        Product product = findProductOrThrow(partnerId, productId);
        product.setIsAvailable(req.getIsAvailable());
        return toProductResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public List<ProductResponse> reorderProducts(Long partnerId, ReorderRequest req) {
        log.info("reorderProducts partnerId={} count={}", partnerId, req.getItems().size());

        req.getItems().forEach(item -> {
            Product p = findProductOrThrow(partnerId, item.getId());
            p.setDisplayOrder(item.getPosition());
            productRepository.save(p);
        });

        return productRepository
                .findByPartnerIdAndStatusNot(partnerId, ProductStatus.DELETED)
                .stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                        b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()))
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    // ========================= OPTION GROUPS — READ ==========

    @Override
    @Transactional(readOnly = true)
    public List<OptionGroupResponse> getOptionGroups(Long partnerId, Long productId) {
        log.debug("getOptionGroups partnerId={} productId={}", partnerId, productId);
        findProductOrThrow(partnerId, productId);
        return productOptionRepository
                .findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(productId)
                .stream()
                .map(g -> toGroupResponse(g, productId))
                .collect(Collectors.toList());
    }

    // ========================= OPTION GROUPS — WRITE ========

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public OptionGroupResponse createOptionGroup(Long partnerId, Long productId,
                                                 CreateOptionGroupRequest req) {
        log.info("createOptionGroup partnerId={} productId={} name={}", partnerId, productId, req.getName());

        findProductOrThrow(partnerId, productId);

        int nextPos = resolveNextGroupPosition(productId, req.getPosition());

        ProductOption group = ProductOption.builder()
                .productId(productId)
                .name(req.getName())
                .type(req.getType())
                .isRequired(req.getIsRequired() != null ? req.getIsRequired() : Boolean.FALSE)
                .minSelection(req.getMinSelection() != null ? req.getMinSelection() : 0)
                .maxSelection(req.getMaxSelection() != null ? req.getMaxSelection() : 1)
                .displayOrder(nextPos)
                .isActive(true)
                .build();

        ProductOption saved = productOptionRepository.save(group);
        return toGroupResponse(saved, productId);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public OptionGroupResponse updateOptionGroup(Long partnerId, Long productId, Long groupId,
                                                 UpdateOptionGroupRequest req) {
        log.info("updateOptionGroup partnerId={} groupId={}", partnerId, groupId);

        findProductOrThrow(partnerId, productId);
        ProductOption group = findGroupOrThrow(groupId, productId);

        if (req.getName() != null)         group.setName(req.getName());
        if (req.getType() != null)         group.setType(req.getType());
        if (req.getIsRequired() != null)   group.setIsRequired(req.getIsRequired());
        if (req.getMinSelection() != null) group.setMinSelection(req.getMinSelection());
        if (req.getMaxSelection() != null) group.setMaxSelection(req.getMaxSelection());
        if (req.getPosition() != null)     group.setDisplayOrder(req.getPosition());

        return toGroupResponse(productOptionRepository.save(group), productId);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public void deleteOptionGroup(Long partnerId, Long productId, Long groupId) {
        log.info("deleteOptionGroup partnerId={} groupId={}", partnerId, groupId);
        findProductOrThrow(partnerId, productId);
        ProductOption group = findGroupOrThrow(groupId, productId);
        // CascadeType.ALL on ProductOption.values → OptionValues deleted automatically
        productOptionRepository.delete(group);
    }

    // ========================= OPTIONS — WRITE ==============

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public OptionResponse createOption(Long partnerId, Long productId, Long groupId,
                                       CreateOptionRequest req) {
        log.info("createOption partnerId={} groupId={} name={}", partnerId, groupId, req.getName());

        findProductOrThrow(partnerId, productId);
        findGroupOrThrow(groupId, productId);

        int nextPos = resolveNextOptionPosition(groupId, req.getPosition());

        OptionValue option = OptionValue.builder()
                .optionId(groupId)
                .name(req.getName())
                .priceModifier(req.getPriceModifier() != null ? req.getPriceModifier() : BigDecimal.ZERO)
                .isDefault(req.getIsDefault() != null ? req.getIsDefault() : Boolean.FALSE)
                .isAvailable(req.getIsAvailable() != null ? req.getIsAvailable() : Boolean.TRUE)
                .displayOrder(nextPos)
                .build();

        OptionValue saved = optionValueRepository.save(option);
        return toOptionResponse(saved, groupId);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public OptionResponse updateOption(Long partnerId, Long optionId, UpdateOptionRequest req) {
        log.info("updateOption partnerId={} optionId={}", partnerId, optionId);

        OptionValue option = optionValueRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option", optionId));

        if (req.getName() != null)          option.setName(req.getName());
        if (req.getPriceModifier() != null) option.setPriceModifier(req.getPriceModifier());
        if (req.getIsDefault() != null)     option.setIsDefault(req.getIsDefault());
        if (req.getIsAvailable() != null)   option.setIsAvailable(req.getIsAvailable());
        if (req.getPosition() != null)      option.setDisplayOrder(req.getPosition());

        return toOptionResponse(optionValueRepository.save(option), option.getOptionId());
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public void deleteOption(Long partnerId, Long optionId) {
        log.info("deleteOption partnerId={} optionId={}", partnerId, optionId);
        OptionValue option = optionValueRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option", optionId));
        optionValueRepository.delete(option);
    }

    // ========================= MAPPING ======================

    ProductResponse toProductResponse(Product p) {
        List<OptionGroupResponse> optionGroups = productOptionRepository
                .findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(p.getId())
                .stream()
                .map(og -> toGroupResponse(og, p.getId()))
                .collect(Collectors.toList());

        return toProductResponse(p, null);
    }

    ProductResponse toProductResponse(Product p, String stockStatus) {
        List<OptionGroupResponse> optionGroups = productOptionRepository
                .findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(p.getId())
                .stream()
                .map(og -> toGroupResponse(og, p.getId()))
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
                .stockStatus(stockStatus)
                .build();
    }

    private OptionGroupResponse toGroupResponse(ProductOption og, Long productId) {
        List<OptionResponse> opts = og.getValues() != null
                ? og.getValues().stream()
                        .map(v -> toOptionResponse(v, og.getId()))
                        .collect(Collectors.toList())
                : List.of();

        return OptionGroupResponse.builder()
                .id(og.getId())
                .productId(productId)
                .name(og.getName())
                .type(og.getType())
                .isRequired(og.getIsRequired())
                .minSelection(og.getMinSelection())
                .maxSelection(og.getMaxSelection())
                .position(og.getDisplayOrder())
                .options(opts)
                .build();
    }

    private OptionResponse toOptionResponse(OptionValue v, Long groupId) {
        return OptionResponse.builder()
                .id(v.getId())
                .groupId(groupId)
                .name(v.getName())
                .priceModifier(v.getPriceModifier())
                .isDefault(v.getIsDefault())
                .isAvailable(v.getIsAvailable())
                .position(v.getDisplayOrder())
                .build();
    }

    // ========================= HELPERS ======================

    private Product findProductOrThrow(Long partnerId, Long productId) {
        return productRepository.findById(productId)
                .filter(p -> partnerId.equals(p.getPartnerId()))
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product introuvable (id=" + productId + ", partnerId=" + partnerId + ")"));
    }

    private ProductOption findGroupOrThrow(Long groupId, Long productId) {
        return productOptionRepository.findById(groupId)
                .filter(g -> productId.equals(g.getProductId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OptionGroup introuvable (id=" + groupId + ", productId=" + productId + ")"));
    }

    private int resolveNextProductPosition(Long partnerId, Integer requested) {
        if (requested != null) return requested;
        return productRepository
                .findTopByPartnerIdAndStatusNotOrderByDisplayOrderDesc(partnerId, ProductStatus.DELETED)
                .map(p -> (p.getDisplayOrder() == null ? 0 : p.getDisplayOrder()) + 1)
                .orElse(1);
    }

    private int resolveNextGroupPosition(Long productId, Integer requested) {
        if (requested != null) return requested;
        return productOptionRepository
                .findTopByProductIdOrderByDisplayOrderDesc(productId)
                .map(g -> (g.getDisplayOrder() == null ? 0 : g.getDisplayOrder()) + 1)
                .orElse(1);
    }

    private int resolveNextOptionPosition(Long groupId, Integer requested) {
        if (requested != null) return requested;
        return optionValueRepository
                .findTopByOptionIdOrderByDisplayOrderDesc(groupId)
                .map(v -> (v.getDisplayOrder() == null ? 0 : v.getDisplayOrder()) + 1)
                .orElse(1);
    }
}
