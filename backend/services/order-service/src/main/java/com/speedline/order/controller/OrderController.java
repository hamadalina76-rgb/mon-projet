package com.speedline.order.controller;

import com.speedline.order.domain.OrderStatus;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.dto.checkout.CheckoutOrderRequest;
import jakarta.validation.Valid;
import com.speedline.order.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST Controller pour Order
 *
 * Endpoints:
 * POST   /orders - Créer commande
 * GET    /orders/{id} - Détails commande
 * GET    /orders - Liste commandes
 * PUT    /orders/{id}/status - Changer statut
 * DELETE /orders/{id} - Annuler commande
 * GET    /orders/{id}/track - Tracking
 * GET    /orders/customers/{customerId}/orders - Historique client (paginé)
 * GET    /partners/{id}/orders - Commandes partenaire
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId,
            @Valid @RequestBody CheckoutOrderRequest request
    ) {
        final Long customerId = extractAuthenticatedUserId(authenticatedUserId);
        final OrderResponse created = orderService.createOrderFromCheckout(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/{id}/delivery-slots")
    public ResponseEntity<Map<String, Object>> getDeliverySlots(@PathVariable Long id) {
        final OrderResponse order = orderService.getOrderById(id);

        final LocalDateTime baseTime = order.getEstimatedDeliveryTime() != null
                ? order.getEstimatedDeliveryTime()
                : LocalDateTime.now().plusMinutes(45);

        final LocalDateTime aligned = baseTime.truncatedTo(ChronoUnit.MINUTES);
        final int minute = aligned.getMinute();
        final int adjustment = minute % 30 == 0 ? 0 : 30 - (minute % 30);
        final LocalDateTime firstSlot = aligned.plusMinutes(adjustment);

        final List<Map<String, Object>> slots = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final LocalDateTime start = firstSlot.plusMinutes((long) i * 30L);
            final LocalDateTime end = start.plusMinutes(30);

            slots.add(Map.of(
                    "startAt", start,
                    "endAt", end,
                    "available", Boolean.TRUE
            ));
        }

        return ResponseEntity.ok(Map.of(
                "orderId", id,
                "slots", slots
        ));
    }

    /**
     * Historique des commandes d'un client (pour admin panel - fiche client).
     * GET /orders/customers/{customerId}/orders?page=0&size=20
     */
    @GetMapping("/customers/{customerId}/orders")
    public ResponseEntity<Page<OrderResponse>> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getCustomerOrders(customerId, pageable));
    }

    @GetMapping("/partners/{partnerId}")
    public ResponseEntity<Page<OrderResponse>> getPartnerOrders(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getPartnerOrders(partnerId, pageable));
    }

    @GetMapping("/partners/{partnerId}/status/{status}")
    public ResponseEntity<Page<OrderResponse>> getPartnerOrdersByStatus(
            @PathVariable Long partnerId,
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getPartnerOrdersByStatus(partnerId, status, pageable));
    }

    @GetMapping("/partners/{partnerId}/active")
    public ResponseEntity<List<OrderResponse>> getActivePartnerOrders(@PathVariable Long partnerId) {
        return ResponseEntity.ok(orderService.getActiveOrdersByPartner(partnerId));
    }

    @GetMapping("/couriers/{courierId}")
    public ResponseEntity<Page<OrderResponse>> getCourierOrders(
            @PathVariable Long courierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getCourierOrders(courierId, pageable));
    }

    @GetMapping("/couriers/{courierId}/active")
    public ResponseEntity<List<OrderResponse>> getActiveCourierOrders(@PathVariable Long courierId) {
        return ResponseEntity.ok(orderService.getActiveOrdersByCourier(courierId));
    }

    @GetMapping("/awaiting-courier")
    public ResponseEntity<List<OrderResponse>> getOrdersAwaitingCourier() {
        return ResponseEntity.ok(orderService.getOrdersAwaitingCourier());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request
    ) {
        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut est obligatoire");
        }

        return ResponseEntity.ok(orderService.updateStatus(
                id,
                request.getStatus(),
                request.getActorType(),
                request.getActorId(),
                request.getNotes()
        ));
    }

    @PostMapping("/{id}/partner/confirm")
    public ResponseEntity<OrderResponse> confirmOrderByPartner(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) String authenticatedPartnerId,
            @RequestBody(required = false) PartnerConfirmRequest request
    ) {
        final Long partnerId = resolveActorId(authenticatedPartnerId, request == null ? null : request.getPartnerId(), "X-Partner-Id");
        final Integer estimatedPrepTime = request == null ? null : request.getEstimatedPrepTime();
        return ResponseEntity.ok(orderService.confirmOrder(id, partnerId, estimatedPrepTime));
    }

    @PostMapping("/{id}/partner/ready")
    public ResponseEntity<OrderResponse> markOrderAsReadyByPartner(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) String authenticatedPartnerId,
            @RequestBody(required = false) PartnerActionRequest request
    ) {
        final Long partnerId = resolveActorId(authenticatedPartnerId, request == null ? null : request.getPartnerId(), "X-Partner-Id");
        return ResponseEntity.ok(orderService.markAsReady(id, partnerId));
    }

    @PostMapping("/{id}/courier/assign")
    public ResponseEntity<OrderResponse> assignCourier(
            @PathVariable Long id,
            @RequestBody CourierAssignRequest request
    ) {
        if (request == null || request.getCourierId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "courierId est obligatoire");
        }
        return ResponseEntity.ok(orderService.assignCourier(id, request.getCourierId()));
    }

    @PostMapping("/{id}/courier/picked-up")
    public ResponseEntity<OrderResponse> markAsPickedUp(
            @PathVariable Long id,
            @RequestHeader(value = "X-Courier-Id", required = false) String authenticatedCourierId,
            @RequestBody(required = false) CourierActionRequest request
    ) {
        final Long courierId = resolveActorId(authenticatedCourierId, request == null ? null : request.getCourierId(), "X-Courier-Id");
        return ResponseEntity.ok(orderService.markAsPickedUp(id, courierId));
    }

    @PostMapping("/{id}/courier/in-delivery")
    public ResponseEntity<OrderResponse> markAsInDelivery(
            @PathVariable Long id,
            @RequestHeader(value = "X-Courier-Id", required = false) String authenticatedCourierId,
            @RequestBody(required = false) CourierActionRequest request
    ) {
        final Long courierId = resolveActorId(authenticatedCourierId, request == null ? null : request.getCourierId(), "X-Courier-Id");
        return ResponseEntity.ok(orderService.markAsInDelivery(id, courierId));
    }

    @PostMapping("/{id}/courier/delivered")
    public ResponseEntity<OrderResponse> markAsDelivered(
            @PathVariable Long id,
            @RequestHeader(value = "X-Courier-Id", required = false) String authenticatedCourierId,
            @RequestBody(required = false) CourierActionRequest request
    ) {
        final Long courierId = resolveActorId(authenticatedCourierId, request == null ? null : request.getCourierId(), "X-Courier-Id");
        return ResponseEntity.ok(orderService.markAsDelivered(id, courierId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        final String cancelledBy = request == null ? "CUSTOMER" : request.getCancelledBy();
        final Long actorId = request == null ? null : request.getActorId();
        final String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(orderService.cancelOrder(id, cancelledBy, actorId, reason));
    }

    /**
     * Stats journalières de commandes pour une liste de partenaires (endpoint interne).
     * Consommé par partner-service via Feign pour les statistiques des catégories.
     * GET /orders/internal/stats/daily?partnerIds=1,2,3&days=30
     */
    @GetMapping("/internal/stats/daily")
    public ResponseEntity<Map<String, Long>> getDailyStatsByPartners(
            @RequestParam List<Long> partnerIds,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(orderService.getDailyStatsByPartners(partnerIds, days));
    }

    private Long extractAuthenticatedUserId(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }

        try {
            return Long.parseLong(authenticatedUserId.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-User-Id header");
        }
    }

    private Long resolveActorId(String actorHeader, Long actorIdFromBody, String headerName) {
        final Long parsedFromHeader = parseOptionalLong(actorHeader);
        if (parsedFromHeader != null) {
            if (actorIdFromBody != null && !Objects.equals(parsedFromHeader, actorIdFromBody)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID acteur incohérent entre header et body");
            }
            return parsedFromHeader;
        }

        if (actorIdFromBody == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing " + headerName + " header");
        }

        return actorIdFromBody;
    }

    private Long parseOptionalLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric header value");
        }
    }

    @Data
    public static class UpdateStatusRequest {
        private OrderStatus status;
        private String actorType;
        private Long actorId;
        private String notes;
    }

    @Data
    public static class PartnerConfirmRequest {
        private Long partnerId;
        private Integer estimatedPrepTime;
    }

    @Data
    public static class PartnerActionRequest {
        private Long partnerId;
    }

    @Data
    public static class CourierAssignRequest {
        private Long courierId;
    }

    @Data
    public static class CourierActionRequest {
        private Long courierId;
    }

    @Data
    public static class CancelOrderRequest {
        private String cancelledBy;
        private Long actorId;
        private String reason;
    }
}
