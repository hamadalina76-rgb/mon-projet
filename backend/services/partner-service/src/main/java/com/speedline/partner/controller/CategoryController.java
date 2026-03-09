package com.speedline.partner.controller;

import com.speedline.partner.dto.CategoryDTO;
import com.speedline.partner.dto.CreateCategoryRequest;
import com.speedline.partner.dto.UpdateCategoryRequest;
import com.speedline.partner.service.CategoryService;
import com.speedline.partner.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

    @PostMapping("/upload-icon")
    public ResponseEntity<Map<String, String>> uploadIcon(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Admin-Id") Long adminId
    ) {
        String url = fileStorageService.storeIcon(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            @RequestHeader("X-Admin-Id") Long adminId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request, adminId));
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Recherche et filtrage des catégories.
     * GET /v1/categories/search?q=pizza&businessType=RESTAURANT&status=active
     */
    @GetMapping("/search")
    public ResponseEntity<List<CategoryDTO>> searchCategories(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(categoryService.searchCategories(q, businessType, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryDTO> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request,
            @RequestHeader("X-Admin-Id") Long adminId
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request, adminId));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<CategoryDTO> toggleActive(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") Long adminId   // ✅ adminId ajouté
    ) {
        return ResponseEntity.ok(categoryService.toggleActive(id, adminId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Id") Long adminId   // ✅ adminId ajouté
    ) {
        categoryService.deleteCategory(id, adminId);
        return ResponseEntity.noContent().build();
    }
}