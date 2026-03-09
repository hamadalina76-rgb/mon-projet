package com.speedline.partner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.speedline.partner.domain.Category;
import com.speedline.partner.domain.CategoryBusinessType;
import com.speedline.partner.domain.JsonNameI18nConverter;
import com.speedline.partner.dto.CategoryDTO;
import com.speedline.partner.dto.CreateCategoryRequest;
import com.speedline.partner.dto.UpdateCategoryRequest;
import com.speedline.partner.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository    categoryRepository;
    private final JsonNameI18nConverter jsonConverter;
    private final AuditLogService       auditLogService;
    private final CategoryCacheService  cacheService;

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
                .displayOrder(request.getDisplayOrder())
                .isActive(true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .categoryBusinessType(request.getCategoryBusinessType()) // ✅ Direct enum
                .categoryType(request.getCategoryType() != null ? request.getCategoryType() : "PARTNER")
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
    public List<CategoryDTO> searchCategories(String q, String businessType, String status) {
        // businessType : valeur brute depuis le frontend (ex: "RESTAURANT") ou null
        String businessTypeParam = null;
        if (StringUtils.hasText(businessType)) {
            try {
                // Valider que c'est un enum connu avant de passer au repo
                CategoryBusinessType.valueOf(businessType.toUpperCase());
                businessTypeParam = businessType.toUpperCase();
            } catch (IllegalArgumentException ignored) {}
        }

        // status : "active" → "true", "inactive" → "false", sinon null
        String statusParam = null;
        if ("active".equalsIgnoreCase(status))   statusParam = "true";
        if ("inactive".equalsIgnoreCase(status)) statusParam = "false";

        String searchTerm = StringUtils.hasText(q) ? q.trim() : null;

        return categoryRepository.findFiltered(searchTerm, businessTypeParam, statusParam)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
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

        category.setNameI18n(nameJson);
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setImage(request.getImage());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());
        category.setIsFeatured(request.getIsFeatured());
        category.setCategoryBusinessType(request.getCategoryBusinessType()); // ✅ Direct enum
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
                .categoryBusinessType(category.getCategoryBusinessType()) // ✅ Direct enum
                .categoryType(category.getCategoryType())
                .backgroundColor(category.getBackgroundColor())
                .textColor(category.getTextColor())
                .partnerCount(category.getPartnerCount())
                .productCount(category.getProductCount())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdBy(category.getCreatedBy())
                .build();
    }
}