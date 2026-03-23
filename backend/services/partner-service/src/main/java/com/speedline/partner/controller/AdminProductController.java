package com.speedline.partner.controller;

import com.speedline.partner.dto.request.RejectProductRequest;
import com.speedline.partner.dto.response.ProductResponse;
import com.speedline.partner.service.MenuProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for product moderation (PENDING -> APPROVED/REJECTED).
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final MenuProductService menuProductService;

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin: get pending products page={}, size={}", page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ProductResponse> pending = menuProductService.getPendingProducts(pageable);
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/{productId}/approve")
    public ResponseEntity<?> approveProduct(
            @PathVariable Long productId,
            HttpServletRequest request) {
        Long adminId = getCurrentAdminId(request);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-User-Id header (admin user id)."));
        }
        log.info("Admin: approving productId={} adminId={}", productId, adminId);

        ProductResponse product = menuProductService.approveProduct(productId, adminId);
        return ResponseEntity.ok(Map.of(
                "message", "Product approved successfully",
                "product", product
        ));
    }

    @PostMapping("/{productId}/reject")
    public ResponseEntity<?> rejectProduct(
            @PathVariable Long productId,
            @Valid @RequestBody RejectProductRequest body,
            HttpServletRequest request) {
        Long adminId = getCurrentAdminId(request);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-User-Id header (admin user id)."));
        }
        log.info("Admin: rejecting productId={} adminId={} reason={}", productId, adminId, body.getReason());

        ProductResponse product = menuProductService.rejectProduct(productId, adminId, body.getReason());
        return ResponseEntity.ok(Map.of(
                "message", "Product rejected successfully",
                "product", product
        ));
    }

    private Long getCurrentAdminId(HttpServletRequest request) {
        String h = request.getHeader("X-User-Id");
        if (h == null || h.isBlank()) {
            h = request.getHeader("X-Admin-Id");
        }
        if (h == null || h.isBlank()) return null;
        try {
            return Long.parseLong(h);
        } catch (Exception e) {
            return null;
        }
    }
}

