package com.speedline.partner.controller;

import com.speedline.partner.dto.request.*;
import com.speedline.partner.dto.response.*;
import com.speedline.partner.scheduler.PromotionEndingScheduler;
import com.speedline.partner.service.FileStorageService;
import com.speedline.partner.service.MenuCategoryService;
import com.speedline.partner.service.MenuProductService;
import com.speedline.partner.service.ProductStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller — Menu d'un partenaire.
 *
 * Base path : /partners/{partnerId}/menu
 *
 * Catégories :
 *   GET    /categories
 *   POST   /categories
 *   PUT    /categories/{catId}
 *   DELETE /categories/{catId}
 *   PATCH  /categories/reorder
 *
 * Produits :
 *   GET    /categories/{catId}/products
 *   POST   /products
 *   PUT    /products/{productId}
 *   DELETE /products/{productId}
 *   PATCH  /products/{productId}/availability
 *   PATCH  /products/reorder
 *
 * Groupes d'options :
 *   POST   /products/{productId}/option-groups
 *   PUT    /products/{productId}/option-groups/{groupId}
 *   DELETE /products/{productId}/option-groups/{groupId}
 *
 * Options :
 *   POST   /products/{productId}/option-groups/{groupId}/options
 *   PUT    /options/{optionId}
 *   DELETE /options/{optionId}
 *
 * Menu complet :
 *   GET    /                  (TC-16)
 */
@Tag(name = "Menu", description = "Gestion du menu partenaire — catégories, produits, options")
@RestController
@RequestMapping("/partners/{partnerId}/menu")
@RequiredArgsConstructor
@Slf4j
public class PartnerMenuController {

    private final MenuCategoryService menuCategoryService;
    private final MenuProductService  menuProductService;
    private final ProductStockService productStockService;
    private final FileStorageService fileStorageService;
    private final PromotionEndingScheduler promotionEndingScheduler;

    // ============================================================
    //  MENU COMPLET  (TC-16)
    // ============================================================

