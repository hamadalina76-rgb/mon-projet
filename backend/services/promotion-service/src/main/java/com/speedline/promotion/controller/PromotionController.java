package com.speedline.promotion.controller;

import com.speedline.promotion.dto.*;
import com.speedline.promotion.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // ---------------------------------------------------------------- LIST --

    @GetMapping
    public ResponseEntity<PromotionResponse<PromotionPageResponse>> getPromotions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @RequestParam(required = false) String partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String column = sortBy.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(column).ascending()
                : Sort.by(column).descending();
        PromotionPageResponse body = promotionService.getPromotions(
                search, status, type, startFrom, startTo, partnerId, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(PromotionResponse.ok(body));
    }

    @GetMapping("/active")
    public ResponseEntity<PromotionResponse<List<PromotionDto>>> getActive() {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.getActivePromotions()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<PromotionResponse<PromotionStatisticsDto>> getStatistics() {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.getStatistics()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse<PromotionDetailDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.getById(id)));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<PromotionResponse<PromotionDto>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.getByCode(code)));
    }

    // ---------------------------------------------------------------- CRUD --

    @PostMapping
    public ResponseEntity<PromotionResponse<PromotionDto>> create(
            @Valid @RequestBody CreatePromotionRequest request) {
        PromotionDto created = promotionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                PromotionResponse.ok("Promotion créée avec succès", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse<PromotionDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionRequest request) {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PromotionResponse<Void>> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok(PromotionResponse.ok("Promotion supprimée", null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PromotionResponse<Void>> toggle(@PathVariable Long id) {
        promotionService.toggleActive(id);
        return ResponseEntity.ok(PromotionResponse.ok("Statut mis à jour", null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PromotionResponse<Void>> activate(@PathVariable Long id) {
        promotionService.activate(id);
        return ResponseEntity.ok(PromotionResponse.ok("Promotion activée", null));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PromotionResponse<Void>> deactivate(@PathVariable Long id) {
        promotionService.deactivate(id);
        return ResponseEntity.ok(PromotionResponse.ok("Promotion désactivée", null));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<PromotionResponse<PromotionDto>> duplicate(@PathVariable Long id) {
        PromotionDto copy = promotionService.duplicate(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                PromotionResponse.ok("Promotion dupliquée", copy));
    }

    // -------------------------------------------------- BUSINESS LOGIC ----

    @PostMapping("/validate")
    public ResponseEntity<PromotionResponse<ValidatePromotionResponse>> validate(
            @Valid @RequestBody ValidatePromotionRequest request) {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.validate(request)));
    }

    @PostMapping("/{code}/apply")
    public ResponseEntity<PromotionResponse<ValidatePromotionResponse>> apply(
            @PathVariable String code,
            @Valid @RequestBody ApplyPromotionRequest request) {
        ApplyPromotionRequest req = new ApplyPromotionRequest(
                code, request.userId(), request.orderId(),
                request.orderSubtotal(), request.deliveryFee(),
                request.partnerId(), request.categoryIds());
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.apply(req)));
    }

    @PostMapping("/{code}/revoke")
    public ResponseEntity<PromotionResponse<Void>> revoke(
            @PathVariable String code,
            @Valid @RequestBody RevokePromotionRequest request) {
        RevokePromotionRequest req = new RevokePromotionRequest(
                code, request.userId(), request.orderId());
        promotionService.revoke(req);
        return ResponseEntity.ok(PromotionResponse.ok("Promotion révoquée", null));
    }

    // ------------------------------------------------------------- STATS ---

    @GetMapping("/{id}/analytics")
    public ResponseEntity<PromotionResponse<PromotionAnalyticsDto>> analytics(
            @PathVariable Long id) {
        return ResponseEntity.ok(PromotionResponse.ok(promotionService.getAnalytics(id)));
    }
}
