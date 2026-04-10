package com.speedline.order.controller;

import com.speedline.order.domain.OrderStatus;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.dto.PartnerOrderHistorySummaryDTO;
import com.speedline.order.dto.checkout.CheckoutOrderRequest;
import jakarta.validation.Valid;
import com.speedline.order.service.OrderService;
import com.speedline.order.service.export.PartnerOrderExportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private final PartnerOrderExportService partnerOrderExportService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId,
            @RequestHeader(value = "X-User-Name", required = false) String authenticatedUserName,
            @Valid @RequestBody CheckoutOrderRequest request
    ) {
        final Long customerId = extractAuthenticatedUserId(authenticatedUserId);
        final OrderResponse created = orderService.createOrderFromCheckout(customerId, authenticatedUserName, request);
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

    /**
     * Liste paginée des commandes d'un partenaire avec filtrage serveur.
     * GET /orders/partners/{partnerId}?status=PENDING&search=ORD-123&sortBy=priority&sortDir=desc&page=0&size=10
     *
     * - status  : filtre optionnel (PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP, CANCELLED…)
     * - search  : recherche textuelle sur orderNumber et customerName
     * - sortBy  : priority (défaut, type Glovo : à traiter en premier puis plus récent) | orderTime | total | orderNumber | createdAt
     * - sortDir : asc | desc — défaut : desc (pour orderTime / total / nombre…)
     * - cancelledBy : uniquement si {@code status=CANCELLED} (ex. PARTNER pour refus partenaire)
     */
    @GetMapping("/partners/{partnerId}")
    public ResponseEntity<Page<OrderResponse>> getPartnerOrders(
            @PathVariable Long partnerId,
            @RequestParam(required = false)                       OrderStatus status,
            @RequestParam(required = false, defaultValue = "")    String      search,
            @RequestParam(defaultValue = "priority")              String      sortBy,
            @RequestParam(defaultValue = "desc")                  String      sortDir,
            @RequestParam(required = false)                       String      from,
            @RequestParam(required = false)                       String      to,
            @RequestParam(required = false)                       String      cancelledBy,
            @RequestParam(defaultValue = "0")                     int         page,
            @RequestParam(defaultValue = "10")                    int         size
    ) {
        final String rawSort = sortBy == null ? "" : sortBy.trim();
        final Pageable pageable;
        if ("priority".equalsIgnoreCase(rawSort) && status == null) {
            /* Tri « Glovo » : uniquement pour la liste sans filtre statut */
            pageable = PageRequest.of(page, size);
        } else if ("priority".equalsIgnoreCase(rawSort)) {
            /* Onglet filtré : priorité = ordre chronologique (plus récent d’abord) */
            final Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by("orderTime").ascending()
                    : Sort.by("orderTime").descending();
            pageable = PageRequest.of(page, size, sort);
        } else {
            final String safeField = sanitizePartnerOrderSortField(rawSort);
            final Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(safeField).ascending()
                    : Sort.by(safeField).descending();
            pageable = PageRequest.of(page, size, sort);
        }
        final LocalDateTime fromDt = parsePartnerOrderDate(from);
        final LocalDateTime toDt   = parsePartnerOrderDate(to);
        return ResponseEntity.ok(orderService.getPartnerOrdersFiltered(
                partnerId, status, search, pageable, rawSort, fromDt, toDt, cancelledBy));
    }

    /**
     * Export Excel (.xlsx) — mêmes filtres que {@code GET /orders/partners/{partnerId}} (historique).
     * GET /orders/partners/{partnerId}/export/excel?from=&to=&status=&search=&cancelledBy=&lang=
     */
    @GetMapping("/partners/{partnerId}/export/excel")
    public ResponseEntity<byte[]> exportPartnerOrdersExcel(
            @PathVariable Long partnerId,
            @RequestHeader(value = "X-Partner-Id", required = false) String authenticatedPartnerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String cancelledBy,
            @RequestParam(required = false, defaultValue = "fr") String lang
    ) {
        assertPartnerExportAccess(partnerId, authenticatedPartnerId);
        final LocalDateTime fromDt = parsePartnerOrderDate(from);
        final LocalDateTime toDt = parsePartnerOrderDate(to);
        final byte[] body = partnerOrderExportService.exportExcel(
                partnerId, status, search, fromDt, toDt, cancelledBy, lang);
        final String filename = "speedline-commandes-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    /**
     * Export PDF — mêmes filtres que {@code GET /orders/partners/{partnerId}}.
     */
    @GetMapping("/partners/{partnerId}/export/pdf")
    public ResponseEntity<byte[]> exportPartnerOrdersPdf(
            @PathVariable Long partnerId,
            @RequestHeader(value = "X-Partner-Id", required = false) String authenticatedPartnerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String cancelledBy,
            @RequestParam(required = false, defaultValue = "fr") String lang
    ) {
        assertPartnerExportAccess(partnerId, authenticatedPartnerId);
        final LocalDateTime fromDt = parsePartnerOrderDate(from);
        final LocalDateTime toDt = parsePartnerOrderDate(to);
        final byte[] body = partnerOrderExportService.exportPdf(
                partnerId, status, search, fromDt, toDt, cancelledBy, lang);
        final String filename = "speedline-commandes-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    private static void assertPartnerExportAccess(Long pathPartnerId, String xPartnerId) {
        if (xPartnerId == null || xPartnerId.isBlank()) {
            return;
        }
        try {
            final long headerPid = Long.parseLong(xPartnerId.trim());
            if (!Objects.equals(headerPid, pathPartnerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé pour ce partenaire");
            }
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Partner-Id invalide");
        }
    }

    private static LocalDateTime parsePartnerOrderDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String s = raw.trim();
            if (s.length() == 10) s = s + "T00:00:00";
            if (s.length() == 16) s = s + ":00";
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static final Set<String> ALLOWED_PARTNER_ORDER_SORT = Set.of(
            "orderTime", "total", "orderNumber", "createdAt"
    );

    private static String sanitizePartnerOrderSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "orderTime";
        }
        for (String allowed : ALLOWED_PARTNER_ORDER_SORT) {
            if (allowed.equalsIgnoreCase(sortBy)) {
                return allowed;
            }
        }
        return "orderTime";
    }

    /**
     * Compteurs par statut (chips), filtrés sur {@code orderTime} si {@code from}/{@code to} sont fournis
     * (mêmes formats que GET /orders/partners/{partnerId}).
     * GET /orders/partners/{partnerId}/counts?from=&to=
     */
    @GetMapping("/partners/{partnerId}/counts")
    public ResponseEntity<Map<String, Long>> getPartnerOrderCounts(
            @PathVariable Long partnerId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        final LocalDateTime fromDt = parsePartnerOrderDate(from);
        final LocalDateTime toDt   = parsePartnerOrderDate(to);
        return ResponseEntity.ok(orderService.getPartnerOrderCounts(partnerId, fromDt, toDt));
    }

    /**
     * Résumé KPI historique (total commandes, CA livrées, annulations, taux) sur {@code orderTime}.
     * GET /orders/partners/{partnerId}/history-summary?from=&to=
     */
    @GetMapping("/partners/{partnerId}/history-summary")
    public ResponseEntity<PartnerOrderHistorySummaryDTO> getPartnerOrderHistorySummary(
            @PathVariable Long partnerId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        final LocalDateTime fromDt = parsePartnerOrderDate(from);
        final LocalDateTime toDt   = parsePartnerOrderDate(to);
        return ResponseEntity.ok(orderService.getPartnerOrderHistorySummary(partnerId, fromDt, toDt));
    }

    /**
     * Historique des statuts d'une commande (filtres + pagination).
     * GET /orders/{id}/history?status=&actorType=&from=&to=&page=&size=
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<Page<OrderResponse.StatusHistoryDTO>> getOrderHistory(
            @PathVariable Long id,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        final int safePage = Math.max(0, page);
        final int safeSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(orderService.getOrderHistory(id, status, actorType, from, to, pageable));
    }

    /** Conservé pour rétro-compatibilité éventuelle (anciens clients). */
    @GetMapping("/partners/{partnerId}/status/{status}")
    public ResponseEntity<Page<OrderResponse>> getPartnerOrdersByStatus(
            @PathVariable Long partnerId,
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderTime"));
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

    /**
     * Ticket cuisine HTML (80&nbsp;mm) pour impression navigateur — partenaire propriétaire uniquement.
     * GET /orders/{id}/partner/kitchen-ticket?partnerId=… ou header X-Partner-Id
     */
    @GetMapping(value = "/{id}/partner/kitchen-ticket", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getPartnerKitchenTicket(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) String authenticatedPartnerId,
            @RequestParam(value = "partnerId", required = false) Long partnerIdParam
    ) {
        final Long partnerId = resolveActorId(authenticatedPartnerId, partnerIdParam, "X-Partner-Id");
        final String html = orderService.buildKitchenTicketHtml(id, partnerId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
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

    /**
     * Liste paginée de toutes les commandes (admin panel).
     * GET /orders/admin?page=0&size=50&status=PENDING&paymentMethod=CASH&search=ORD&sort=createdAt,desc&startDate=2026-04-01T00:00:00&endDate=2026-04-09T23:59:59
     */
    @GetMapping("/admin")
    public ResponseEntity<Page<OrderResponse>> getAdminOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        final String[] sortParts = sort.split(",");
        final String sortField = sanitizeAdminSortField(sortParts[0]);
        final Sort.Direction dir = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortField));
        final LocalDateTime parsedStart = parseDateTime(startDate);
        final LocalDateTime parsedEnd = parseDateTime(endDate);
        return ResponseEntity.ok(orderService.getAdminOrders(status, paymentMethod, search,
                parsedStart, parsedEnd, partnerId, courierId, amountMin, amountMax, pageable));
    }

    private static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static final Set<String> ALLOWED_ADMIN_SORT = Set.of(
            "createdAt", "orderTime", "total", "orderNumber", "status"
    );

    private static String sanitizeAdminSortField(String raw) {
        if (raw == null || raw.isBlank()) return "createdAt";
        for (String allowed : ALLOWED_ADMIN_SORT) {
            if (allowed.equalsIgnoreCase(raw.trim())) return allowed;
        }
        return "createdAt";
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
