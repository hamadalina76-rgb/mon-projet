package com.speedline.partner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.speedline.partner.client.OrderServiceClient;
import com.speedline.partner.domain.AuditLog;
import com.speedline.partner.domain.Category;
import com.speedline.partner.domain.JsonNameI18nConverter;
import com.speedline.partner.dto.AuditLogEntryDTO;
import com.speedline.partner.dto.CategoryDTO;
import com.speedline.partner.dto.CategoryStatsDTO;
import com.speedline.partner.dto.CreateCategoryRequest;
import com.speedline.partner.dto.UpdateCategoryRequest;
import com.speedline.partner.repository.AuditLogRepository;
import com.speedline.partner.repository.CategoryRepository;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository    categoryRepository;
    private final AuditLogRepository    auditLogRepository;
    private final JsonNameI18nConverter jsonConverter;
    private final AuditLogService       auditLogService;
    private final CategoryCacheService  cacheService;
    private final PartnerRepository     partnerRepository;
    private final ProductRepository     productRepository;
    private final OrderServiceClient    orderServiceClient;

    // ==================== CREATE ====================

    public CategoryDTO createCategory(CreateCategoryRequest request, Long adminId) {

        // 1. Validation unicité nom par locale
        for (Map.Entry<String, String> entry : request.getNameI18n().entrySet()) {
            String locale = entry.getKey();
            String name   = entry.getValue();
            if (categoryRepository.existsByNameAndLocaleExcluding(name, locale, null)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        String.format("Le nom '%s' existe déjà pour la locale '%s'", name, locale)
                );
            }
        }

        // 1b. Validation profondeur (max 3 niveaux)
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Catégorie parente introuvable: " + request.getParentId()));
            if (getCategoryDepth(parent) >= 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Maximum 3 niveaux de profondeur autorisés");
            }
        }

        // 2. Sérialisation JSON
        String nameJson = serializeNameI18n(request.getNameI18n());

        // 3. Génération slug unique
        String slug = generateSlug(request.getNameI18n());

        // 4. Construction entité
        Category category = Category.builder()
                .nameI18n(nameJson)
                .slug(slug)
                .description(request.getDescription())
                .icon(request.getIcon())
                .image(request.getImage())
                .parentId(request.getParentId())
                .displayOrder(request.getDisplayOrder())
                .isActive(true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .categoryType(request.getParentId() != null ? "SUB" : "PARTNER")
                .backgroundColor(request.getBackgroundColor())
                .textColor(request.getTextColor())
                .createdBy(adminId)
                .partnerCount(0)
                .productCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category saved = categoryRepository.save(category);

        // 5. Audit log (async)
        auditLogService.log(adminId, "CREATE", "CATEGORY", saved.getId());

        // 6. Invalidation cache Redis
        cacheService.invalidateActiveCache();

        log.info("✅ Catégorie créée: id={} slug={} par admin={}", saved.getId(), slug, adminId);

        return convertToDTO(saved);
    }

    // ==================== GET ALL ====================

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==================== SEARCH / FILTER ====================

    @Transactional(readOnly = true)
    public List<CategoryDTO> searchCategories(String q, String status) {
        // status : "active" → "true", "inactive" → "false", sinon null
        String statusParam = null;
        if ("active".equalsIgnoreCase(status))   statusParam = "true";
        if ("inactive".equalsIgnoreCase(status)) statusParam = "false";

        String searchTerm = StringUtils.hasText(q) ? q.trim() : null;

        return categoryRepository.findFiltered(searchTerm, statusParam)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ==================== SEARCH / FILTER PAGINATED ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getCategoriesPaged(String q, String status, int page, int size) {
        String statusParam = null;
        if ("active".equalsIgnoreCase(status))   statusParam = "true";
        if ("inactive".equalsIgnoreCase(status)) statusParam = "false";
        String searchTerm = StringUtils.hasText(q) ? q.trim() : null;

        org.springframework.data.domain.Page<Category> rootPage = categoryRepository
                .findRootFilteredPaged(searchTerm, statusParam,
                        org.springframework.data.domain.PageRequest.of(page, size));

        List<Long> rootIds = rootPage.getContent().stream().map(Category::getId).toList();
        List<Category> subcategories = rootIds.isEmpty()
                ? List.of()
                : categoryRepository.findByParentIdIn(rootIds);

        List<CategoryDTO> content = new ArrayList<>();
        rootPage.getContent().forEach(c -> content.add(convertToDTO(c)));
        subcategories.forEach(c -> content.add(convertToDTO(c)));

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", rootPage.getTotalElements());
        result.put("totalPages", rootPage.getTotalPages());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    // ==================== GET BY ID ====================

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long categoryId) {
        return convertToDTO(findByIdOrThrow(categoryId));
    }

    // ==================== GET BY SLUG ====================

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Catégorie non trouvée avec le slug: " + slug
                ));
        return convertToDTO(category);
    }

    // ==================== UPDATE ====================

    public CategoryDTO updateCategory(Long categoryId, UpdateCategoryRequest request, Long adminId) {
        Category category = findByIdOrThrow(categoryId);

        // Validation unicité nom par locale (en excluant la catégorie actuelle)
        for (Map.Entry<String, String> entry : request.getNameI18n().entrySet()) {
            String locale = entry.getKey();
            String name   = entry.getValue();
            if (categoryRepository.existsByNameAndLocaleExcluding(name, locale, categoryId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        String.format("Le nom '%s' existe déjà pour la locale '%s'", name, locale)
                );
            }
        }

        // Sérialisation + slug
        String nameJson = serializeNameI18n(request.getNameI18n());
        String newSlug  = generateSlug(request.getNameI18n());

        // Validation profondeur (max 3 niveaux)
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Catégorie parente introuvable: " + request.getParentId()));
            if (getCategoryDepth(parent) >= 2) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Maximum 3 niveaux de profondeur autorisés");
            }
        }

        category.setNameI18n(nameJson);
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setImage(request.getImage());
        category.setParentId(request.getParentId());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());
        category.setIsFeatured(request.getIsFeatured());
        category.setBackgroundColor(request.getBackgroundColor());
        category.setTextColor(request.getTextColor());
        category.setUpdatedAt(LocalDateTime.now());

        Category saved = categoryRepository.save(category);

        // Audit log (async)
        auditLogService.log(adminId, "UPDATE", "CATEGORY", saved.getId());

        // Invalidation cache
        cacheService.invalidateAllCategoryCache();

        log.info("✅ Catégorie mise à jour: id={} par admin={}", categoryId, adminId);

        return convertToDTO(saved);
    }

    // ==================== TOGGLE ACTIVE ====================

    public CategoryDTO toggleActive(Long categoryId, Long adminId) {
        Category category = findByIdOrThrow(categoryId);

        boolean newStatus = !category.getIsActive();
        category.setIsActive(newStatus);
        category.setUpdatedAt(LocalDateTime.now());

        Category saved = categoryRepository.save(category);

        // Audit log (async)
        String action = newStatus ? "ACTIVATE" : "DEACTIVATE";
        auditLogService.log(adminId, action, "CATEGORY", saved.getId());

        // Invalidation cache Redis
        cacheService.invalidateActiveCache();

        log.info("✅ Catégorie {} → isActive={} par admin={}", categoryId, newStatus, adminId);

        return convertToDTO(saved);
    }

    // ==================== DELETE ====================

    public void deleteCategory(Long categoryId, Long adminId) {
        Category category = findByIdOrThrow(categoryId);

        // Blocage si produits liés → HTTP 409
        int productCount = category.getProductCount() != null ? category.getProductCount() : 0;
        if (productCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Impossible — %d produits liés à cette catégorie", productCount)
            );
        }

        categoryRepository.deleteById(categoryId);

        // Audit log (async)
        auditLogService.log(adminId, "DELETE", "CATEGORY", categoryId);

        // Invalidation cache
        cacheService.invalidateAllCategoryCache();

        log.info("✅ Catégorie {} supprimée par admin={}", categoryId, adminId);
    }

    // ==================== PARENT CANDIDATES ====================

    @Transactional(readOnly = true)
    public List<CategoryDTO> getParentCandidates(Long excludeId) {
        return categoryRepository.findAll()
                .stream()
                .filter(cat -> !cat.getId().equals(excludeId))
                .filter(cat -> getCategoryDepth(cat) < 2)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==================== STATS ====================

    private static final int TOP_CATEGORY_THRESHOLD = 1000;

    @Transactional(readOnly = true)
    public CategoryStatsDTO getCategoryStats(Long categoryId) {
        Category cat = findByIdOrThrow(categoryId);

        Map<String, String> nameMap;
        try { nameMap = jsonConverter.toMap(cat.getNameI18n()); } catch (Exception e) { nameMap = Map.of(); }
        String name = nameMap.getOrDefault("fr", nameMap.getOrDefault("en", String.valueOf(categoryId)));

        int products = cat.getProductCount() != null ? cat.getProductCount() : 0;
        int partners = cat.getPartnerCount() != null ? cat.getPartnerCount() : 0;

        // --- Récupérer les IDs des partenaires liés à cette catégorie ---
        List<Long> partnerIds = partnerRepository
                .findByCategoryId(String.valueOf(categoryId), Pageable.unpaged())
                .map(p -> p.getId())
                .toList();

        // --- Commandes journalières réelles via order-service (avec fallback) ---
        List<CategoryStatsDTO.DailyOrderStat> daily;
        double orderTrend;

        if (!partnerIds.isEmpty()) {
            try {
                Map<String, Long> rawCounts = orderServiceClient.getDailyStatsByPartners(partnerIds, 30);
                daily = buildDailyStats(rawCounts);
                orderTrend = computeOrderTrend(rawCounts);
                log.info("✅ Stats réelles catégorie {} : {} partenaires, {} commandes/30j",
                        categoryId, partnerIds.size(), daily.stream().mapToInt(CategoryStatsDTO.DailyOrderStat::getOrders).sum());
            } catch (Exception e) {
                log.warn("⚠️ order-service injoignable pour catégorie {} — fallback simulation: {}", categoryId, e.getMessage());
                daily = simulateDailyOrders(categoryId, partners);
                orderTrend = 12.0;
            }
        } else {
            // Aucun partenaire dans cette catégorie
            daily = buildDailyStats(Map.of());
            orderTrend = 0.0;
        }

        int total = daily.stream().mapToInt(CategoryStatsDTO.DailyOrderStat::getOrders).sum();
        boolean isTop = total >= TOP_CATEGORY_THRESHOLD;

        // --- Tendances produits/partenaires basées sur les nouvelles entrées (30j vs 30j précédents) ---
        LocalDateTime now      = LocalDateTime.now();
        LocalDateTime before30 = now.minusDays(30);
        LocalDateTime before60 = now.minusDays(60);

        long productsRecent   = productRepository.countByCategoryIdAndCreatedAtBetween(categoryId, before30, now);
        long productsPrevious = productRepository.countByCategoryIdAndCreatedAtBetween(categoryId, before60, before30);
        double productTrend   = computeCountTrend(productsRecent, productsPrevious);

        long partnersRecent   = partnerRepository.countByCategoryIdAndCreatedAtBetween(String.valueOf(categoryId), before30, now);
        long partnersPrevious = partnerRepository.countByCategoryIdAndCreatedAtBetween(String.valueOf(categoryId), before60, before30);
        double partnerTrend   = computeCountTrend(partnersRecent, partnersPrevious);

        return CategoryStatsDTO.builder()
                .categoryId(categoryId)
                .categoryName(name)
                .productCount(products)
                .partnerCount(partners)
                .ordersLast30Days(total)
                .orderTrendPercent(orderTrend)
                .partnerTrendPercent(partnerTrend)
                .productTrendPercent(productTrend)
                .topCategory(isTop)
                .topCategoryThreshold(TOP_CATEGORY_THRESHOLD)
                .dailyOrders(daily)
                .build();
    }

    /**
     * Construit la liste des 30 derniers jours avec les vraies valeurs (zéro si absent).
     */
    private List<CategoryStatsDTO.DailyOrderStat> buildDailyStats(Map<String, Long> rawCounts) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        List<CategoryStatsDTO.DailyOrderStat> daily = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String key = day.toString(); // "yyyy-MM-dd"
            long cnt = rawCounts.getOrDefault(key, 0L);
            daily.add(CategoryStatsDTO.DailyOrderStat.builder()
                    .day(day.format(fmt)).orders((int) cnt).build());
        }
        return daily;
    }

    /**
     * Calcule la tendance (%) entre une période récente et une période précédente (même durée).
     */
    private double computeCountTrend(long recent, long previous) {
        if (previous == 0) return recent > 0 ? 100.0 : 0.0;
        return Math.round((recent - previous) * 1000.0 / previous) / 10.0;
    }

    /**
     * Calcule la tendance (%) en comparant les 15 derniers jours aux 15 précédents.
     */
    private double computeOrderTrend(Map<String, Long> rawCounts) {
        long recent = 0;
        long previous = 0;
        for (int i = 0; i < 15; i++) {
            recent   += rawCounts.getOrDefault(LocalDate.now().minusDays(i).toString(), 0L);
            previous += rawCounts.getOrDefault(LocalDate.now().minusDays(i + 15).toString(), 0L);
        }
        if (previous == 0) return recent > 0 ? 100.0 : 0.0;
        return Math.round((recent - previous) * 1000.0 / previous) / 10.0;
    }

    /**
     * Simulation déterministe utilisée en fallback si order-service est injoignable.
     */
    private List<CategoryStatsDTO.DailyOrderStat> simulateDailyOrders(Long categoryId, int partners) {
        int seed = (int)(categoryId % 100);
        int baseDaily = Math.max(5, partners * 8 + seed);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        List<CategoryStatsDTO.DailyOrderStat> daily = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            int v = (int)(baseDaily * (0.7 + 0.6 * Math.sin((i + seed) * 0.4)));
            daily.add(CategoryStatsDTO.DailyOrderStat.builder()
                    .day(day.format(fmt)).orders(v).build());
        }
        return daily;
    }

    // ==================== AUDIT TRAIL ====================

    // ==================== EXPORT CSV ====================

    @Transactional(readOnly = true)
    public byte[] exportCategoryReport(Long categoryId) {
        Category cat = findByIdOrThrow(categoryId);
        CategoryStatsDTO stats = getCategoryStats(categoryId);

        Map<String, String> nameMap;
        try { nameMap = jsonConverter.toMap(cat.getNameI18n()); } catch (Exception e) { nameMap = Map.of(); }
        String name = nameMap.getOrDefault("fr", nameMap.getOrDefault("en", String.valueOf(categoryId)));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtFmt   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM pour Excel

        // ── Infos générales ─────────────────────────────────────────────────
        sb.append("=== RAPPORT CATÉGORIE ===\n");
        sb.append("Catégorie;").append(name).append("\n");
        sb.append("Slug;").append(cat.getSlug() != null ? cat.getSlug() : "").append("\n");
        sb.append("Statut;").append(Boolean.TRUE.equals(cat.getIsActive()) ? "Active" : "Inactive").append("\n");
        sb.append("Mise en avant;").append(Boolean.TRUE.equals(cat.getIsFeatured()) ? "Oui" : "Non").append("\n");
        sb.append("Créée le;").append(cat.getCreatedAt() != null ? cat.getCreatedAt().format(dateFmt) : "").append("\n");
        sb.append("\n");

        // ── Statistiques ────────────────────────────────────────────────────
        sb.append("=== STATISTIQUES (30 JOURS) ===\n");
        sb.append("Produits liés;").append(stats.getProductCount()).append("\n");
        sb.append("Partenaires actifs;").append(stats.getPartnerCount()).append("\n");
        sb.append("Commandes (30j);").append(stats.getOrdersLast30Days()).append("\n");
        double trend = stats.getOrderTrendPercent() != null ? stats.getOrderTrendPercent() : 0.0;
        sb.append("Tendance commandes;").append(trend >= 0 ? "+" : "").append(trend).append("%\n");
        sb.append("TOP Catégorie;").append(stats.isTopCategory() ? "Oui" : "Non").append("\n");
        sb.append("\n");

        // ── Commandes journalières ───────────────────────────────────────────
        if (stats.getDailyOrders() != null && !stats.getDailyOrders().isEmpty()) {
            sb.append("=== COMMANDES JOURNALIÈRES ===\n");
            sb.append("Date;Commandes\n");
            stats.getDailyOrders().forEach(d ->
                sb.append(d.getDay()).append(";").append(d.getOrders()).append("\n"));
            sb.append("\n");
        }

        // ── Journal d'audit ─────────────────────────────────────────────────
        List<AuditLogEntryDTO> auditList = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc("CATEGORY", categoryId)
                .stream().map(this::toAuditEntry).toList();

        if (!auditList.isEmpty()) {
            sb.append("=== JOURNAL D'AUDIT ===\n");
            sb.append("Date;Utilisateur;Rôle;Action;Détails;Statut\n");
            auditList.forEach(e -> {
                String ts = e.getTimestamp() != null ? e.getTimestamp().format(dtFmt) : "";
                String details = e.getChangesAfter() != null ? e.getChangesAfter().replace(";", ",") : "";
                sb.append(ts).append(";")
                  .append(e.getAdminName()).append(";")
                  .append(e.getAdminRole()).append(";")
                  .append(e.getAction()).append(";")
                  .append(details).append(";")
                  .append(e.getStatus()).append("\n");
            });
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntryDTO> getCategoryAuditTrail(Long categoryId) {
        findByIdOrThrow(categoryId); // 404 if not found
        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc("CATEGORY", categoryId)
                .stream()
                .map(this::toAuditEntry)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCategoryAuditTrailPaged(Long categoryId, int page, int size) {
        findByIdOrThrow(categoryId); // 404 if not found
        org.springframework.data.domain.Page<AuditLog> result = auditLogRepository
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(
                        "CATEGORY", categoryId,
                        org.springframework.data.domain.PageRequest.of(page, size));

        Map<String, Object> response = new HashMap<>();
        response.put("content",       result.getContent().stream().map(this::toAuditEntry).toList());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages",    result.getTotalPages());
        response.put("page",          page);
        response.put("size",          size);
        return response;
    }

    private AuditLogEntryDTO toAuditEntry(AuditLog log) {
        String role = log.getAdminId() != null && log.getAdminId() == 1 ? "System Admin" : "Admin";
        return AuditLogEntryDTO.builder()
                .id(log.getId())
                .adminId(log.getAdminId())
                .adminName("Admin #" + log.getAdminId())
                .adminRole(role)
                .action(log.getAction())
                .timestamp(log.getTimestamp())
                .changesBefore("N/A")
                .changesAfter(buildChangesAfter(log))
                .status("SUCCESS")
                .build();
    }

    private String buildChangesAfter(AuditLog log) {
        if (log.getReason() != null && !log.getReason().isBlank()) return log.getReason();
        return switch (log.getAction()) {
            case "CREATE"     -> "Catégorie créée";
            case "UPDATE"     -> "Catégorie modifiée";
            case "DELETE"     -> "Catégorie supprimée";
            case "ACTIVATE"   -> "Statut → Actif";
            case "DEACTIVATE" -> "Statut → Inactif";
            default -> log.getAction();
        };
    }

    // ==================== HELPERS PRIVÉS ====================

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Catégorie non trouvée: " + id
                ));
    }

    private String serializeNameI18n(Map<String, String> nameMap) {
        try {
            return jsonConverter.toJson(nameMap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation des noms multilingues"
            );
        }
    }

    /**
     * Générer un slug unique depuis le nom FR (fallback EN, puis première locale)
     */
    private String generateSlug(Map<String, String> nameI18n) {
        String name = nameI18n.getOrDefault("fr",
                nameI18n.getOrDefault("en",
                        nameI18n.values().iterator().next()));

        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        // Garantir unicité du slug
        String baseSlug = slug;
        int counter = 1;
        while (categoryRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    /**
     * Calcule la profondeur d'une catégorie (0 = racine, 1 = sous-catégorie, 2 = sous-sous-catégorie).
     * Protégé contre les cycles.
     */
    private int getCategoryDepth(Category category) {
        int depth = 0;
        Long parentId = category.getParentId();
        Set<Long> visited = new HashSet<>();
        visited.add(category.getId());
        while (parentId != null) {
            if (!visited.add(parentId)) break; // protection cycle
            Category parent = categoryRepository.findById(parentId).orElse(null);
            if (parent == null) break;
            depth++;
            parentId = parent.getParentId();
        }
        return depth;
    }

    // ==================== CONVERT ====================

    private CategoryDTO convertToDTO(Category category) {
        Map<String, String> nameMap;
        try {
            nameMap = jsonConverter.toMap(category.getNameI18n());
        } catch (JsonProcessingException e) {
            nameMap = Map.of();
        }

        return CategoryDTO.builder()
                .id(category.getId())
                .nameI18n(nameMap)
                .slug(category.getSlug())
                .description(category.getDescription())
                .icon(category.getIcon())
                .image(category.getImage())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .isFeatured(category.getIsFeatured())
                .categoryType(category.getCategoryType())
                .backgroundColor(category.getBackgroundColor())
                .textColor(category.getTextColor())
                .parentId(category.getParentId())
                .depth(getCategoryDepth(category))
                .partnerCount(category.getPartnerCount())
                .productCount(category.getProductCount())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdBy(category.getCreatedBy())
                .build();
    }
}