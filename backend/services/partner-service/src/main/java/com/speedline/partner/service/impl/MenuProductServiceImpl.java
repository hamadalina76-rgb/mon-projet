package com.speedline.partner.service.impl;

import com.speedline.partner.domain.MenuCategory;
import com.speedline.partner.domain.OptionValue;
import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductOption;
import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.domain.ProductStock;
import com.speedline.partner.dto.request.*;
import com.speedline.partner.dto.response.ImportConfirmResult;
import com.speedline.partner.dto.response.ImportPreviewResponse;
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

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public List<ProductResponse> setPromotion(Long partnerId, List<Long> productIds,
                                              String promotionLabel, java.time.LocalDate promotionEndDate, Integer discountPercentage) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllById(productIds).stream()
                .filter(p -> partnerId.equals(p.getPartnerId()))
                .toList();
        for (Product p : products) {
            p.setPromotionLabel(promotionLabel != null && !promotionLabel.isBlank() ? promotionLabel.trim() : null);
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
        }
        return products.stream().map(this::toProductResponse).toList();
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
                .promotionLabel(p.getPromotionLabel())
                .promotionEndDate(p.getPromotionEndDate())
                .originalPrice(p.getOriginalPrice())
                .discountPercentage(p.getDiscountPercentage())
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
