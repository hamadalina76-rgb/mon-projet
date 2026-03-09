package com.speedline.partner.service.impl;

import com.speedline.partner.domain.MenuCategory;
import com.speedline.partner.domain.Product;
import com.speedline.partner.domain.ProductStock;
import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.dto.request.UpdateStockRequest;
import com.speedline.partner.dto.response.BulkStockUpdateResult;
import com.speedline.partner.dto.response.ProductStockDTO;
import com.speedline.partner.exception.ResourceNotFoundException;
import com.speedline.partner.domain.Partner;
import com.speedline.partner.event.PartnerStockEventPublisher;
import com.speedline.partner.repository.MenuCategoryRepository;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.repository.ProductRepository;
import com.speedline.partner.repository.ProductStockRepository;
import com.speedline.partner.service.ProductStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductStockServiceImpl implements ProductStockService {

    private static final String STOCK_STATUS_IN_STOCK = "IN_STOCK";
    private static final String STOCK_STATUS_LOW_STOCK = "LOW_STOCK";
    private static final String STOCK_STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerStockEventPublisher stockEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockDTO> getStockList(Long partnerId) {
        List<Product> products = productRepository.findByPartnerIdAndStatusNot(partnerId, ProductStatus.DELETED);
        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, ProductStock> stockByProductId = productStockRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(ProductStock::getProductId, ps -> ps));
        Map<Long, String> categoryNames = loadCategoryNames(products);

        return products.stream()
                .map(p -> toStockDTO(p, stockByProductId.get(p.getId()),
                        Optional.ofNullable(p.getCategoryId()).map(categoryNames::get).orElse(null)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockDTO> getStockList(Long partnerId, String search, String status) {
        Stream<ProductStockDTO> stream = getStockList(partnerId).stream();

        if (search != null && !search.isBlank()) {
            String lc = search.trim().toLowerCase();
            stream = stream.filter(dto ->
                    dto.getProductName().toLowerCase().contains(lc)
                            || Optional.ofNullable(dto.getCategoryName())
                            .map(c -> c.toLowerCase().contains(lc))
                            .orElse(false));
        }

        if (status != null && !status.isBlank()) {
            final String statusFilter = status.toUpperCase();
            stream = stream.filter(dto -> Objects.equals(dto.getStockStatus(), statusFilter));
        }

        return stream.toList();
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public ProductStockDTO updateStock(Long partnerId, Long productId, UpdateStockRequest request) {
        Product product = findProductOrThrow(partnerId, productId);
        ProductStock stock = productStockRepository.findByProductId(productId)
                .orElseGet(() -> createDefaultStock(productId));

        if (request.getQuantity() != null) {
            stock.setQuantity(request.getQuantity());
        }
        if (request.getLowStockThreshold() != null) {
            stock.setLowStockThreshold(request.getLowStockThreshold());
        }
        if (request.getIsTrackingEnabled() != null) {
            stock.setIsTrackingEnabled(request.getIsTrackingEnabled());
        }
        ProductStock saved = productStockRepository.save(stock);
        applyAvailabilityAndEvents(partnerId, product, saved);
        productRepository.flush();
        String categoryName = Optional.ofNullable(product.getCategoryId())
                .flatMap(menuCategoryRepository::findById)
                .map(MenuCategory::getName)
                .orElse(null);
        return toStockDTO(product, saved, categoryName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockDTO> getLowStock(Long partnerId) {
        return getStockList(partnerId).stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsTrackingEnabled()))
                .filter(dto -> dto.getQuantity() != null && dto.getLowStockThreshold() != null
                        && dto.getQuantity() <= dto.getLowStockThreshold())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockDTO> getOutOfStock(Long partnerId) {
        return getStockList(partnerId).stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getIsTrackingEnabled()))
                .filter(dto -> dto.getQuantity() != null && dto.getQuantity() == 0)
                .toList();
    }

    @Override
    @CacheEvict(value = "menus:full", key = "#partnerId")
    public BulkStockUpdateResult bulkUpdate(Long partnerId, MultipartFile file) {
        List<BulkStockUpdateResult.BulkStockError> errors = new ArrayList<>();
        int processed = 0;
        int success = 0;

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("productId") || trimmed.startsWith("product_id")) {
                    continue;
                }
                String[] parts = trimmed.split("[,;]");
                if (parts.length < 2) {
                    errors.add(bulkError(row, null, "Format invalide: productId,quantity attendu"));
                    processed++;
                    continue;
                }
                Optional<Long> productIdOpt = parseLong(parts[0].trim());
                Optional<Integer> quantityOpt = parseInt(parts[1].trim());
                if (productIdOpt.isEmpty() || quantityOpt.isEmpty()) {
                    errors.add(BulkStockUpdateResult.BulkStockError.builder()
                            .row(row)
                            .productId(productIdOpt.orElse(null))
                            .message("productId et quantity doivent être numériques")
                            .build());
                    processed++;
                    continue;
                }
                long productId = productIdOpt.get();
                int quantity = quantityOpt.get();
                if (quantity < 0) {
                    errors.add(bulkError(row, productId, "quantity doit être >= 0"));
                    processed++;
                    continue;
                }
                processed++;
                try {
                    var req = new UpdateStockRequest();
                    req.setQuantity(quantity);
                    updateStock(partnerId, productId, req);
                    success++;
                } catch (ResourceNotFoundException e) {
                    errors.add(bulkError(row, productId, "Produit introuvable ou n'appartient pas au partenaire"));
                } catch (Exception e) {
                    log.warn("Bulk stock update row {} productId {}: {}", row, productId, e.getMessage());
                    errors.add(bulkError(row, productId, e.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("Bulk stock parse error: {}", e.getMessage(), e);
            return BulkStockUpdateResult.builder()
                    .processed(0)
                    .success(0)
                    .errors(List.of(bulkError(0, null, "Erreur lecture fichier: " + e.getMessage())))
                    .build();
        }

        return BulkStockUpdateResult.builder()
                .processed(processed)
                .success(success)
                .errors(errors)
                .build();
    }

    private static BulkStockUpdateResult.BulkStockError bulkError(int row, Long productId, String message) {
        return BulkStockUpdateResult.BulkStockError.builder()
                .row(row)
                .productId(productId)
                .message(message)
                .build();
    }

    private static Optional<Long> parseLong(String s) {
        if (s == null || !s.matches("\\d+")) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Product findProductOrThrow(Long partnerId, Long productId) {
        return productRepository.findById(productId)
                .filter(p -> partnerId.equals(p.getPartnerId()))
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product introuvable (id=" + productId + ", partnerId=" + partnerId + ")"));
    }

    private ProductStock createDefaultStock(Long productId) {
        ProductStock stock = ProductStock.builder()
                .productId(productId)
                .quantity(0)
                .lowStockThreshold(0)
                .isTrackingEnabled(true)
                .build();
        return productStockRepository.save(stock);
    }

    /**
     * Apply automatic rules: when isTrackingEnabled, set Product.isAvailable from quantity;
     * publish LOW_STOCK / OUT_OF_STOCK events when applicable.
     * partnerUserId is sent so the notification is stored under the partner's user account and appears in history.
     */
    private void applyAvailabilityAndEvents(Long partnerId, Product product, ProductStock stock) {
        if (Boolean.FALSE.equals(stock.getIsTrackingEnabled())) {
            return;
        }
        Long partnerUserId = partnerRepository.findById(partnerId).map(Partner::getUserId).orElse(null);
        int qty = Optional.ofNullable(stock.getQuantity()).orElse(0);
        if (qty == 0) {
            product.setIsAvailable(false);
            stockEventPublisher.publishOutOfStock(partnerId, partnerUserId, product.getId(), product.getName());
        } else {
            product.setIsAvailable(true);
            int threshold = Optional.ofNullable(stock.getLowStockThreshold()).orElse(0);
            if (qty <= threshold) {
                log.info("Publishing LOW_STOCK event: partnerId={}, productId={}, productName={}, quantity={}, threshold={}",
                        partnerId, product.getId(), product.getName(), qty, threshold);
                stockEventPublisher.publishLowStock(partnerId, partnerUserId, product.getId(), product.getName(), qty, threshold);
            }
        }
        productRepository.save(product);
    }

    private Map<Long, String> loadCategoryNames(List<Product> products) {
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return menuCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(MenuCategory::getId, MenuCategory::getName));
    }

    /**
     * Stock status is computed from quantity vs threshold so the UI always shows the real state
     * (LOW_STOCK when qty <= threshold, OUT_OF_STOCK when qty == 0). isTrackingEnabled only
     * controls product availability and notification publishing, not the displayed status.
     */
    private ProductStockDTO toStockDTO(Product product, ProductStock stock, String categoryName) {
        if (stock == null) {
            return ProductStockDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .categoryName(categoryName)
                    .quantity(0)
                    .lowStockThreshold(0)
                    .isTrackingEnabled(false)
                    .isAvailable(Optional.ofNullable(product.getIsAvailable()).orElse(true))
                    .stockStatus(STOCK_STATUS_OUT_OF_STOCK)
                    .updatedAt(product.getUpdatedAt())
                    .build();
        }
        int qty = Optional.ofNullable(stock.getQuantity()).orElse(0);
        int threshold = Optional.ofNullable(stock.getLowStockThreshold()).orElse(0);
        String status = qty == 0 ? STOCK_STATUS_OUT_OF_STOCK
                : qty <= threshold ? STOCK_STATUS_LOW_STOCK
                : STOCK_STATUS_IN_STOCK;
        return ProductStockDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .categoryName(categoryName)
                .quantity(qty)
                .lowStockThreshold(threshold)
                .isTrackingEnabled(stock.getIsTrackingEnabled())
                .isAvailable(Optional.ofNullable(product.getIsAvailable()).orElse(true))
                .stockStatus(status)
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}
