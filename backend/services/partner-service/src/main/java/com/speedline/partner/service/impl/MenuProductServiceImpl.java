package com.speedline.partner.service.impl;

import com.speedline.partner.domain.MenuCategory;
import com.speedline.partner.domain.OptionValue;
import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductOption;
import com.speedline.partner.domain.ProductModerationStatus;
import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.domain.ProductStock;
import com.speedline.partner.domain.PromotionLog;
import com.speedline.partner.domain.ProductHistoryBackup;
import com.speedline.partner.domain.Partner;
import com.speedline.partner.event.PartnerEvent;
import com.speedline.partner.event.PartnerEventPublisher;
import com.speedline.partner.dto.request.*;
import com.speedline.partner.dto.response.ImportConfirmResult;
import com.speedline.partner.dto.response.ImportPreviewResponse;
import com.speedline.partner.dto.response.OptionGroupResponse;
import com.speedline.partner.dto.response.OptionResponse;
import com.speedline.partner.dto.response.ProductResponse;
import com.speedline.partner.dto.response.PromotionLogResponse;
import com.speedline.partner.exception.ResourceNotFoundException;
import com.speedline.partner.repository.*;
import com.speedline.partner.service.AuditLogService;
import com.speedline.partner.service.MenuProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;
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
    private final PromotionLogRepository promotionLogRepository;
    private final ProductHistoryBackupRepository productHistoryBackupRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerEventPublisher partnerEventPublisher;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public Page<ProductResponse> getProductsPage(
            Long partnerId,
            String search,
            Long categoryId,
            String status,
            String moderationStatus,
            Pageable pageable) {
        log.debug("getProductsPage partnerId={} search={} categoryId={} status={} moderationStatus={}",
                partnerId, search, categoryId, status, moderationStatus);
        Boolean isAvailable = null;
        List<Long> lowStockIds = null;
        ProductModerationStatus moderationFilter = null;

        if (moderationStatus != null && !moderationStatus.isBlank() && !"ALL".equalsIgnoreCase(moderationStatus)) {
            try {
                moderationFilter = ProductModerationStatus.valueOf(moderationStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid moderationStatus: " + moderationStatus);
            }
        }
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
                .findProductsPage(
                        partnerId,
                        categoryId,
                        search != null ? search.trim() : null,
                        isAvailable,
                        moderationFilter,
                        lowStockIds,
                        pageable);
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

        boolean allowDirectEdits = isProductAutoApprovalEnabled(partnerId);
        ProductModerationStatus initialModerationStatus = allowDirectEdits
                ? ProductModerationStatus.APPROVED
                : ProductModerationStatus.PENDING;

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
                .moderationStatus(initialModerationStatus)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created id={}", saved.getId());

        auditLogService.logWithChanges(
                null,
                "PRODUCT_SUBMITTED",
                "PRODUCT",
                saved.getId(),
                null,
                buildProductSnapshot(saved),
                allowDirectEdits ? "Created by partner, auto-approved" : "Created by partner, pending moderation"
        );
        saveProductHistoryBackup(
                saved.getPartnerId(),
                saved.getId(),
                "PRODUCT_SUBMITTED",
                "PARTNER",
                null,
                null,
                buildProductSnapshot(saved),
                null
        );

        if (allowDirectEdits) {
            // Record an APPROVE entry so UI knows there is no pending diff to review.
            saveProductHistoryBackup(
                    saved.getPartnerId(),
                    saved.getId(),
                    "APPROVE",
                    "SYSTEM",
                    null,
                    "{\"moderationStatus\":\"PENDING\"}",
                    "{\"moderationStatus\":\"APPROVED\"}",
                    null
            );
            publishProductDecisionEvent(
                    saved,
                    ProductModerationStatus.APPROVED,
                    null,
                    PartnerEvent.EventType.PRODUCT_APPROVED
            );
        } else {
            publishProductModerationRequest(partnerId, saved);
        }
        return toProductResponse(saved);
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public ProductResponse updateProduct(Long partnerId, Long productId, UpdateProductRequest req) {
        log.info("updateProduct partnerId={} productId={}", partnerId, productId);

        Product product = findProductOrThrow(partnerId, productId);
        String beforeSnapshot = buildProductSnapshot(product);
        boolean allowDirectEdits = isProductAutoApprovalEnabled(partnerId);

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

        // Re-moderation for any product change unless partner is allowed to auto-approve.
        product.setModerationStatus(allowDirectEdits ? ProductModerationStatus.APPROVED : ProductModerationStatus.PENDING);
        product.setModerationReason(null);

        Product saved = productRepository.save(product);

        auditLogService.logWithChanges(
                null,
                "PRODUCT_RESUBMITTED",
                "PRODUCT",
                saved.getId(),
                beforeSnapshot,
                buildProductSnapshot(saved),
                allowDirectEdits ? "Updated by partner, auto-approved" : "Updated by partner, pending moderation"
        );
        saveProductHistoryBackup(
                saved.getPartnerId(),
                saved.getId(),
                "PRODUCT_RESUBMITTED",
                "PARTNER",
                null,
                beforeSnapshot,
                buildProductSnapshot(saved),
                null
        );

        if (allowDirectEdits) {
            saveProductHistoryBackup(
                    saved.getPartnerId(),
                    saved.getId(),
                    "APPROVE",
                    "SYSTEM",
                    null,
                    "{\"moderationStatus\":\"PENDING\"}",
                    "{\"moderationStatus\":\"APPROVED\"}",
                    null
            );
            publishProductDecisionEvent(
                    saved,
                    ProductModerationStatus.APPROVED,
                    null,
                    PartnerEvent.EventType.PRODUCT_APPROVED
            );
        } else {
            publishProductModerationRequest(partnerId, saved);
        }
        return toProductResponse(saved);
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

    // ========================= MODERATION (admin) =========================

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getPendingProducts(Pageable pageable) {
        return productRepository
                .findByModerationStatusAndStatusNot(
                        ProductModerationStatus.PENDING,
                        ProductStatus.DELETED,
                        pageable
                )
                .map(this::toProductResponse);
    }

    @Override
    @CacheEvict(value = "menus:full", allEntries = true)
    public ProductResponse approveProduct(Long productId, Long adminId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new IllegalStateException("Cannot approve a deleted product");
        }

        if (product.getModerationStatus() != ProductModerationStatus.PENDING) {
            throw new IllegalStateException("Product is not pending moderation");
        }

        product.setModerationStatus(ProductModerationStatus.APPROVED);
        product.setModerationReason(null);

        Product saved = productRepository.save(product);

        // Audit history
        auditLogService.logWithChanges(
                adminId,
                "APPROVE",
                "PRODUCT",
                productId,
                "{\"moderationStatus\":\"PENDING\"}",
                "{\"moderationStatus\":\"APPROVED\"}",
                null
        );
        saveProductHistoryBackup(
                saved.getPartnerId(),
                saved.getId(),
                "APPROVE",
                "ADMIN",
                adminId,
                "{\"moderationStatus\":\"PENDING\"}",
                "{\"moderationStatus\":\"APPROVED\"}",
                null
        );

        // Notify partner via Pub/Sub -> notification-service
        publishProductDecisionEvent(
                saved,
                ProductModerationStatus.APPROVED,
                null,
                PartnerEvent.EventType.PRODUCT_APPROVED
        );

        return toProductResponse(saved);
    }

    @Override
    @CacheEvict(value = "menus:full", allEntries = true)
    public ProductResponse rejectProduct(Long productId, Long adminId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new IllegalStateException("Cannot reject a deleted product");
        }

        if (product.getModerationStatus() != ProductModerationStatus.PENDING) {
            throw new IllegalStateException("Product is not pending moderation");
        }

        product.setModerationStatus(ProductModerationStatus.REJECTED);
        product.setModerationReason(reason);

        Product saved = productRepository.save(product);

        // Audit history
        auditLogService.logWithChanges(
                adminId,
                "REJECT",
                "PRODUCT",
                productId,
                "{\"moderationStatus\":\"PENDING\"}",
                "{\"moderationStatus\":\"REJECTED\"}",
                reason
        );
        saveProductHistoryBackup(
                saved.getPartnerId(),
                saved.getId(),
                "REJECT",
                "ADMIN",
                adminId,
                "{\"moderationStatus\":\"PENDING\"}",
                "{\"moderationStatus\":\"REJECTED\"}",
                reason
        );

        // Notify partner via Pub/Sub -> notification-service
        publishProductDecisionEvent(
                saved,
                ProductModerationStatus.REJECTED,
                reason,
                PartnerEvent.EventType.PRODUCT_REJECTED
        );

        return toProductResponse(saved);
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

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public List<ProductResponse> setPromotion(Long partnerId, List<Long> productIds,
                                              String promotionLabel, java.time.LocalDate promotionStartDate,
                                              java.time.LocalDate promotionEndDate, Integer discountPercentage) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllById(productIds).stream()
                .filter(p -> partnerId.equals(p.getPartnerId()))
                .toList();
        java.time.LocalDateTime appliedAt = java.time.LocalDateTime.now();
        for (Product p : products) {
            String beforeSnapshot = buildProductSnapshot(p);
            p.setPromotionLabel(promotionLabel != null && !promotionLabel.isBlank() ? promotionLabel.trim() : null);
            p.setPromotionStartDate(promotionStartDate);
            p.setPromotionEndDate(promotionEndDate);
            if (discountPercentage == null || discountPercentage <= 0) {
                if (p.getOriginalPrice() != null) {
                    p.setPrice(p.getOriginalPrice());
                    p.setOriginalPrice(null);
                    p.setDiscountPercentage(null);
                }
            } else {
                BigDecimal basePrice = p.getOriginalPrice() != null ? p.getOriginalPrice() : p.getPrice();
                if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
                    p.setOriginalPrice(basePrice);
                    BigDecimal pct = BigDecimal.valueOf(discountPercentage).min(new BigDecimal("100"));
                    BigDecimal reduced = basePrice.multiply(BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP)));
                    p.setPrice(reduced);
                    p.setDiscountPercentage(pct);
                }
            }
            productRepository.save(p);
            saveProductHistoryBackup(
                    p.getPartnerId(),
                    p.getId(),
                    "PRODUCT_PROMOTION_UPDATED",
                    "PARTNER",
                    null,
                    beforeSnapshot,
                    buildProductSnapshot(p),
                    null
            );
            // Historique : log pour chaque produit (même en cas de suppression de promo)
            promotionLogRepository.save(PromotionLog.builder()
                    .partnerId(partnerId)
                    .productId(p.getId())
                    .productName(p.getName())
                    .promotionLabel(p.getPromotionLabel())
                    .promotionStartDate(p.getPromotionStartDate())
                    .promotionEndDate(p.getPromotionEndDate())
                    .discountPercentage(p.getDiscountPercentage())
                    .appliedAt(appliedAt)
                    .build());
        }
        return products.stream().map(this::toProductResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionLogResponse> getPromotionLogs(Long partnerId, Pageable pageable,
                                                      String search, LocalDate dateFrom, LocalDate dateTo,
                                                      Long productId) {
        Specification<PromotionLog> spec = buildPromotionLogSpec(partnerId, search, dateFrom, dateTo, productId);
        return promotionLogRepository.findAll(spec, pageable)
                .map(log -> PromotionLogResponse.builder()
                        .id(log.getId())
                        .productId(log.getProductId())
                        .productName(log.getProductName())
                        .promotionLabel(log.getPromotionLabel())
                        .promotionStartDate(log.getPromotionStartDate())
                        .promotionEndDate(log.getPromotionEndDate())
                        .discountPercentage(log.getDiscountPercentage())
                        .appliedAt(log.getAppliedAt())
                        .build());
    }

    private static Specification<PromotionLog> buildPromotionLogSpec(Long partnerId, String search,
                                                                   LocalDate dateFrom, LocalDate dateTo,
                                                                   Long productId) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("partnerId"), partnerId));

            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (search != null && !search.isBlank()) {
                var pattern = "%" + search.trim().toLowerCase() + "%";
                var nameLike = cb.like(cb.lower(cb.coalesce(root.get("productName"), "")), pattern);
                var labelLike = cb.like(cb.lower(cb.coalesce(root.get("promotionLabel"), "")), pattern);
                predicates.add(cb.or(nameLike, labelLike));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("appliedAt"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("appliedAt"), dateTo.atTime(23, 59, 59, 999_999_999)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMenuCsv(Long partnerId) {
        List<Product> products = productRepository.findByPartnerIdAndStatusNot(partnerId, ProductStatus.DELETED);
        if (products.isEmpty()) {
            return "id,name,category,price,isAvailable,stock,description\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        var categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNames = categoryIds.isEmpty() ? Map.of() : menuCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(com.speedline.partner.domain.MenuCategory::getId, com.speedline.partner.domain.MenuCategory::getName));
        var productIds = products.stream().map(Product::getId).toList();
        Map<Long, Integer> stockByProduct = productStockRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(ProductStock::getProductId, ps -> java.util.Optional.ofNullable(ps.getQuantity()).orElse(0)));
        var sb = new StringBuilder();
        sb.append("id,name,category,price,isAvailable,stock,description\n");
        for (Product p : products) {
            String categoryName = p.getCategoryId() != null ? categoryNames.getOrDefault(p.getCategoryId(), "") : "";
            int stock = stockByProduct.getOrDefault(p.getId(), 0);
            Boolean available = p.getIsAvailable();
            sb.append(p.getId()).append(',')
                    .append(escapeCsv(p.getName())).append(',')
                    .append(escapeCsv(categoryName)).append(',')
                    .append(p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO).append(',')
                    .append(Boolean.TRUE.equals(available)).append(',')
                    .append(stock).append(',')
                    .append(escapeCsv(p.getDescription())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportPreviewResponse importPreview(Long partnerId, MultipartFile file) {
        List<ImportPreviewResponse.ImportChangeRow> changes = new ArrayList<>();
        List<ImportPreviewResponse.ImportParseError> parseErrors = new ArrayList<>();
        Map<Long, String> categoryNames = loadCategoryNamesForPartner(partnerId);
        List<Product> partnerProducts = productRepository.findByPartnerIdAndStatusNot(partnerId, ProductStatus.DELETED);
        Map<Long, Product> productMap = partnerProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Integer> stockMap = productStockRepository.findByProductIdIn(productMap.keySet().stream().toList()).stream()
                .collect(Collectors.toMap(ProductStock::getProductId, ps -> Optional.ofNullable(ps.getQuantity()).orElse(0)));

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || !headerLine.toLowerCase().replace(" ", "").startsWith("id,name,")) {
                parseErrors.add(ImportPreviewResponse.ImportParseError.builder().row(1).message("Header invalide (attendu: id,name,category,price,isAvailable,stock,description)").build());
                return ImportPreviewResponse.builder().changes(changes).parseErrors(parseErrors).build();
            }
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 5) {
                    parseErrors.add(ImportPreviewResponse.ImportParseError.builder().row(row).message("Colonnes insuffisantes").build());
                    continue;
                }
                Long productId = parseLong(cols.get(0));
                if (productId == null) {
                    parseErrors.add(ImportPreviewResponse.ImportParseError.builder().row(row).message("id produit invalide").build());
                    continue;
                }
                Product product = productMap.get(productId);
                if (product == null) {
                    parseErrors.add(ImportPreviewResponse.ImportParseError.builder().row(row).message("Produit " + productId + " introuvable ou n'appartient pas au partenaire").build());
                    continue;
                }
                String name = cols.size() > 1 ? cols.get(1).trim() : "";
                String categoryName = cols.size() > 2 ? cols.get(2).trim() : "";
                BigDecimal price = parseBigDecimal(cols.size() > 3 ? cols.get(3) : "0");
                boolean isAvailable = parseBoolean(cols.size() > 4 ? cols.get(4) : "true");
                int stock = parsePositiveInt(cols.size() > 5 ? cols.get(5) : "0");
                String description = cols.size() > 6 ? cols.get(6).trim() : "";

                String currentCategoryName = product.getCategoryId() != null ? categoryNames.getOrDefault(product.getCategoryId(), "") : "";
                if (!Objects.equals(name, product.getName())) {
                    changes.add(ImportPreviewResponse.ImportChangeRow.builder().productId(productId).productName(product.getName()).field("name").oldValue(product.getName()).newValue(name).build());
                }
                if (!Objects.equals(categoryName, currentCategoryName)) {
                    changes.add(ImportPreviewResponse.ImportChangeRow.builder().productId(productId).productName(product.getName()).field("category").oldValue(currentCategoryName).newValue(categoryName).build());
                }
                if (price != null && product.getPrice() != null && price.compareTo(product.getPrice()) != 0) {
                    changes.add(ImportPreviewResponse.ImportChangeRow.builder().productId(productId).productName(product.getName()).field("price").oldValue(String.valueOf(product.getPrice())).newValue(String.valueOf(price)).build());
                }
                if (Boolean.TRUE.equals(product.getIsAvailable()) != isAvailable) {
                    changes.add(ImportPreviewResponse.ImportChangeRow.builder().productId(productId).productName(product.getName()).field("isAvailable").oldValue(String.valueOf(product.getIsAvailable())).newValue(String.valueOf(isAvailable)).build());
                }
                int currentStock = stockMap.getOrDefault(productId, 0);
                if (stock != currentStock) {
                    changes.add(ImportPreviewResponse.ImportChangeRow.builder().productId(productId).productName(product.getName()).field("stock").oldValue(String.valueOf(currentStock)).newValue(String.valueOf(stock)).build());
                }
                if (!Objects.equals(description, product.getDescription() != null ? product.getDescription() : "")) {
                    changes.add(ImportPreviewResponse.ImportChangeRow.builder().productId(productId).productName(product.getName()).field("description").oldValue(product.getDescription()).newValue(description).build());
                }
            }
        } catch (Exception e) {
            log.warn("importPreview error: {}", e.getMessage());
            parseErrors.add(ImportPreviewResponse.ImportParseError.builder().row(0).message("Erreur lecture fichier: " + e.getMessage()).build());
        }
        return ImportPreviewResponse.builder().changes(changes).parseErrors(parseErrors).build();
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    @Transactional
    public ImportConfirmResult importConfirm(Long partnerId, MultipartFile file) {
        List<ImportConfirmResult.ImportConfirmError> errors = new ArrayList<>();
        int processed = 0;
        int success = 0;
        List<Product> partnerProducts = productRepository.findByPartnerIdAndStatusNot(partnerId, ProductStatus.DELETED);
        Map<Long, Product> productMap = partnerProducts.stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<String, Long> categoryNameToId = menuCategoryRepository.findByPartnerIdOrderByPositionAsc(partnerId).stream()
                .collect(Collectors.toMap(MenuCategory::getName, MenuCategory::getId, (a, b) -> a));

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || !headerLine.toLowerCase().replace(" ", "").startsWith("id,name,")) {
                errors.add(ImportConfirmResult.ImportConfirmError.builder().row(1).message("Header invalide").build());
                return ImportConfirmResult.builder().processed(0).success(0).errors(errors).build();
            }
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                processed++;
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 5) {
                    errors.add(ImportConfirmResult.ImportConfirmError.builder().row(row).message("Colonnes insuffisantes").build());
                    continue;
                }
                Long productId = parseLong(cols.get(0));
                if (productId == null) {
                    errors.add(ImportConfirmResult.ImportConfirmError.builder().row(row).message("id produit invalide").build());
                    continue;
                }
                Product product = productMap.get(productId);
                if (product == null) {
                    errors.add(ImportConfirmResult.ImportConfirmError.builder().row(row).productId(productId).message("Produit introuvable ou n'appartient pas au partenaire").build());
                    continue;
                }
                try {
                    String name = cols.size() > 1 ? cols.get(1).trim() : product.getName();
                    String categoryName = cols.size() > 2 ? cols.get(2).trim() : "";
                    BigDecimal price = parseBigDecimal(cols.size() > 3 ? cols.get(3) : "0");
                    boolean isAvailable = parseBoolean(cols.size() > 4 ? cols.get(4) : "true");
                    int stock = parsePositiveInt(cols.size() > 5 ? cols.get(5) : "0");
                    String description = cols.size() > 6 ? cols.get(6).trim() : "";

                    if (name != null && !name.isBlank()) product.setName(name);
                    Long categoryId = categoryName.isBlank() ? null : categoryNameToId.get(categoryName);
                    product.setCategoryId(categoryId);
                    if (price != null) product.setPrice(price);
                    product.setIsAvailable(isAvailable);
                    if (description != null) product.setDescription(description);
                    productRepository.save(product);

                    ProductStock ps;
                    var stockOpt = productStockRepository.findByProductId(productId);
                    if (stockOpt.isPresent()) {
                        ps = stockOpt.get();
                        ps.setQuantity(stock);
                        ps = productStockRepository.save(ps);
                    } else {
                        ps = productStockRepository.save(ProductStock.builder()
                            .productId(productId).quantity(stock).lowStockThreshold(0).isTrackingEnabled(false)
                            .updatedAt(java.time.LocalDateTime.now())
                            .build());
                    }
                    applyAvailabilityAndEventsForProduct(partnerId, product, ps);
                    success++;
                } catch (Exception e) {
                    errors.add(ImportConfirmResult.ImportConfirmError.builder().row(row).productId(productId).message(e.getMessage()).build());
                }
            }
        } catch (Exception e) {
            log.error("importConfirm error: {}", e.getMessage(), e);
            errors.add(ImportConfirmResult.ImportConfirmError.builder().row(0).message("Erreur lecture: " + e.getMessage()).build());
        }
        return ImportConfirmResult.builder().processed(processed).success(success).errors(errors).build();
    }

    private Map<Long, String> loadCategoryNamesForPartner(Long partnerId) {
        return menuCategoryRepository.findByPartnerIdOrderByPositionAsc(partnerId).stream()
                .collect(Collectors.toMap(MenuCategory::getId, MenuCategory::getName));
    }

    private void applyAvailabilityAndEventsForProduct(Long partnerId, Product product, ProductStock stock) {
        if (stock == null) return;
        if (Boolean.FALSE.equals(stock.getIsTrackingEnabled())) return;
        int qty = Optional.ofNullable(stock.getQuantity()).orElse(0);
        product.setIsAvailable(qty > 0);
        productRepository.save(product);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            if (line.charAt(i) == '"') {
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < line.length()) {
                    char c = line.charAt(i);
                    if (c == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            sb.append('"');
                            i += 2;
                        } else {
                            i++;
                            break;
                        }
                    } else {
                        sb.append(c);
                        i++;
                    }
                }
                out.add(sb.toString());
                if (i < line.length() && line.charAt(i) == ',') i++;
            } else {
                int start = i;
                while (i < line.length() && line.charAt(i) != ',') i++;
                out.add(line.substring(start, i).trim());
                if (i < line.length()) i++;
            }
        }
        return out;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean parseBoolean(String s) {
        if (s == null) return true;
        String v = s.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "oui".equals(v);
    }

    private static int parsePositiveInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
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
        return toProductResponse(p, null);
    }

    ProductResponse toProductResponse(Product p, String stockStatus) {
        List<OptionGroupResponse> optionGroups = productOptionRepository
                .findByProductIdAndIsActiveTrueOrderByDisplayOrderAsc(p.getId())
                .stream()
                .map(og -> toGroupResponse(og, p.getId()))
                .collect(Collectors.toList());

        ProductResponse response = ProductResponse.builder()
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
                .promotionLabel(p.getPromotionLabel())
                .promotionStartDate(p.getPromotionStartDate())
                .promotionEndDate(p.getPromotionEndDate())
                .originalPrice(p.getOriginalPrice())
                .discountPercentage(p.getDiscountPercentage())
                .moderationStatus(p.getModerationStatus())
                .moderationReason(p.getModerationReason())
                .build();

        attachPendingPartnerChangesDetails(response, p.getId());
        return response;
    }

    private void attachPendingPartnerChangesDetails(ProductResponse response, Long productId) {
        List<ProductHistoryBackup> history = productHistoryBackupRepository.findByProductIdOrderByCreatedAtAsc(productId);
        if (history == null || history.isEmpty()) return;

        int startIdx = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            String action = history.get(i).getAction();
            if ("APPROVE".equals(action) || "REJECT".equals(action)) {
                startIdx = i + 1;
                break;
            }
        }

        List<ProductHistoryBackup> pendingPartnerActions = history.subList(startIdx, history.size()).stream()
                .filter(this::isPendingPartnerChangeEntry)
                .toList();

        if (pendingPartnerActions.isEmpty()) {
            return;
        }

        Map<String, Object> firstBefore = new LinkedHashMap<>();
        Map<String, Object> latestAfter = new LinkedHashMap<>();
        ProductHistoryBackup latestEntry = pendingPartnerActions.get(pendingPartnerActions.size() - 1);

        for (ProductHistoryBackup entry : pendingPartnerActions) {
            Map<String, Object> beforeMap = parseJsonMap(entry.getChangesBefore());
            Map<String, Object> afterMap = parseJsonMap(entry.getChangesAfter());
            for (Map.Entry<String, Object> e : beforeMap.entrySet()) {
                if (!firstBefore.containsKey(e.getKey())) {
                    firstBefore.put(e.getKey(), e.getValue());
                }
            }
            latestAfter.putAll(afterMap);
        }

        Map<String, Object> mergedBefore = new LinkedHashMap<>();
        Map<String, Object> mergedAfter = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : latestAfter.entrySet()) {
            String key = e.getKey();
            Object before = firstBefore.containsKey(key) ? firstBefore.get(key) : null;
            Object after = e.getValue();
            if (!areEquivalentSnapshotValues(before, after)) {
                mergedBefore.put(key, before);
                mergedAfter.put(key, after);
            }
        }

        if (mergedAfter.isEmpty()) return;

        response.setLastChangesBefore(writeJsonMap(mergedBefore));
        response.setLastChangesAfter(writeJsonMap(mergedAfter));
        response.setLastAuditAction(latestEntry.getAction());
        response.setLastAuditAt(latestEntry.getCreatedAt());
    }

    private boolean isPendingPartnerChangeEntry(ProductHistoryBackup entry) {
        if (entry == null) return false;
        String actorType = entry.getActorType();
        if (actorType != null && "PARTNER".equalsIgnoreCase(actorType.trim())) return true;

        String action = entry.getAction() == null ? "" : entry.getAction().trim();
        return "PRODUCT_SUBMITTED".equals(action)
                || "PRODUCT_RESUBMITTED".equals(action)
                || "PRODUCT_PROMOTION_UPDATED".equals(action);
    }

    private boolean areEquivalentSnapshotValues(Object before, Object after) {
        if (Objects.equals(before, after)) return true;
        if (before == null || after == null) return false;

        String b = String.valueOf(before).trim();
        String a = String.valueOf(after).trim();
        if (b.equals(a)) return true;

        try {
            return new BigDecimal(b).compareTo(new BigDecimal(a)) == 0;
        } catch (Exception ignored) {
            return false;
        }
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

    private void publishProductModerationRequest(Long partnerId, Product product) {
        try {
            var partnerOpt = partnerRepository.findById(partnerId);
            if (partnerOpt.isEmpty()) {
                log.warn("publishProductModerationRequest: partner not found id={}", partnerId);
                return;
            }
            var partner = partnerOpt.get();

            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(PartnerEvent.EventType.PRODUCT_REQUEST_SUBMITTED)
                    .partnerId(partnerId)
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(ProductModerationStatus.PENDING.name())
                    .newModerationStatus(ProductModerationStatus.PENDING.name())
                    .reason(null)
                    .productId(product.getId())
                    .productName(product.getName())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error("publishProductModerationRequest failed: partnerId={}, productId={}: {}", partnerId, product.getId(), e.getMessage(), e);
        }
    }

    private boolean isProductAutoApprovalEnabled(Long partnerId) {
        try {
            return partnerRepository.findById(partnerId)
                    .map(Partner::getAllowProductUpdatesWithoutApproval)
                    .map(Boolean.TRUE::equals)
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Could not load partner auto-approval flag for partnerId={}: {}", partnerId, e.getMessage());
            return false;
        }
    }

    private String buildProductSnapshot(Product p) {
        return String.format(
                "{\"name\":\"%s\",\"description\":\"%s\",\"price\":\"%s\",\"originalPrice\":\"%s\",\"discountPercentage\":\"%s\",\"categoryId\":%s,\"imageUrl\":\"%s\",\"tags\":\"%s\",\"isAvailable\":%s,\"isPopular\":%s,\"preparationTimeMin\":%s,\"stockQuantity\":%s,\"promotionLabel\":\"%s\",\"promotionStartDate\":\"%s\",\"promotionEndDate\":\"%s\",\"isVegetarian\":%s,\"isVegan\":%s,\"isHalal\":%s,\"isGlutenFree\":%s,\"spicyLevel\":%s,\"isNew\":%s,\"isFeatured\":%s,\"status\":\"%s\",\"moderationStatus\":\"%s\"}",
                safe(p.getName()),
                safe(p.getDescription()),
                p.getPrice() != null ? p.getPrice().toPlainString() : "",
                p.getOriginalPrice() != null ? p.getOriginalPrice().toPlainString() : "",
                p.getDiscountPercentage() != null ? p.getDiscountPercentage().toPlainString() : "",
                p.getCategoryId(),
                safe(p.getImage()),
                safe(p.getTags()),
                p.getIsAvailable(),
                p.getIsPopular(),
                p.getPreparationTime(),
                p.getStockQuantity(),
                safe(p.getPromotionLabel()),
                p.getPromotionStartDate() != null ? p.getPromotionStartDate().toString() : "",
                p.getPromotionEndDate() != null ? p.getPromotionEndDate().toString() : "",
                p.getIsVegetarian(),
                p.getIsVegan(),
                p.getIsHalal(),
                p.getIsGlutenFree(),
                p.getSpicyLevel(),
                p.getIsNew(),
                p.getIsFeatured(),
                p.getStatus() != null ? p.getStatus().name() : "",
                p.getModerationStatus() != null ? p.getModerationStatus().name() : ""
        );
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"");
    }

    private Map<String, Object> parseJsonMap(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJsonMap(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private void saveProductHistoryBackup(
            Long partnerId,
            Long productId,
            String action,
            String actorType,
            Long actorId,
            String changesBefore,
            String changesAfter,
            String reason) {
        try {
            productHistoryBackupRepository.save(ProductHistoryBackup.builder()
                    .partnerId(partnerId)
                    .productId(productId)
                    .action(action)
                    .actorType(actorType)
                    .actorId(actorId)
                    .changesBefore(changesBefore)
                    .changesAfter(changesAfter)
                    .reason(reason)
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to save product history backup productId={} action={} error={}", productId, action, ex.getMessage());
        }
    }

    private void publishProductDecisionEvent(
            Product product,
            ProductModerationStatus newStatus,
            String reason,
            PartnerEvent.EventType eventType) {
        Long partnerId = product.getPartnerId();
        try {
            var partnerOpt = partnerRepository.findById(partnerId);
            if (partnerOpt.isEmpty()) {
                log.warn("publishProductDecisionEvent: partner not found id={}", partnerId);
                return;
            }
            var partner = partnerOpt.get();

            partnerEventPublisher.publish(PartnerEvent.builder()
                    .eventType(eventType)
                    .partnerId(partnerId)
                    .userId(partner.getUserId())
                    .businessName(partner.getBusinessName())
                    .brandName(partner.getBrandName())
                    .email(partner.getEmail())
                    .status(newStatus.name())
                    .newModerationStatus(newStatus.name())
                    .reason(reason)
                    .productId(product.getId())
                    .productName(product.getName())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.error(
                    "publishProductDecisionEvent failed: partnerId={}, productId={}, newStatus={}: {}",
                    partnerId,
                    product.getId(),
                    newStatus,
                    e.getMessage(),
                    e
            );
        }
    }
}