    /**
     * GET /partners/{partnerId}/menu
     * Retourne le menu complet structuré consommé par l'application client.
     * TC-16 : structure imbriquée {categories:[{category, products:[{..., optionGroups:[...]}]}]}
     */
    @Operation(summary = "Menu complet", description = "Retourne la structure complète du menu (TC-16)")
    @GetMapping
    public ResponseEntity<?> getFullMenu(@PathVariable Long partnerId) {
        try {
            FullMenuResponse menu = menuCategoryService.buildFullMenu(partnerId);
            return ResponseEntity.ok(menu);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("getFullMenu error partnerId={}: {}", partnerId, ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * GET /partners/{partnerId}/menu/export/csv
     * TC-57 : Exporte le menu en CSV (id, name, category, price, isAvailable, stock, description).
     */
    @Operation(summary = "Exporter le menu en CSV")
    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<?> exportMenuCsv(@PathVariable Long partnerId) {
        try {
            byte[] csv = menuProductService.exportMenuCsv(partnerId);
            String filename = "menu-export-" + LocalDate.now() + ".csv";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", filename);
            return ResponseEntity.ok().headers(headers).body(csv);
        } catch (Exception ex) {
            log.error("exportMenuCsv error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/import/preview
     * TC-58 : Preview des modifications avant import CSV.
     */
    @Operation(summary = "Preview import CSV menu")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importPreview(
            @PathVariable Long partnerId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(menuProductService.importPreview(partnerId, file));
        } catch (Exception ex) {
            log.error("importPreview error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/import/confirm
     * TC-59 : Confirme l'import CSV, applique les mises à jour, retourne rapport d'erreurs.
     */
    @Operation(summary = "Confirmer import CSV menu")
    @PostMapping(value = "/import/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importConfirm(
            @PathVariable Long partnerId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(menuProductService.importConfirm(partnerId, file));
        } catch (Exception ex) {
            log.error("importConfirm error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    // ============================================================
    //  CATÉGORIES
    // ============================================================

    /**
     * GET /partners/{partnerId}/menu/categories
     * Retourne les catégories triées par position.
     */
    @Operation(summary = "Liste des catégories")
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(@PathVariable Long partnerId) {
        try {
            List<MenuCategoryResponse> list = menuCategoryService.getCategories(partnerId);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            log.error("getCategories error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/categories
     * TC-09 : HTTP 201, id retourné, position auto = dernière.
     */
    @Operation(summary = "Créer une catégorie", description = "TC-09 : position auto si non fournie")
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            @PathVariable Long partnerId,
            @Valid @RequestBody CreateMenuCategoryRequest request) {
        try {
            MenuCategoryResponse created = menuCategoryService.createCategory(partnerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception ex) {
            log.error("createCategory error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PUT /partners/{partnerId}/menu/categories/{catId}
     */
    @Operation(summary = "Modifier une catégorie")
    @PutMapping("/categories/{catId}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long partnerId,
            @PathVariable Long catId,
            @Valid @RequestBody UpdateMenuCategoryRequest request) {
        try {
            MenuCategoryResponse updated = menuCategoryService.updateCategory(partnerId, catId, request);
            return ResponseEntity.ok(updated);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("updateCategory error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * DELETE /partners/{partnerId}/menu/categories/{catId}
     * TC-11 : soft-delete (isVisible=false) si produits liés, hard-delete sinon.
     */
    @Operation(summary = "Supprimer une catégorie", description = "TC-11 : soft-delete si produits liés")
    @DeleteMapping("/categories/{catId}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable Long partnerId,
            @PathVariable Long catId) {
        try {
            menuCategoryService.deleteCategory(partnerId, catId);
            return ResponseEntity.noContent().build();
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (com.speedline.partner.exception.BusinessRuleException ex) {
            return unprocessable(ex.getMessage());
        } catch (Exception ex) {
            log.error("deleteCategory error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PATCH /partners/{partnerId}/menu/categories/{catId}/toggle-visibility
     * Bascule isVisible de la catégorie. Retourne la catégorie mise à jour.
     */
    @Operation(summary = "Basculer la visibilité d'une catégorie")
    @PatchMapping("/categories/{catId}/toggle-visibility")
    public ResponseEntity<?> toggleCategoryVisibility(
            @PathVariable Long partnerId,
            @PathVariable Long catId) {
        try {
            MenuCategoryResponse updated = menuCategoryService.toggleVisibility(partnerId, catId);
            return ResponseEntity.ok(updated);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("toggleVisibility error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PATCH /partners/{partnerId}/menu/categories/reorder
     * TC-12 : [{id:3,position:1},{id:1,position:2},{id:2,position:3}] → ordre mis à jour.
     */
    @Operation(summary = "Réordonner les catégories", description = "TC-12")
    @PatchMapping("/categories/reorder")
    public ResponseEntity<?> reorderCategories(
            @PathVariable Long partnerId,
            @Valid @RequestBody ReorderRequest request) {
        try {
            List<MenuCategoryResponse> reordered = menuCategoryService.reorderCategories(partnerId, request);
            return ResponseEntity.ok(reordered);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("reorderCategories error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    // ============================================================
    //  PRODUITS
    // ============================================================

    /**
     * GET /partners/{partnerId}/menu/products
     * Page de produits avec filtres et pagination côté serveur.
     * Paramètres : search (nom), categoryId, status (all|available|unavailable|low_stock), page, size.
     */
    @Operation(summary = "Produits paginés", description = "Filtres (search, categoryId, status) et pagination côté backend")
    @GetMapping("/products")
    public ResponseEntity<?> getProductsPage(
            @PathVariable Long partnerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "ALL") String moderationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ProductResponse> result = menuProductService.getProductsPage(
                    partnerId, search, categoryId, status, moderationStatus,
                    org.springframework.data.domain.PageRequest.of(page, size));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("content", result.getContent());
            body.put("totalElements", result.getTotalElements());
            body.put("totalPages", result.getTotalPages());
            body.put("size", result.getSize());
            body.put("number", result.getNumber());
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            log.error("getProductsPage error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    // ============================================================
    //  STOCK (routes before /products/{productId} to avoid path conflict)
    // ============================================================

    /**
     * GET /partners/{partnerId}/menu/products/stock
     * Liste tous les produits avec leur stock actuel.
     */
    @Operation(summary = "Stats stock", description = "Nombre total / en stock / faible / épuisé pour le partenaire.")
    @GetMapping("/products/stock/stats")
    public ResponseEntity<?> getStockStats(@PathVariable Long partnerId) {
        try {
            List<ProductStockDTO> all = productStockService.getStockList(partnerId);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total",      all.size());
            stats.put("inStock",    all.stream().filter(p -> "IN_STOCK".equals(p.getStockStatus())).count());
            stats.put("lowStock",   all.stream().filter(p -> "LOW_STOCK".equals(p.getStockStatus())).count());
            stats.put("outOfStock", all.stream().filter(p -> "OUT_OF_STOCK".equals(p.getStockStatus())).count());
            return ResponseEntity.ok(stats);
        } catch (Exception ex) {
            log.error("getStockStats error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    @Operation(summary = "Liste stock paginée", description = "Produits avec stock, filtrés et paginés côté serveur.")
    @GetMapping("/products/stock")
    public ResponseEntity<?> getStockList(
            @PathVariable Long partnerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<ProductStockDTO> filtered = productStockService.getStockList(partnerId, search, status);
            long totalElements = filtered.size();
            int from = page * size;
            int to   = Math.min(from + size, filtered.size());
            List<ProductStockDTO> content = (from >= filtered.size())
                    ? new ArrayList<>()
                    : filtered.subList(from, to);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("content",       content);
            resp.put("totalElements", totalElements);
            resp.put("page",          page);
            resp.put("size",          size);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.error("getStockList error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * GET /partners/{partnerId}/menu/products/low-stock
     * Produits sous le seuil d'alerte (quantity <= lowStockThreshold, isTrackingEnabled = true).
     */
    @Operation(summary = "Produits sous le seuil")
    @GetMapping("/products/low-stock")
    public ResponseEntity<?> getLowStock(@PathVariable Long partnerId) {
        try {
            return ResponseEntity.ok(productStockService.getLowStock(partnerId));
        } catch (Exception ex) {
            log.error("getLowStock error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * GET /partners/{partnerId}/menu/products/out-of-stock
     * Produits épuisés (quantity = 0, isTrackingEnabled = true).
     */
    @Operation(summary = "Produits épuisés")
    @GetMapping("/products/out-of-stock")
    public ResponseEntity<?> getOutOfStock(@PathVariable Long partnerId) {
        try {
            return ResponseEntity.ok(productStockService.getOutOfStock(partnerId));
        } catch (Exception ex) {
            log.error("getOutOfStock error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/products/stock/restore-all
     * Remet en stock tous les produits épuisés (quantity = 1 ou lowStockThreshold).
     */
    @Operation(summary = "Tout remettre en stock")
    @PostMapping("/products/stock/restore-all")
    public ResponseEntity<?> restoreAllOutOfStock(@PathVariable Long partnerId) {
        try {
            int restored = productStockService.restoreAllOutOfStock(partnerId);
            return ResponseEntity.ok(Map.of("restored", restored));
        } catch (Exception ex) {
            log.error("restoreAllOutOfStock error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/products/stock/bulk
     * Mise à jour stock en masse (CSV: productId,quantity).
     */
    @Operation(summary = "Import CSV stock")
    @PostMapping(value = "/products/stock/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpdateStock(
            @PathVariable Long partnerId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            return ResponseEntity.ok(productStockService.bulkUpdate(partnerId, file));
        } catch (Exception ex) {
            log.error("bulkUpdateStock error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * GET /partners/{partnerId}/menu/categories/{catId}/products
     */
    @Operation(summary = "Produits d'une catégorie")
    @GetMapping("/categories/{catId}/products")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable Long partnerId,
            @PathVariable Long catId) {
        try {
            List<ProductResponse> products = menuProductService.getProductsByCategory(partnerId, catId);
            return ResponseEntity.ok(products);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("getProductsByCategory error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/products/{productId}/duplicate
     * TC-38 : Duplique le produit et ses options.
     */
    @Operation(summary = "Dupliquer un produit", description = "TC-38 : copie produit + groupes d'options")
    @PostMapping("/products/{productId}/duplicate")
    public ResponseEntity<?> duplicateProduct(
            @PathVariable Long partnerId,
            @PathVariable Long productId) {
        try {
            ProductResponse created = menuProductService.duplicateProduct(partnerId, productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("duplicateProduct error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * GET /partners/{partnerId}/menu/products/{productId}
     * Détail d'un produit (pour édition).
     */
    @Operation(summary = "Détail d'un produit")
    @GetMapping("/products/{productId}")
    public ResponseEntity<?> getProduct(
            @PathVariable Long partnerId,
            @PathVariable Long productId) {
        try {
            ProductResponse product = menuProductService.getProduct(partnerId, productId);
            return ResponseEntity.ok(product);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("getProduct error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/products
     * TC-10 : HTTP 201, produit lié à la catégorie.
     */
    @Operation(summary = "Créer un produit", description = "TC-10 : HTTP 201, lié à la catégorie")
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(
            @PathVariable Long partnerId,
            @Valid @RequestBody CreateProductRequest request) {
        try {
            ProductResponse created = menuProductService.createProduct(partnerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception ex) {
            log.error("createProduct error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PUT /partners/{partnerId}/menu/products/{productId}
     */
    @Operation(summary = "Modifier un produit")
    @PutMapping("/products/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        try {
            ProductResponse updated = menuProductService.updateProduct(partnerId, productId, request);
            return ResponseEntity.ok(updated);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("updateProduct error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * DELETE /partners/{partnerId}/menu/products/{productId}
     * Soft-delete — status = DELETED.
     */
    @Operation(summary = "Supprimer un produit (soft-delete)")
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long partnerId,
            @PathVariable Long productId) {
        try {
            menuProductService.deleteProduct(partnerId, productId);
            return ResponseEntity.noContent().build();
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("deleteProduct error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PATCH /partners/{partnerId}/menu/products/{productId}/availability
     * TC-13 : isAvailable=false → produit non commandable côté client.
     */
    @Operation(summary = "Toggle disponibilité", description = "TC-13")
    @PatchMapping("/products/{productId}/availability")
    public ResponseEntity<?> updateAvailability(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        try {
            ProductResponse updated = menuProductService.updateAvailability(partnerId, productId, request);
            return ResponseEntity.ok(updated);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("updateAvailability error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PATCH /partners/{partnerId}/menu/products/{productId}/stock
     * Met à jour la quantité (et optionnellement lowStockThreshold, isTrackingEnabled).
     */
    @Operation(summary = "Mettre à jour le stock d'un produit")
    @PatchMapping("/products/{productId}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateStockRequest request) {
        try {
            return ResponseEntity.ok(productStockService.updateStock(partnerId, productId, request));
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("updateStock error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PATCH /partners/{partnerId}/menu/products/reorder
     */
    @Operation(summary = "Réordonner les produits")
    @PatchMapping("/products/reorder")
    public ResponseEntity<?> reorderProducts(
            @PathVariable Long partnerId,
            @Valid @RequestBody ReorderRequest request) {
        try {
            List<ProductResponse> reordered = menuProductService.reorderProducts(partnerId, request);
            return ResponseEntity.ok(reordered);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("reorderProducts error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PATCH /partners/{partnerId}/menu/products/promotions
     * Définit ou supprime le label et la date de fin de promotion pour une liste de produits.
     */
    @Operation(summary = "Définir les promotions sur des produits")
    @PatchMapping("/products/promotions")
    public ResponseEntity<?> setPromotion(
            @PathVariable Long partnerId,
            @Valid @RequestBody SetPromotionRequest request) {
        try {
            List<ProductResponse> updated = menuProductService.setPromotion(
                    partnerId,
                    request.getProductIds(),
                    request.getPromotionLabel(),
                    request.getPromotionStartDate(),
                    request.getPromotionEndDate(),
                    request.getDiscountPercentage());
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            log.error("setPromotion error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * GET /partners/{partnerId}/menu/products/promotions/logs
     * Historique des promotions (paginé et filtré côté serveur).
     * Filtres optionnels : search, dateFrom, dateTo, productId.
     */
    @Operation(summary = "Historique des promotions (paginé et filtré)")
    @GetMapping("/products/promotions/logs")
    public ResponseEntity<?> getPromotionLogs(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long productId) {
        try {
            var pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
            Page<PromotionLogResponse> result = menuProductService.getPromotionLogs(partnerId, pageRequest, search, dateFrom, dateTo, productId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("content", result.getContent());
            body.put("totalElements", result.getTotalElements());
            body.put("totalPages", result.getTotalPages());
            body.put("size", result.getSize());
            body.put("number", result.getNumber());
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            log.error("getPromotionLogs error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/trigger-promotion-ending-check
     * Déclenche manuellement la vérification « promotion se termine dans 3 j / demain » pour ce partenaire.
     * Utile pour tester sans attendre le cron 8h. Envoie les événements vers notification-service.
     */
    @Operation(summary = "Déclencher la vérification fin de promotion (test)")
    @PostMapping("/trigger-promotion-ending-check")
    public ResponseEntity<?> triggerPromotionEndingCheck(@PathVariable Long partnerId) {
        try {
            int count = promotionEndingScheduler.runNowForPartner(partnerId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", count > 0 ? "Events sent for " + count + " product(s)." : "No products with promotion ending in 1 or 3 days.");
            body.put("eventsSent", count);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            log.error("triggerPromotionEndingCheck error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    // ============================================================
    //  GROUPES D'OPTIONS
    // ============================================================

    /**
     * GET /partners/{partnerId}/menu/products/{productId}/option-groups
     * Retourne tous les groupes d'options actifs d'un produit.
     */
    @Operation(summary = "Groupes d'options d'un produit")
    @GetMapping("/products/{productId}/option-groups")
    public ResponseEntity<?> getOptionGroups(
            @PathVariable Long partnerId,
            @PathVariable Long productId) {
        try {
            List<OptionGroupResponse> groups = menuProductService.getOptionGroups(partnerId, productId);
            return ResponseEntity.ok(groups);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("getOptionGroups error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * POST /partners/{partnerId}/menu/products/{productId}/option-groups
     * TC-14 : SINGLE minSelection=1 maxSelection=1.
     * TC-15 : MULTIPLE minSelection=0 maxSelection=3.
     */
    @Operation(summary = "Créer un groupe d'options", description = "TC-14 / TC-15")
    @PostMapping("/products/{productId}/option-groups")
    public ResponseEntity<?> createOptionGroup(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @Valid @RequestBody CreateOptionGroupRequest request) {
        try {
            OptionGroupResponse created = menuProductService.createOptionGroup(partnerId, productId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("createOptionGroup error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PUT /partners/{partnerId}/menu/products/{productId}/option-groups/{groupId}
     */
    @Operation(summary = "Modifier un groupe d'options")
    @PutMapping("/products/{productId}/option-groups/{groupId}")
    public ResponseEntity<?> updateOptionGroup(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateOptionGroupRequest request) {
        try {
            OptionGroupResponse updated = menuProductService.updateOptionGroup(partnerId, productId, groupId, request);
            return ResponseEntity.ok(updated);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("updateOptionGroup error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * DELETE /partners/{partnerId}/menu/products/{productId}/option-groups/{groupId}
     * Supprime le groupe ET toutes ses options en cascade.
     */
    @Operation(summary = "Supprimer un groupe d'options (cascade)")
    @DeleteMapping("/products/{productId}/option-groups/{groupId}")
    public ResponseEntity<?> deleteOptionGroup(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @PathVariable Long groupId) {
        try {
            menuProductService.deleteOptionGroup(partnerId, productId, groupId);
            return ResponseEntity.noContent().build();
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("deleteOptionGroup error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    // ============================================================
    //  OPTIONS
    // ============================================================

    /**
     * POST /partners/{partnerId}/menu/products/{productId}/option-groups/{groupId}/options
     * TC-17 : priceModifier=0 → retourné sans erreur.
     * TC-18 : position auto = max_position + 1.
     */
    @Operation(summary = "Ajouter une option", description = "TC-17 / TC-18")
    @PostMapping("/products/{productId}/option-groups/{groupId}/options")
    public ResponseEntity<?> createOption(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateOptionRequest request) {
        try {
            OptionResponse created = menuProductService.createOption(partnerId, productId, groupId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("createOption error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * PUT /partners/{partnerId}/menu/options/{optionId}
     */
    @Operation(summary = "Modifier une option")
    @PutMapping("/options/{optionId}")
    public ResponseEntity<?> updateOption(
            @PathVariable Long partnerId,
            @PathVariable Long optionId,
            @Valid @RequestBody UpdateOptionRequest request) {
        try {
            OptionResponse updated = menuProductService.updateOption(partnerId, optionId, request);
            return ResponseEntity.ok(updated);
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("updateOption error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    /**
     * DELETE /partners/{partnerId}/menu/options/{optionId}
     */
    @Operation(summary = "Supprimer une option")
    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<?> deleteOption(
            @PathVariable Long partnerId,
            @PathVariable Long optionId) {
        try {
            menuProductService.deleteOption(partnerId, optionId);
            return ResponseEntity.noContent().build();
        } catch (com.speedline.partner.exception.ResourceNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception ex) {
            log.error("deleteOption error: {}", ex.getMessage(), ex);
            return serverError(ex.getMessage());
        }
    }

    // ============================================================
    //  UPLOAD IMAGES  (TC-19 / TC-20 / TC-21 / TC-22 / TC-23 / TC-24)
    // ============================================================

    /**
     * POST /partners/{partnerId}/menu/products/{productId}/upload/image
     * TC-23 : Upload photo produit → HTTP 200, imageUrl retournée.
     */
    @Operation(summary = "Upload image produit", description = "TC-23 : multipart/form-data, champ 'file'")
    @PostMapping(value = "/products/{productId}/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImage(
            @PathVariable Long partnerId,
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.storeProductImage(file, partnerId, productId);
        UpdateProductRequest req = new UpdateProductRequest();
        req.setImageUrl(imageUrl);
        ProductResponse updated = menuProductService.updateProduct(partnerId, productId, req);
        return ResponseEntity.ok(Map.of("url", imageUrl, "imageUrl", imageUrl, "product", updated));
    }

    /**
     * POST /partners/{partnerId}/menu/categories/{catId}/upload/image
     * Upload image d'une catégorie du menu.
     */
    @Operation(summary = "Upload image catégorie menu", description = "multipart/form-data, champ 'file'")
    @PostMapping(value = "/categories/{catId}/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCategoryImage(
            @PathVariable Long partnerId,
            @PathVariable Long catId,
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeCategoryImage(file, partnerId, catId);
        UpdateMenuCategoryRequest req = new UpdateMenuCategoryRequest();
        req.setImageUrl(url);
        MenuCategoryResponse updated = menuCategoryService.updateCategory(partnerId, catId, req);
        return ResponseEntity.ok(Map.of("url", url, "category", updated));
    }

    // ============================================================
    //  ERROR HELPERS
    // ============================================================

    private ResponseEntity<Map<String, Object>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", message));
    }

    private ResponseEntity<Map<String, Object>> unprocessable(String message) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "BUSINESS_RULE_VIOLATION", "message", message));
    }

    private ResponseEntity<Map<String, Object>> serverError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_ERROR", "message", message != null ? message : "Une erreur est survenue"));
    }
}
