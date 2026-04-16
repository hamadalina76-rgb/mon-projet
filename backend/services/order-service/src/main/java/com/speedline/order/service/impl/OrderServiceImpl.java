package com.speedline.order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.order.client.PartnerServiceClient;
import com.speedline.order.client.PromotionServiceClient;
import com.speedline.order.client.UserServiceClient;
import com.speedline.order.client.dto.CustomerSnapshot;
import com.speedline.order.client.dto.AddressSnapshot;
import com.speedline.order.client.LocationServiceClient;
import com.speedline.order.client.dto.ZoneSnapshot;
import com.speedline.order.client.dto.PartnerSnapshot;
import com.speedline.order.client.dto.ProductSnapshot;
import com.speedline.order.client.dto.promotion.PromotionApiResponse;
import com.speedline.order.client.dto.promotion.PromotionApplyRequest;
import com.speedline.order.client.dto.promotion.PromotionValidateRequest;
import com.speedline.order.client.dto.promotion.PromotionValidateResponse;
import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderItem;
import com.speedline.order.domain.OrderStatus;
import com.speedline.order.domain.OrderStatusHistory;
import com.speedline.order.dto.CreateOrderRequest;
import com.speedline.order.dto.OrderItemDTO;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.dto.AdminLogDTO;
import com.speedline.order.dto.OrderStatsResponse;
import com.speedline.order.dto.PartnerOrderHistorySummaryDTO;
import com.speedline.order.dto.cart.CartItemPayload;
import com.speedline.order.dto.cart.CartResponse;
import com.speedline.order.dto.checkout.CheckoutOrderRequest;
import com.speedline.order.event.OrderCreatedEvent;
import com.speedline.order.event.OrderStatusChangedEvent;
import com.speedline.order.event.producer.OrderEventProducer;
import com.speedline.order.repository.OrderItemRepository;
import com.speedline.order.repository.OrderRepository;
import com.speedline.order.repository.OrderStatusHistoryRepository;
import com.speedline.order.service.CartService;
import com.speedline.order.service.OrderService;
import com.speedline.order.service.kitchen.KitchenTicketHtmlBuilder;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implémentation du service de gestion des commandes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
        private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.PICKED_UP,
            OrderStatus.IN_DELIVERY
        );
        private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED),
            OrderStatus.READY_FOR_PICKUP, Set.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED),
            OrderStatus.PICKED_UP, Set.of(OrderStatus.IN_DELIVERY, OrderStatus.CANCELLED),
            OrderStatus.IN_DELIVERY, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
        );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartService cartService;
    private final PartnerServiceClient partnerServiceClient;
    private final PromotionServiceClient promotionServiceClient;
    private final UserServiceClient userServiceClient;
    private final LocationServiceClient locationServiceClient;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    // ==================== CRÉATION ====================

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw badRequest("Le payload de création de commande est obligatoire");
        }

        if (request.getCustomerId() == null) {
            throw badRequest("L'identifiant client est obligatoire");
        }

        if (request.getPartnerId() == null) {
            throw badRequest("L'identifiant partenaire est obligatoire");
        }

        if (request.getPaymentMethod() == null) {
            throw badRequest("La méthode de paiement est obligatoire");
        }

        final List<CartItemPayload> cartItems = toCartPayloads(request);
        if (cartItems.isEmpty()) {
            throw badRequest("La commande doit contenir au moins un article");
        }

        final String paymentMethod = request.getPaymentMethod().name();
        final String addressId = request.getDeliveryAddressId() == null
                ? null
                : String.valueOf(request.getDeliveryAddressId());

        return createOrderInternal(
                request.getCustomerId(),
                null,
                cartItems,
                request.getPromoCode(),
                addressId,
                null,
                paymentMethod,
            false,
            request.getIsScheduled(),
            request.getScheduledDeliveryTime()
        );
    }

    @Override
    @Transactional
    public OrderResponse createOrderFromCheckout(Long customerId, String customerName, CheckoutOrderRequest request) {
        if (customerId == null) {
            throw badRequest("L'identifiant client est obligatoire");
        }

        if (request == null) {
            throw badRequest("Le payload checkout est obligatoire");
        }

        final List<CartItemPayload> cartItems = resolveCartItemsForCheckout(customerId, request);
        mergeKitchenNotesFromCheckoutPayload(cartItems, request.getCartItems());
        if (cartItems.isEmpty()) {
            throw badRequest("Le panier est vide");
        }

        return createOrderInternal(
                customerId,
                customerName,
                cartItems,
                request.getPromoCode(),
                request.getAddressId(),
                request.getDeliveryAddressDetails(),
                request.getPaymentMethod(),
            true,
            request.getIsScheduled(),
            resolveRequestedSchedule(request)
        );
    }

    // ==================== LECTURE ====================

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable: " + orderId
                ));

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw badRequest("Le numéro de commande est obligatoire");
        }

        final Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable: " + orderNumber
                ));

        return toResponse(order);
    }

    // ==================== MISE À JOUR DU STATUT ====================

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus, String actorType, Long actorId, String notes) {
        if (newStatus == null) {
            throw badRequest("Le nouveau statut est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        final OrderStatus previousStatus = order.getStatus();
        final String normalizedActorType = normalizeActorType(actorType);
        applyStatusTransition(
                order,
                newStatus,
                normalizedActorType,
                actorId,
                notes,
                "Statut mis à jour"
        );
            maybePublishOrderAcceptedEvent(order, previousStatus, normalizedActorType, actorId);
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(Long orderId, Long partnerId, Integer estimatedPrepTime) {
        if (partnerId == null) {
            throw badRequest("L'identifiant partenaire est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        assertPartnerOwnsOrder(order, partnerId);
        final OrderStatus previousStatus = order.getStatus();

        final String historyNotes;
        if (estimatedPrepTime != null && estimatedPrepTime > 0) {
            final int prepMinutes = Math.max(10, estimatedPrepTime);
            historyNotes = "PREP_MINUTES:" + prepMinutes;
            order.setSuggestedPreparationMinutes(prepMinutes);
        } else {
            historyNotes = null;
        }

        applyStatusTransition(
                order,
                OrderStatus.PREPARING,
                "PARTNER",
                partnerId,
                historyNotes,
                "Commande confirmée par le partenaire"
        );

        if (estimatedPrepTime != null && estimatedPrepTime > 0 && !Boolean.TRUE.equals(order.getIsScheduled())) {
            final int prepMinutes = Math.max(10, estimatedPrepTime);
            order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(prepMinutes + 15L));
            orderRepository.save(order);
        }

        maybePublishOrderAcceptedEvent(order, previousStatus, "PARTNER", partnerId);

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markAsReady(Long orderId, Long partnerId) {
        if (partnerId == null) {
            throw badRequest("L'identifiant partenaire est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        assertPartnerOwnsOrder(order, partnerId);

        applyStatusTransition(
                order,
                OrderStatus.READY_FOR_PICKUP,
                "PARTNER",
                partnerId,
                null,
                "Commande prête pour récupération"
        );

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String cancelledBy, Long actorId, String reason) {
        final Order order = getOrderOrThrow(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette commande ne peut plus être annulée");
        }

        final String normalizedActorType = normalizeActorType(cancelledBy);
        order.setCancellationReason(trimToNull(reason));
        order.setCancelledBy(normalizedActorType);

        applyStatusTransition(
                order,
                OrderStatus.CANCELLED,
                normalizedActorType,
                actorId,
                reason,
                "Commande annulée"
        );

        return toResponse(order);
    }

    // ==================== ASSIGNATION LIVREUR ====================

    @Override
    @Transactional
    public OrderResponse assignCourier(Long orderId, Long courierId) {
        if (courierId == null) {
            throw badRequest("L'identifiant livreur est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un livreur ne peut être assigné que sur une commande READY_FOR_PICKUP"
            );
        }

        order.setCourierId(courierId);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .orderId(order.getId())
                .previousStatus(order.getStatus())
                .status(order.getStatus())
                .description("Livreur assigné")
                .updatedBy("SYSTEM")
                .actorType("SYSTEM")
                .notes("courierId=" + courierId)
                .build());

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markAsPickedUp(Long orderId, Long courierId) {
        if (courierId == null) {
            throw badRequest("L'identifiant livreur est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        assertCourierAssigned(order, courierId);

        applyStatusTransition(
                order,
                OrderStatus.PICKED_UP,
                "COURIER",
                courierId,
                null,
                "Commande récupérée par le livreur"
        );

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markAsInDelivery(Long orderId, Long courierId) {
        if (courierId == null) {
            throw badRequest("L'identifiant livreur est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        assertCourierAssigned(order, courierId);

        applyStatusTransition(
                order,
                OrderStatus.IN_DELIVERY,
                "COURIER",
                courierId,
                null,
                "Commande en cours de livraison"
        );

        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse markAsDelivered(Long orderId, Long courierId) {
        if (courierId == null) {
            throw badRequest("L'identifiant livreur est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        assertCourierAssigned(order, courierId);

        applyStatusTransition(
                order,
                OrderStatus.DELIVERED,
                "COURIER",
                courierId,
                null,
                "Commande livrée"
        );

        if (order.getPaymentStatus() == Order.PaymentStatus.PENDING
                && (order.getPaymentMethod() == Order.PaymentMethod.CASH
                || order.getPaymentMethod() == Order.PaymentMethod.CARD_ON_DELIVERY)) {
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            orderRepository.save(order);
        }

        return toResponse(order);
    }

    // ==================== RECHERCHE CLIENT ====================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdAndStatusIn(customerId, ACTIVE_STATUSES).stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderResponse toResponse(com.speedline.order.domain.Order o) {
        final List<OrderItemDTO> items = mapOrderItems(o.getId());
        final List<OrderResponse.StatusHistoryDTO> history = mapStatusHistory(o.getId());

        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerId(o.getCustomerId())
                .partnerId(o.getPartnerId())
                .courierId(o.getCourierId())
                .customerName(o.getCustomerName())
                .customerPhone(o.getCustomerPhone())
                .partnerName(o.getPartnerName())
                .partnerAddress(o.getPartnerAddress())
                .partnerPhone(o.getPartnerPhone())
                .courierName(o.getCourierName())
                .courierPhone(o.getCourierPhone())
                .status(o.getStatus())
                .statusLabel(o.getStatus() != null ? o.getStatus().name() : null)
                .type(o.getType())
                .subtotal(o.getSubtotal())
                .deliveryFee(o.getDeliveryFee())
                .serviceFee(o.getServiceFee())
                .tax(o.getTax())
                .discount(o.getDiscount())
                .promoCode(o.getPromoCode())
                .tip(o.getTip())
                .total(o.getTotal())
                .deliveryAddress(parseDeliveryAddress(o.getDeliveryAddressJson()))
                .deliveryInstructions(o.getDeliveryInstructions())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .orderTime(o.getOrderTime())
                .estimatedDeliveryTime(o.getEstimatedDeliveryTime())
                .actualDeliveryTime(o.getActualDeliveryTime())
                .suggestedPreparationMinutes(o.getSuggestedPreparationMinutes())
                .isScheduled(o.getIsScheduled())
                .scheduledDeliveryTime(o.getScheduledDeliveryTime())
                .customerNotes(o.getCustomerNotes())
                .cancellationReason(o.getCancellationReason())
                .items(items)
                .statusHistory(history)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .deliveryTimeMinutes(o.getDeliveryTimeMinutes())
                .isCancellable(o.isCancellable())
                .isCompleted(o.isCompleted())
                .build();
    }

    // ==================== RECHERCHE PARTENAIRE ====================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getPartnerOrders(Long partnerId, Pageable pageable) {
        return orderRepository.findByPartnerId(partnerId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getPartnerOrdersByStatus(Long partnerId, OrderStatus status, Pageable pageable) {
        if (status == null) {
            throw badRequest("Le statut est obligatoire");
        }
        return orderRepository.findByPartnerIdAndStatus(partnerId, status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrdersByPartner(Long partnerId) {
        return orderRepository.findByPartnerIdAndStatusIn(partnerId, ACTIVE_STATUSES).stream()
                .map(this::toResponse)
                .toList();
    }

    // ==================== RECHERCHE LIVREUR ====================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getCourierOrders(Long courierId, Pageable pageable) {
        return orderRepository.findByCourierId(courierId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrdersByCourier(Long courierId) {
        return orderRepository.findByCourierIdAndStatusIn(courierId, ACTIVE_STATUSES).stream()
                .map(this::toResponse)
                .toList();
    }

    // ==================== RECHERCHE GÉNÉRALE ====================

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        if (status == null) {
            throw badRequest("Le statut est obligatoire");
        }
        return orderRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        if (start == null || end == null) {
            throw badRequest("La période de recherche est obligatoire");
        }
        if (end.isBefore(start)) {
            throw badRequest("La date de fin doit être postérieure à la date de début");
        }
        return orderRepository.findByDateRange(start, end, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersAwaitingCourier() {
        return orderRepository.findOrdersAwaitingCourier().stream()
                .map(this::toResponse)
                .toList();
    }

    // ==================== TRACKING ====================

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackOrder(Long orderId) {
        return getOrderById(orderId);
    }

    @Override
    @Transactional
    public OrderResponse updateEstimatedDeliveryTime(Long orderId, LocalDateTime estimatedTime) {
        if (estimatedTime == null) {
            throw badRequest("L'heure estimée de livraison est obligatoire");
        }

        final Order order = getOrderOrThrow(orderId);
        order.setEstimatedDeliveryTime(estimatedTime);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .orderId(order.getId())
                .previousStatus(order.getStatus())
                .status(order.getStatus())
                .description("Heure estimée de livraison mise à jour")
                .updatedBy("SYSTEM")
                .actorType("SYSTEM")
                .notes("estimatedTime=" + estimatedTime)
                .build());

        return toResponse(order);
    }

    // ==================== STATS INTERNES ====================

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getDailyStatsByPartners(List<Long> partnerIds, int days) {
        if (partnerIds == null || partnerIds.isEmpty()) {
            return java.util.Map.of();
        }
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = orderRepository.countDailyByPartnerIdsSince(partnerIds, since);
        java.util.Map<String, Long> result = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            String day = row[0].toString(); // "yyyy-MM-dd"
            Long cnt = ((Number) row[1]).longValue();
            result.put(day, cnt);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAdminOrders(OrderStatus status, String paymentMethod, String paymentStatus,
                                              String search,
                                              LocalDateTime startDate, LocalDateTime endDate,
                                              Long partnerId, Long courierId,
                                              BigDecimal amountMin, BigDecimal amountMax,
                                              Pageable pageable) {
        final Order.PaymentMethod pm = parsePaymentMethodOrNull(paymentMethod);
        final Order.PaymentStatus ps = parsePaymentStatusOrNull(paymentStatus);
        final String safeSearch = search == null ? "" : search.trim();
        return orderRepository.findAllAdmin(status, pm, ps, safeSearch, startDate, endDate,
                partnerId, courierId, amountMin, amountMax, pageable)
                .map(this::toResponse);
    }

    private Order.PaymentMethod parsePaymentMethodOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Order.PaymentMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Order.PaymentStatus parsePaymentStatusOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Order.PaymentStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatsResponse getAdminStats(LocalDate date, String granularity) {
        final LocalDateTime dayStart = date.atStartOfDay();
        final LocalDateTime dayEnd = date.atTime(23, 59, 59);

        // KPIs — current period
        long total = orderRepository.countByCreatedAtBetween(dayStart, dayEnd);
        long active = orderRepository.countActiveOrders();
        long cancelled = orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.CANCELLED, dayStart, dayEnd);
        double cancelRate = total > 0 ? (cancelled * 100.0 / total) : 0;
        Double avgMinRaw = orderRepository.avgDeliveryMinutesByDateRange(dayStart, dayEnd);
        double avgMin = avgMinRaw != null ? avgMinRaw : 0.0;
        BigDecimal revenue = orderRepository.sumRevenueByDateRange(dayStart, dayEnd);

        // KPIs — previous period (yesterday) for trend comparison
        LocalDate prevDate = date.minusDays(1);
        final LocalDateTime prevStart = prevDate.atStartOfDay();
        final LocalDateTime prevEnd = prevDate.atTime(23, 59, 59);
        long prevTotal = orderRepository.countByCreatedAtBetween(prevStart, prevEnd);
        long prevCancelled = orderRepository.countByStatusAndCreatedAtBetween(OrderStatus.CANCELLED, prevStart, prevEnd);
        double prevCancelRate = prevTotal > 0 ? (prevCancelled * 100.0 / prevTotal) : 0;
        Double prevAvgMinRaw = orderRepository.avgDeliveryMinutesByDateRange(prevStart, prevEnd);
        double prevAvgMin = prevAvgMinRaw != null ? prevAvgMinRaw : 0.0;
        BigDecimal prevRevenue = orderRepository.sumRevenueByDateRange(prevStart, prevEnd);

        OrderStatsResponse.KpiData kpis = OrderStatsResponse.KpiData.builder()
                .totalOrders(total)
                .activeOrders(active)
                .cancelRate(Math.round(cancelRate * 10.0) / 10.0)
                .avgDeliveryMinutes(Math.round(avgMin * 10.0) / 10.0)
                .totalRevenueTND(revenue != null ? revenue : BigDecimal.ZERO)
                .prevTotalOrders(prevTotal)
                .prevCancelRate(Math.round(prevCancelRate * 10.0) / 10.0)
                .prevAvgDeliveryMinutes(Math.round(prevAvgMin * 10.0) / 10.0)
                .prevRevenueTND(prevRevenue != null ? prevRevenue : BigDecimal.ZERO)
                .build();

        // Distribution — order status breakdown for the selected day
        OrderStatsResponse.DistributionData distribution = buildDistribution(dayStart, dayEnd);

        // Chart data
        OrderStatsResponse.ChartData chart;
        if ("DAY".equalsIgnoreCase(granularity)) {
            LocalDateTime weekStart = date.minusDays(6).atStartOfDay();
            chart = buildDailyChart(weekStart, dayEnd);
        } else {
            chart = buildHourlyChart(dayStart, dayEnd);
        }

        return OrderStatsResponse.builder().kpis(kpis).chart(chart).distribution(distribution).build();
    }

    private OrderStatsResponse.DistributionData buildDistribution(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.countByStatusGrouped(from, to);
        long pending = 0, confirmed = 0, preparing = 0, inDelivery = 0, delivered = 0, cancelled = 0;
        for (Object[] row : rows) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            switch (status) {
                case "PENDING" -> pending = count;
                case "CONFIRMED" -> confirmed = count;
                case "PREPARING", "READY_FOR_PICKUP", "PICKED_UP" -> preparing += count;
                case "IN_DELIVERY" -> inDelivery = count;
                case "DELIVERED" -> delivered = count;
                case "CANCELLED" -> cancelled = count;
            }
        }
        return OrderStatsResponse.DistributionData.builder()
                .pending(pending).confirmed(confirmed).preparing(preparing)
                .inDelivery(inDelivery).delivered(delivered).cancelled(cancelled)
                .build();
    }

    private OrderStatsResponse.ChartData buildHourlyChart(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.countByHourAndStatus(from, to);
        Map<Integer, long[]> map = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) map.put(h, new long[3]); // [new, delivered, cancelled]

        for (Object[] row : rows) {
            int hour = ((Number) row[0]).intValue();
            String status = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] arr = map.get(hour);
            if (arr == null) continue;
            if ("DELIVERED".equals(status)) arr[1] += count;
            else if ("CANCELLED".equals(status)) arr[2] += count;
            else arr[0] += count;
        }

        List<String> labels = new ArrayList<>();
        List<Long> newOrders = new ArrayList<>();
        List<Long> delivered = new ArrayList<>();
        List<Long> cancelled = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : map.entrySet()) {
            labels.add(String.format("%02d:00", e.getKey()));
            newOrders.add(e.getValue()[0]);
            delivered.add(e.getValue()[1]);
            cancelled.add(e.getValue()[2]);
        }
        return OrderStatsResponse.ChartData.builder()
                .labels(labels).newOrders(newOrders)
                .deliveredOrders(delivered).cancelledOrders(cancelled).build();
    }

    private OrderStatsResponse.ChartData buildDailyChart(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.countByDayAndStatus(from, to);
        Map<LocalDate, long[]> map = new LinkedHashMap<>();
        LocalDate d = from.toLocalDate();
        LocalDate end = to.toLocalDate();
        while (!d.isAfter(end)) {
            map.put(d, new long[3]);
            d = d.plusDays(1);
        }

        for (Object[] row : rows) {
            LocalDate day;
            if (row[0] instanceof java.sql.Date) {
                day = ((java.sql.Date) row[0]).toLocalDate();
            } else {
                day = LocalDate.parse(row[0].toString());
            }
            String status = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] arr = map.get(day);
            if (arr == null) continue;
            if ("DELIVERED".equals(status)) arr[1] += count;
            else if ("CANCELLED".equals(status)) arr[2] += count;
            else arr[0] += count;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<String> labels = new ArrayList<>();
        List<Long> newOrders = new ArrayList<>();
        List<Long> delivered = new ArrayList<>();
        List<Long> cancelled = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> e : map.entrySet()) {
            labels.add(e.getKey().format(fmt));
            newOrders.add(e.getValue()[0]);
            delivered.add(e.getValue()[1]);
            cancelled.add(e.getValue()[2]);
        }
        return OrderStatsResponse.ChartData.builder()
                .labels(labels).newOrders(newOrders)
                .deliveredOrders(delivered).cancelledOrders(cancelled).build();
    }

    private OrderResponse createOrderInternal(
            Long customerId,
            String customerName,
            List<CartItemPayload> cartItems,
            String promoCode,
            String addressId,
            CheckoutOrderRequest.DeliveryAddressRequest deliveryAddressDetails,
            String paymentMethodRaw,
            boolean clearCartAfterSuccess,
            Boolean requestedScheduled,
            LocalDateTime requestedScheduledDeliveryTime
    ) {
        final Order.PaymentMethod paymentMethod = parsePaymentMethod(paymentMethodRaw);
        if (paymentMethod != Order.PaymentMethod.CASH) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Seul le paiement CASH est supporté actuellement"
            );
        }

        final Long partnerId = extractSinglePartnerId(cartItems);
        final PartnerSnapshot partner = fetchPartner(partnerId);

    final boolean acceptsOrders = Boolean.TRUE.equals(partner.getAcceptsOrders());
    if (!acceptsOrders) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Le partenaire n'accepte pas de nouvelles commandes: " + partnerId
        );
    }

    final boolean partnerOpen = partner.getIsCurrentlyOpen() == null || Boolean.TRUE.equals(partner.getIsCurrentlyOpen());
    final LocalDateTime normalizedScheduledDeliveryTime = resolveScheduledDeliveryTime(
        requestedScheduled,
        requestedScheduledDeliveryTime,
        partnerOpen
    );

        final List<OrderItem> orderItems = buildOrderItems(partner, partnerId, cartItems);
        if (orderItems.isEmpty()) {
            throw badRequest("Aucun article valide à commander");
        }

        final BigDecimal subtotal = scaleMoney(orderItems.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        validateMinimumOrder(partner, subtotal);

        final String normalizedPromoCode = trimToNull(promoCode);

        final ResolvedCustomerInfo customerInfo = resolveCustomerInfo(customerId);
        final ResolvedDeliveryAddress resolvedDeliveryAddress = resolveDeliveryAddress(addressId, deliveryAddressDetails);

        final ZoneSnapshot zoneSnapshot = resolveZone(
                resolvedDeliveryAddress.deliveryLatitude(),
                resolvedDeliveryAddress.deliveryLongitude());
        final BigDecimal deliveryFee = zoneSnapshot != null && zoneSnapshot.getDeliveryFee() != null
                ? scaleMoney(zoneSnapshot.getDeliveryFee())
                : scaleMoney(partner.getDeliveryFee());
        final BigDecimal serviceFee = zoneSnapshot != null && zoneSnapshot.getServiceFee() != null
                ? scaleMoney(zoneSnapshot.getServiceFee())
                : ZERO;
        final BigDecimal tax = ZERO;
        final BigDecimal discount = normalizedPromoCode == null
            ? ZERO
            : resolvePromoDiscount(
                normalizedPromoCode,
                customerId,
                partnerId,
                subtotal,
                deliveryFee,
                serviceFee,
                orderItems
            );
        final BigDecimal tip = ZERO;

        final BigDecimal total = scaleMoney(
                subtotal
                        .add(deliveryFee)
                        .add(serviceFee)
                        .add(tax)
                        .add(tip)
                        .subtract(discount)
        );

        final int maxSuggestedPrepMinutes = orderItems.stream()
                .map(OrderItem::getPreparationTimeMin)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(20);

        final Order order = Order.builder()
                .customerId(customerId)
                .partnerId(partnerId)
                .status(OrderStatus.PENDING)
                .type(Order.OrderType.DELIVERY)
            .customerName(customerInfo.name())
            .customerEmail(customerInfo.email())
            .customerPhone(customerInfo.phone())
                .partnerName(resolvePartnerName(partner))
                .partnerAddress(partner.getAddress())
                .partnerPhone(partner.getPhoneNumber())
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .serviceFee(serviceFee)
                .tax(tax)
                .discount(discount)
                .promoCode(normalizedPromoCode)
                .tip(tip)
                .total(total)
                .deliveryAddressJson(resolvedDeliveryAddress.deliveryAddressJson())
                .deliveryLatitude(resolvedDeliveryAddress.deliveryLatitude())
                .deliveryLongitude(resolvedDeliveryAddress.deliveryLongitude())
                .deliveryInstructions(resolvedDeliveryAddress.deliveryInstructions())
                .paymentMethod(paymentMethod)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .isScheduled(normalizedScheduledDeliveryTime != null)
                .scheduledDeliveryTime(normalizedScheduledDeliveryTime)
                .estimatedDeliveryTime(
                    normalizedScheduledDeliveryTime != null
                        ? normalizedScheduledDeliveryTime
                        : estimateDeliveryTime(partner)
                )
                .suggestedPreparationMinutes(maxSuggestedPrepMinutes)
                .build();

        final Order savedOrder = orderRepository.save(order);
        syncLegacyDeliveryFields(savedOrder, resolvedDeliveryAddress.deliveryAddressJson());

        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        orderItemRepository.saveAll(orderItems);

        orderStatusHistoryRepository.save(OrderStatusHistory.systemChange(
                savedOrder.getId(),
                null,
                OrderStatus.PENDING,
            normalizedScheduledDeliveryTime == null
                ? "Commande créée"
                : "Commande planifiée créée"
        ));

        publishOrderCreated(savedOrder, orderItems);

        // Apply promotion usage (record in promotion-service)
        if (normalizedPromoCode != null) {
            applyPromotionUsage(normalizedPromoCode, customerId, savedOrder.getId(),
                    subtotal, deliveryFee, partnerId, orderItems);
        }

        if (clearCartAfterSuccess) {
            cartService.clearCart(customerId);
        }

        return getOrderById(savedOrder.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public String buildKitchenTicketHtml(Long orderId, Long partnerId) {
        if (partnerId == null) {
            throw badRequest("L'identifiant partenaire est obligatoire");
        }
        final Order order = getOrderOrThrow(orderId);
        assertPartnerOwnsOrder(order, partnerId);
        final List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return KitchenTicketHtmlBuilder.buildDocument(order, items, objectMapper);
    }

    private Order getOrderOrThrow(Long orderId) {
        if (orderId == null) {
            throw badRequest("L'identifiant de commande est obligatoire");
        }

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Commande introuvable: " + orderId
                ));
    }

    private void assertPartnerOwnsOrder(Order order, Long partnerId) {
        if (!Objects.equals(order.getPartnerId(), partnerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette commande n'appartient pas à ce partenaire");
        }
    }

    private void assertCourierAssigned(Order order, Long courierId) {
        if (order.getCourierId() == null || !Objects.equals(order.getCourierId(), courierId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce livreur n'est pas assigné à cette commande");
        }
    }

    private String normalizeActorType(String actorType) {
        final String normalized = actorType == null ? "SYSTEM" : actorType.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SYSTEM", "CUSTOMER", "PARTNER", "COURIER", "ADMIN").contains(normalized)) {
            throw badRequest("Type d'acteur invalide: " + actorType);
        }
        return normalized;
    }

    private void applyStatusTransition(
            Order order,
            OrderStatus newStatus,
            String actorType,
            Long actorId,
            String notes,
            String description
    ) {
        final OrderStatus previousStatus = order.getStatus();
        validateStatusTransition(previousStatus, newStatus);

        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED && order.getActualDeliveryTime() == null) {
            order.setActualDeliveryTime(LocalDateTime.now());
        }
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .orderId(order.getId())
                .previousStatus(previousStatus)
                .status(newStatus)
                .description(description)
                .notes(trimToNull(notes))
                .updatedBy(formatUpdatedBy(actorType, actorId))
                .actorType(actorType)
                .actorId(actorId)
                .build());

        // Publish status change event for real-time notifications
        try {
            orderEventProducer.publishOrderStatusChanged(
                    OrderStatusChangedEvent.builder()
                            .orderId(order.getId())
                            .orderNumber(order.getOrderNumber())
                            .customerId(order.getCustomerId())
                            .partnerId(order.getPartnerId())
                            .previousStatus(previousStatus)
                            .newStatus(newStatus)
                            .actorType(actorType)
                            .description(description)
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception ex) {
            log.warn("Failed to publish ORDER_STATUS_CHANGED for orderId={}", order.getId(), ex);
        }
    }

    private void validateStatusTransition(OrderStatus fromStatus, OrderStatus toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw badRequest("La transition de statut est invalide");
        }

        if (fromStatus == toStatus) {
            return;
        }

        final Set<OrderStatus> allowed = ALLOWED_STATUS_TRANSITIONS.getOrDefault(fromStatus, Set.of());
        if (!allowed.contains(toStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transition de statut invalide: " + fromStatus + " -> " + toStatus
            );
        }
    }

    private String formatUpdatedBy(String actorType, Long actorId) {
        if (actorType == null || actorType.isBlank()) {
            return "SYSTEM";
        }

        if (actorId == null) {
            return actorType;
        }

        return actorType + ":" + actorId;
    }

    private LocalDateTime resolveRequestedSchedule(CheckoutOrderRequest request) {
        if (request.getScheduledDeliveryTime() != null) {
            return request.getScheduledDeliveryTime();
        }

        final boolean hasDate = request.getScheduledDate() != null;
        final boolean hasTime = request.getScheduledTime() != null;
        if (!hasDate && !hasTime) {
            return null;
        }

        if (!hasDate || !hasTime) {
            throw badRequest("Pour planifier, la date et l'heure doivent être renseignées");
        }

        return LocalDateTime.of(request.getScheduledDate(), request.getScheduledTime());
    }

    private LocalDateTime resolveScheduledDeliveryTime(
            Boolean requestedScheduled,
            LocalDateTime requestedScheduledDeliveryTime,
            boolean partnerOpen
    ) {
        final boolean scheduleRequested = Boolean.TRUE.equals(requestedScheduled) || requestedScheduledDeliveryTime != null;

        if (!scheduleRequested) {
            if (!partnerOpen) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Le partenaire est actuellement fermé. Choisissez une date et une heure de planification."
                );
            }
            return null;
        }

        if (requestedScheduledDeliveryTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La commande planifiée nécessite une date et une heure de livraison"
            );
        }

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime normalized = requestedScheduledDeliveryTime.withSecond(0).withNano(0);

        if (normalized.isBefore(now.plusMinutes(15))) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "L'heure planifiée doit être au moins 15 minutes dans le futur"
            );
        }

        if (normalized.isAfter(now.plusDays(7))) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "L'heure planifiée ne peut pas dépasser 7 jours"
            );
        }

        return normalized;
    }

    private List<CartItemPayload> resolveCartItemsForCheckout(Long customerId, CheckoutOrderRequest request) {
        final List<CartItemPayload> requestItems = sanitizeCartItems(toCartPayloads(request.getCartItems()));
        final CartResponse cartResponse = cartService.getCart(customerId);
        final List<CartItemPayload> serverItems = sanitizeCartItems(
                cartResponse == null ? List.of() : cartResponse.getItems()
        );

        if (serverItems.isEmpty()) {
            return requestItems;
        }

        if (requestItems.isEmpty()) {
            return serverItems;
        }

        if (!cartSignature(serverItems).equals(cartSignature(requestItems))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le panier serveur ne correspond pas à la demande. Rafraîchissez votre panier puis réessayez."
            );
        }

        return serverItems;
    }

    private List<CartItemPayload> toCartPayloads(List<CheckoutOrderRequest.CartItemRequest> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return List.of();
        }

        return cartItems.stream()
                .filter(Objects::nonNull)
                .map(item -> CartItemPayload.builder()
                        .productId(trimToNull(item.getProductId()))
                        .partnerId(trimToNull(item.getPartnerId()))
                        .productName(trimToNull(item.getProductName()))
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .selectedOptions(item.getSelectedOptions() == null ? List.of() : item.getSelectedOptions())
                        .kitchenNote(trimToNull(item.getKitchenNote()))
                        .build())
                .toList();
    }

    private List<CartItemPayload> toCartPayloads(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return List.of();
        }

        final String partnerId = String.valueOf(request.getPartnerId());

        return request.getItems().stream()
                .filter(Objects::nonNull)
                .map(item -> CartItemPayload.builder()
                        .productId(item.getProductId() == null ? null : String.valueOf(item.getProductId()))
                        .partnerId(partnerId)
                        .quantity(item.getQuantity())
                        .selectedOptions(List.of())
                        .kitchenNote(trimToNull(item.getSpecialInstructions()))
                        .build())
                .toList();
    }

    private List<CartItemPayload> sanitizeCartItems(List<CartItemPayload> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        final List<CartItemPayload> sanitized = new ArrayList<>();
        for (CartItemPayload item : source) {
            if (item == null) {
                continue;
            }

            final Integer quantity = item.getQuantity();
            if (quantity == null || quantity <= 0) {
                continue;
            }

            final String productId = trimToNull(item.getProductId());
            final String partnerId = trimToNull(item.getPartnerId());
            if (productId == null || partnerId == null) {
                continue;
            }

            sanitized.add(CartItemPayload.builder()
                    .productId(productId)
                    .partnerId(partnerId)
                    .productName(trimToNull(item.getProductName()))
                    .quantity(quantity)
                    .selectedOptions(item.getSelectedOptions() == null ? List.of() : item.getSelectedOptions())
                    .kitchenNote(trimToNull(item.getKitchenNote()))
                    .build());
        }

        return List.copyOf(sanitized);
    }

    private List<String> cartSignature(List<CartItemPayload> items) {
        return items.stream()
                .map(item -> {
                final List<String> normalizedOptions = normalizeOptionsForSignature(item.getSelectedOptions());

                    final String note = item.getKitchenNote() == null
                            ? ""
                            : item.getKitchenNote().trim().toLowerCase(Locale.ROOT);

                    return item.getProductId() + "|"
                            + item.getPartnerId() + "|"
                            + item.getQuantity() + "|"
                            + String.join(",", normalizedOptions) + "|"
                            + note;
                })
                .sorted()
                .toList();
    }

    /**
     * Clé de ligne panier sans la note — pour rapprocher panier Redis et payload checkout
     * quand la note n’a pas encore été synchronisée côté serveur.
     */
    private String cartLineKeyWithoutNote(CartItemPayload item) {
        final List<String> normalizedOptions = normalizeOptionsForSignature(item.getSelectedOptions());
        final Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
        return item.getProductId() + "|"
                + item.getPartnerId() + "|"
                + qty + "|"
                + String.join(",", normalizedOptions);
    }

    /**
     * Si le panier résolu (souvent Redis) n’a pas de {@code kitchenNote} mais que le client
     * l’a envoyée dans le checkout, on la recopie — évite les commandes sans note par plat.
     */
    private void mergeKitchenNotesFromCheckoutPayload(
            List<CartItemPayload> resolved,
            List<CheckoutOrderRequest.CartItemRequest> rawCartItems) {
        if (resolved == null || resolved.isEmpty() || rawCartItems == null || rawCartItems.isEmpty()) {
            return;
        }
        final List<CartItemPayload> fromRequest = toCartPayloads(rawCartItems);
        if (fromRequest.isEmpty()) {
            return;
        }
        final Map<String, ArrayDeque<CartItemPayload>> queues = new HashMap<>();
        for (CartItemPayload r : fromRequest) {
            queues.computeIfAbsent(cartLineKeyWithoutNote(r), k -> new ArrayDeque<>()).addLast(r);
        }
        for (CartItemPayload s : resolved) {
            if (trimToNull(s.getKitchenNote()) != null) {
                continue;
            }
            final ArrayDeque<CartItemPayload> q = queues.get(cartLineKeyWithoutNote(s));
            if (q == null || q.isEmpty()) {
                continue;
            }
            final String note = trimToNull(q.pollFirst().getKitchenNote());
            if (note != null) {
                s.setKitchenNote(note);
            }
        }
    }

            private List<String> normalizeOptionsForSignature(List<Object> rawSelectedOptions) {
            return normalizeSelectedOptions(rawSelectedOptions).stream()
                .map(option -> {
                    final String optionName = trimToNull(option.getOptionName()) == null
                        ? ""
                        : option.getOptionName().trim().toLowerCase(Locale.ROOT);
                    final String valueName = trimToNull(option.getValueName()) == null
                        ? ""
                        : option.getValueName().trim().toLowerCase(Locale.ROOT);
                    final String priceModifier = scaleMoney(option.getPriceModifier()).toPlainString();
                    return optionName + ":" + valueName + ":" + priceModifier;
                })
                .filter(token -> !token.equals("::0.00"))
                .sorted()
                .toList();
            }

    private Long extractSinglePartnerId(List<CartItemPayload> cartItems) {
        Long partnerId = null;

        for (CartItemPayload item : cartItems) {
            final Long currentPartnerId;
            try {
                currentPartnerId = Long.parseLong(item.getPartnerId());
            } catch (NumberFormatException ex) {
                throw badRequest("Identifiant partenaire invalide dans le panier");
            }

            if (partnerId == null) {
                partnerId = currentPartnerId;
                continue;
            }

            if (!partnerId.equals(currentPartnerId)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Une commande ne peut contenir qu'un seul partenaire"
                );
            }
        }

        if (partnerId == null) {
            throw badRequest("Le panier ne contient aucun partenaire valide");
        }

        return partnerId;
    }

    private PartnerSnapshot fetchPartner(Long partnerId) {
        try {
            return partnerServiceClient.getPartnerById(partnerId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire introuvable: " + partnerId);
        } catch (Exception ex) {
            log.error("Erreur lors de la récupération du partenaire {}", partnerId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service partenaire indisponible");
        }
    }

    private ProductSnapshot fetchProduct(Long partnerId, Long productId) {
        try {
            return partnerServiceClient.getProductById(partnerId, productId);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Produit introuvable ou non rattaché au partenaire: " + productId
            );
        } catch (Exception ex) {
            log.error("Erreur lors de la récupération du produit {} pour partenaire {}", productId, partnerId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service partenaire indisponible");
        }
    }

    private void validatePartnerAvailability(PartnerSnapshot partner, Long partnerId) {
        final boolean acceptsOrders = Boolean.TRUE.equals(partner.getAcceptsOrders());
        final boolean currentlyOpen = partner.getIsCurrentlyOpen() == null || Boolean.TRUE.equals(partner.getIsCurrentlyOpen());

        if (!acceptsOrders || !currentlyOpen) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le partenaire n'accepte pas de nouvelles commandes: " + partnerId
            );
        }
    }

    private void validateMinimumOrder(PartnerSnapshot partner, BigDecimal subtotal) {
        final BigDecimal minimumOrder = scaleMoney(partner.getMinimumOrder());
        if (minimumOrder.compareTo(ZERO) > 0 && subtotal.compareTo(minimumOrder) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Montant minimum non atteint: " + minimumOrder
            );
        }
    }

    private ZoneSnapshot resolveZone(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        try {
            return locationServiceClient.findZoneForPoint(latitude, longitude);
        } catch (Exception ex) {
            log.warn("Impossible de récupérer la zone pour lat={}, lon={}: {}", latitude, longitude, ex.getMessage());
            return null;
        }
    }

    private BigDecimal resolvePromoDiscount(
            String promoCode,
            Long customerId,
            Long partnerId,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal serviceFee,
            List<OrderItem> orderItems
    ) {
        try {
            final List<Long> productIds = orderItems.stream()
                    .map(OrderItem::getProductId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            final PromotionValidateRequest request = PromotionValidateRequest.builder()
                    .code(promoCode)
                    .userId(customerId)
                    .orderSubtotal(subtotal)
                    .deliveryFee(deliveryFee)
                    .partnerId(partnerId)
                    .productIds(productIds)
                    .build();

            final PromotionApiResponse<PromotionValidateResponse> response =
                    promotionServiceClient.validatePromotion(request);

            if (response == null || response.getData() == null || !Boolean.TRUE.equals(response.getData().getIsValid())) {
                final String message = response != null && response.getData() != null
                        ? response.getData().getMessage()
                        : (response == null ? null : response.getMessage());

                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        trimToNull(message) == null ? "Code promo invalide" : message
                );
            }

            BigDecimal discount = scaleMoney(response.getData().getDiscountAmount());
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                discount = ZERO;
            }

            final BigDecimal maxDiscount = subtotal.add(deliveryFee);
            if (discount.compareTo(maxDiscount) > 0) {
                discount = maxDiscount;
            }

            return scaleMoney(discount);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (FeignException ex) {
            if (ex.status() == 400 || ex.status() == 404 || ex.status() == 422) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Code promo invalide");
            }

            log.error("Erreur promotion-service lors de la validation du code {}", promoCode, ex);
            log.warn("Validation promo indisponible, poursuite du checkout sans réduction. code={}", promoCode);
            return ZERO;
        } catch (Exception ex) {
            log.error("Erreur lors de la validation du code promo {}", promoCode, ex);
            log.warn("Validation promo indisponible, poursuite du checkout sans réduction. code={}", promoCode);
            return ZERO;
        }
    }

    /**
     * Call POST /promotions/{code}/apply to record usage atomically.
     * If the call fails (circuit breaker open, timeout, etc.), log the error
     * but do NOT fail the order — the discount was already applied.
     */
    private void applyPromotionUsage(
            String promoCode,
            Long customerId,
            Long orderId,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            Long partnerId,
            List<OrderItem> orderItems
    ) {
        try {
            final PromotionApplyRequest request = PromotionApplyRequest.builder()
                    .userId(customerId)
                    .orderId(orderId)
                    .orderSubtotal(subtotal)
                    .deliveryFee(deliveryFee)
                    .partnerId(partnerId)
                    .itemCount(orderItems.size())
                    .build();

            PromotionApiResponse<PromotionValidateResponse> response =
                    promotionServiceClient.applyPromotion(promoCode, request);

            // Persist promotionId on the order
            if (response != null && response.getData() != null
                    && response.getData().getPromotionId() != null) {
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.setPromotionId(response.getData().getPromotionId());
                    orderRepository.save(order);
                });
            }
            log.info("Promotion {} applied for order {}", promoCode, orderId);
        } catch (Exception ex) {
            log.error("Failed to apply promotion {} for order {} — usage not recorded",
                    promoCode, orderId, ex);
        }
    }

    private void publishOrderCreated(Order order, List<OrderItem> orderItems) {
        try {
            final int itemCount = orderItems.stream()
                    .map(OrderItem::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);

            Long partnerUserId = null;
            try {
                final PartnerSnapshot partner = partnerServiceClient.getPartnerById(order.getPartnerId());
                if (partner != null && partner.getUserId() != null) {
                    partnerUserId = partner.getUserId();
                }
            } catch (Exception ex) {
                log.warn("Impossible de résoudre partnerUserId pour partnerId={} : {}",
                        order.getPartnerId(), ex.getMessage());
            }

            final OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .customerId(order.getCustomerId())
                    .partnerId(order.getPartnerId())
                    .partnerUserId(partnerUserId)
                    .subtotal(order.getSubtotal())
                    .deliveryFee(order.getDeliveryFee())
                    .serviceFee(order.getServiceFee())
                    .discount(order.getDiscount())
                    .total(order.getTotal())
                    .paymentMethod(order.getPaymentMethod())
                    .paymentStatus(order.getPaymentStatus())
                    .status(order.getStatus())
                    .itemCount(itemCount)
                    .createdAt(order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt())
                    .build();

            orderEventProducer.publishOrderCreated(event);
        } catch (Exception ex) {
            // Event publication should never fail order creation.
            log.warn("Impossible de publier ORDER_CREATED pour orderId={}", order.getId(), ex);
        }
    }

    private void maybePublishOrderAcceptedEvent(Order order, OrderStatus previousStatus, String actorType, Long actorId) {
        if (order == null || previousStatus == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.PREPARING) {
            return;
        }
        if (previousStatus == OrderStatus.PREPARING) {
            return;
        }

        final String normalizedActorType = actorType == null || actorType.isBlank()
                ? "PARTNER"
                : actorType.trim().toUpperCase(Locale.ROOT);

        try {
            final OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
                    .eventType("ORDER_ACCEPTED")
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .customerId(order.getCustomerId())
                    .partnerId(order.getPartnerId())
                    .previousStatus(previousStatus)
                    .status(order.getStatus())
                    .actorType(normalizedActorType)
                    .actorId(actorId)
                    .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                    .updatedAt(LocalDateTime.now())
                    .build();

            orderEventProducer.publishOrderStatusChanged(event);
        } catch (Exception ex) {
            log.warn("Impossible de publier ORDER_ACCEPTED pour orderId={}", order.getId(), ex);
        }
    }

    private LocalDateTime estimateDeliveryTime(PartnerSnapshot partner) {
        final int partnerPreparation = partner.getPreparationTime() == null ? 20 : Math.max(0, partner.getPreparationTime());
        final int totalMinutes = Math.max(30, partnerPreparation + 15);
        return LocalDateTime.now().plusMinutes(totalMinutes);
    }

    private List<OrderItem> buildOrderItems(PartnerSnapshot partner, Long partnerId, List<CartItemPayload> cartItems) {
        final List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemPayload cartItem : cartItems) {
            final Long productId;
            try {
                productId = Long.parseLong(cartItem.getProductId());
            } catch (NumberFormatException ex) {
                throw badRequest("Identifiant produit invalide: " + cartItem.getProductId());
            }

            final ProductSnapshot product = fetchProduct(partnerId, productId);

            if (!Boolean.TRUE.equals(product.getIsAvailable())) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Produit indisponible: " + productId
                );
            }

            final BigDecimal unitPrice = product.getPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Prix produit invalide depuis partner-service pour le produit: " + productId
                );
            }

            final BigDecimal normalizedUnitPrice = scaleMoney(unitPrice);
            final Integer quantity = cartItem.getQuantity();
            final int linePrepMin = resolveLinePreparationMinutes(product, partner);
                final List<OrderItemDTO.SelectedOptionDTO> normalizedSelectedOptions =
                    normalizeSelectedOptions(cartItem.getSelectedOptions());
                final BigDecimal modifiersTotal = scaleMoney(normalizedSelectedOptions.stream()
                    .map(OrderItemDTO.SelectedOptionDTO::getPriceModifier)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
                final BigDecimal totalUnitPrice = scaleMoney(normalizedUnitPrice.add(modifiersTotal));

            final OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .productName(resolveProductName(product, cartItem, productId))
                    .productDescription(product.getDescription())
                    .productImage(product.getImageUrl())
                    .quantity(quantity)
                    .unitPrice(normalizedUnitPrice)
                    .modifiersTotal(modifiersTotal)
                    .subtotal(scaleMoney(totalUnitPrice.multiply(BigDecimal.valueOf(quantity))))
                    .selectedOptionsJson(writeJson(normalizedSelectedOptions))
                    .selectedAddonsJson("[]")
                    .specialInstructions(trimToNull(cartItem.getKitchenNote()))
                    .preparationTimeMin(linePrepMin)
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private List<OrderItemDTO.SelectedOptionDTO> normalizeSelectedOptions(List<Object> rawSelectedOptions) {
        if (rawSelectedOptions == null || rawSelectedOptions.isEmpty()) {
            return List.of();
        }

        final List<OrderItemDTO.SelectedOptionDTO> normalized = new ArrayList<>();

        for (Object rawOption : rawSelectedOptions) {
            if (rawOption instanceof String optionLabel) {
                final String trimmedLabel = optionLabel.trim();
                if (trimmedLabel.isEmpty()) {
                    continue;
                }

                final String optionName = normalizeOptionNameFromLabel(trimmedLabel);
                final String valueName = firstNonBlank(
                        trimmedLabel.contains(":")
                                ? trimmedLabel.substring(trimmedLabel.indexOf(':') + 1).trim()
                                : null,
                        trimmedLabel
                );

                normalized.add(OrderItemDTO.SelectedOptionDTO.builder()
                        .optionName(optionName)
                        .valueName(valueName)
                        .priceModifier(ZERO)
                        .build());
                continue;
            }

            if (!(rawOption instanceof Map<?, ?> map)) {
                continue;
            }

            final String optionName = firstNonBlank(
                    asString(map.get("optionName")),
                    asString(map.get("groupName")),
                    asString(map.get("groupLabel"))
            );
            final String valueName = firstNonBlank(
                    asString(map.get("valueName")),
                    asString(map.get("optionValueName")),
                    asString(map.get("label")),
                    asString(map.get("name")),
                    optionName
            );
            final BigDecimal priceModifier = scaleMoney(asBigDecimal(
                    map.containsKey("priceModifier") ? map.get("priceModifier") : map.get("price")
            ));

            if (trimToNull(optionName) == null && trimToNull(valueName) == null) {
                continue;
            }

            normalized.add(OrderItemDTO.SelectedOptionDTO.builder()
                    .optionId(asLong(map.get("optionId")))
                    .optionName(optionName)
                    .valueId(asLong(map.get("valueId")))
                    .valueName(valueName)
                    .priceModifier(priceModifier)
                    .build());
        }

        return List.copyOf(normalized);
    }

    private String normalizeOptionNameFromLabel(String label) {
        final int separatorIndex = label.indexOf(':');
        if (separatorIndex < 0) {
            return null;
        }
        final String group = label.substring(0, separatorIndex).trim();
        return group.isEmpty() ? null : group;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            final String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    /**
     * Minutes de préparation pour une ligne : fiche produit, sinon défaut partenaire (aligné sur estimateDeliveryTime).
     */
    private int resolveLinePreparationMinutes(ProductSnapshot product, PartnerSnapshot partner) {
        if (product.getPreparationTimeMin() != null) {
            return Math.max(0, product.getPreparationTimeMin());
        }
        if (partner.getPreparationTime() != null) {
            return Math.max(0, partner.getPreparationTime());
        }
        return 20;
    }

    private String resolveProductName(ProductSnapshot product, CartItemPayload cartItem, Long productId) {
        if (trimToNull(product.getName()) != null) {
            return product.getName().trim();
        }

        if (trimToNull(cartItem.getProductName()) != null) {
            return cartItem.getProductName().trim();
        }

        return "Produit #" + productId;
    }

    private String resolvePartnerName(PartnerSnapshot partner) {
        if (trimToNull(partner.getBusinessName()) != null) {
            return partner.getBusinessName().trim();
        }

        if (trimToNull(partner.getName()) != null) {
            return partner.getName().trim();
        }

        return "Partenaire #" + partner.getId();
    }

    private ResolvedCustomerInfo resolveCustomerInfo(Long customerId) {
        try {
            final CustomerSnapshot snapshot = userServiceClient.getCustomerByUserId(customerId);
            if (snapshot != null) {
                final String resolvedName = firstNonBlank(
                        joinNonBlank(snapshot.getFirstName(), snapshot.getLastName()),
                        snapshot.getFirstName(),
                        snapshot.getLastName()
                );

                return new ResolvedCustomerInfo(
                        firstNonBlank(resolvedName, "Client #" + customerId),
                        trimToNull(snapshot.getEmail()),
                        trimToNull(snapshot.getPhoneNumber())
                );
            }
        } catch (Exception ex) {
            log.warn("Impossible de récupérer les informations client userId={} : {}", customerId, ex.getMessage());
        }

        return new ResolvedCustomerInfo("Client #" + customerId, null, null);
    }

    private ResolvedDeliveryAddress resolveDeliveryAddress(
            String legacyAddressId,
            CheckoutOrderRequest.DeliveryAddressRequest deliveryAddressDetails
    ) {
        final String requestAddressId = deliveryAddressDetails == null
                ? null
                : trimToNull(deliveryAddressDetails.getAddressId());
        final String normalizedAddressId = firstNonBlank(requestAddressId, trimToNull(legacyAddressId));

        final Map<String, Object> payload = new LinkedHashMap<>();
        if (normalizedAddressId != null) {
            payload.put("addressId", normalizedAddressId);
        }

        if (deliveryAddressDetails != null) {
            putIfNotBlank(payload, "label", deliveryAddressDetails.getLabel());
            putIfNotBlank(payload, "deliveryAddress", deliveryAddressDetails.getDeliveryAddress());
            putIfNotBlank(payload, "deliveryLocation", deliveryAddressDetails.getDeliveryLocation());
            putIfNotBlank(payload, "street", deliveryAddressDetails.getStreet());
            putIfNotBlank(payload, "building", deliveryAddressDetails.getBuilding());
            putIfNotBlank(payload, "floor", deliveryAddressDetails.getFloor());
            putIfNotBlank(payload, "apartment", deliveryAddressDetails.getApartment());
            putIfNotBlank(payload, "city", deliveryAddressDetails.getCity());
            putIfNotBlank(payload, "postalCode", deliveryAddressDetails.getPostalCode());
            putIfNotBlank(payload, "state", deliveryAddressDetails.getState());
            putIfNotBlank(payload, "country", deliveryAddressDetails.getCountry());
            putIfNotBlank(payload, "formattedAddress", deliveryAddressDetails.getFormattedAddress());
            putIfNotBlank(payload, "deliveryInstructions", deliveryAddressDetails.getDeliveryInstructions());
            putIfNotBlank(payload, "contactName", deliveryAddressDetails.getContactName());
            putIfNotBlank(payload, "contactPhone", deliveryAddressDetails.getContactPhone());
            if (deliveryAddressDetails.getIsFromMap() != null) {
                payload.put("isFromMap", deliveryAddressDetails.getIsFromMap());
            }

            final BigDecimal latitude = resolveCoordinate(
                    deliveryAddressDetails.getDeliveryLatitude(),
                    deliveryAddressDetails.getLatitude()
            );
            final BigDecimal longitude = resolveCoordinate(
                    deliveryAddressDetails.getDeliveryLongitude(),
                    deliveryAddressDetails.getLongitude()
            );

            // Si lat/lon manquent mais addressId présent → chercher dans user-service
            BigDecimal resolvedLat = latitude;
            BigDecimal resolvedLon = longitude;
            if ((resolvedLat == null || resolvedLon == null) && normalizedAddressId != null) {
                try {
                    Long addrId = Long.parseLong(normalizedAddressId);
                    AddressSnapshot addr = userServiceClient.getAddressById(addrId);
                    if (addr != null) {
                        if (resolvedLat == null) resolvedLat = addr.getLatitude();
                        if (resolvedLon == null) resolvedLon = addr.getLongitude();
                        if (!payload.containsKey("formattedAddress") && addr.getFormattedAddress() != null) {
                            payload.put("formattedAddress", addr.getFormattedAddress());
                        }
                        if (!payload.containsKey("street") && addr.getStreet() != null) {
                            payload.put("street", addr.getStreet());
                        }
                        if (!payload.containsKey("city") && addr.getCity() != null) {
                            payload.put("city", addr.getCity());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Impossible de récupérer l'adresse ID={}: {}", normalizedAddressId, ex.getMessage());
                }
            }

            if (resolvedLat != null) {
                payload.put("deliveryLatitude", resolvedLat);
                payload.put("latitude", resolvedLat);
            }
            if (resolvedLon != null) {
                payload.put("deliveryLongitude", resolvedLon);
                payload.put("longitude", resolvedLon);
            }

            return new ResolvedDeliveryAddress(
                    payload.isEmpty() ? null : writeJson(payload),
                    resolvedLat,
                    resolvedLon,
                    trimToNull(deliveryAddressDetails.getDeliveryInstructions())
            );
        }

        // Pas de deliveryAddressDetails mais addressId présent → chercher dans user-service
        if (normalizedAddressId != null) {
            try {
                Long addrId = Long.parseLong(normalizedAddressId);
                AddressSnapshot addr = userServiceClient.getAddressById(addrId);
                if (addr != null) {
                    if (addr.getLatitude() != null) {
                        payload.put("deliveryLatitude", addr.getLatitude());
                        payload.put("latitude", addr.getLatitude());
                    }
                    if (addr.getLongitude() != null) {
                        payload.put("deliveryLongitude", addr.getLongitude());
                        payload.put("longitude", addr.getLongitude());
                    }
                    if (addr.getFormattedAddress() != null) {
                        payload.put("formattedAddress", addr.getFormattedAddress());
                    }
                    if (addr.getStreet() != null) {
                        payload.put("street", addr.getStreet());
                    }
                    if (addr.getCity() != null) {
                        payload.put("city", addr.getCity());
                    }
                    return new ResolvedDeliveryAddress(
                            writeJson(payload),
                            addr.getLatitude(),
                            addr.getLongitude(),
                            addr.getDeliveryInstructions()
                    );
                }
            } catch (Exception ex) {
                log.warn("Impossible de récupérer l'adresse ID={}: {}", normalizedAddressId, ex.getMessage());
            }
        }

        if (payload.isEmpty()) {
            return new ResolvedDeliveryAddress(null, null, null, null);
        }

        return new ResolvedDeliveryAddress(writeJson(payload), null, null, null);
    }

    private void syncLegacyDeliveryFields(Order savedOrder, String deliveryAddressJson) {
        if (savedOrder == null || savedOrder.getId() == null) {
            return;
        }

        try {
            orderRepository.syncLegacyDeliveryFields(
                    savedOrder.getId(),
                    deliveryAddressJson,
                    savedOrder.getDeliveryLatitude(),
                    savedOrder.getDeliveryLongitude()
            );
        } catch (Exception ex) {
            log.debug("Impossible de synchroniser les colonnes legacy de livraison pour orderId={}", savedOrder.getId(), ex);
        }
    }

    private BigDecimal resolveCoordinate(BigDecimal preferred, BigDecimal fallback) {
        if (preferred != null) {
            return preferred;
        }
        return fallback;
    }

    private void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        final String normalized = trimToNull(value);
        if (normalized != null) {
            payload.put(key, normalized);
        }
    }

    private String joinNonBlank(String first, String second) {
        final String left = trimToNull(first);
        final String right = trimToNull(second);
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left + " " + right;
    }

    private String buildDeliveryAddressJson(String addressId) {
        final String normalized = trimToNull(addressId);
        if (normalized == null) {
            return null;
        }

        return writeJson(Map.of("addressId", normalized));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Impossible de sérialiser le JSON", ex);
            return "{}";
        }
    }

    private List<OrderItemDTO> mapOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(this::toOrderItemDto)
                .toList();
    }

    private OrderItemDTO toOrderItemDto(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productDescription(item.getProductDescription())
                .productImage(item.getProductImage())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .modifiersTotal(item.getModifiersTotal())
                .totalUnitPrice(item.getTotalUnitPrice())
                .subtotal(item.getSubtotal())
                .selectedOptions(parseSelectedOptions(item.getSelectedOptionsJson()))
                .selectedAddons(parseSelectedAddons(item.getSelectedAddonsJson()))
                .specialInstructions(item.getSpecialInstructions())
                .preparationTimeMin(item.getPreparationTimeMin())
                .build();
    }

    private List<OrderItemDTO.SelectedOptionDTO> parseSelectedOptions(String selectedOptionsJson) {
        if (selectedOptionsJson == null || selectedOptionsJson.isBlank()) {
            return List.of();
        }

        try {
            final List<?> rawOptions = objectMapper.readValue(selectedOptionsJson, new TypeReference<List<?>>() {});
            final List<OrderItemDTO.SelectedOptionDTO> parsed = new ArrayList<>();

            for (Object rawOption : rawOptions) {
                if (rawOption instanceof String optionLabel) {
                    final String trimmed = optionLabel.trim();
                    if (!trimmed.isEmpty()) {
                        parsed.add(OrderItemDTO.SelectedOptionDTO.builder()
                                .optionName(trimmed)
                                .valueName(trimmed)
                                .priceModifier(ZERO)
                                .build());
                    }
                    continue;
                }

                if (rawOption instanceof Map<?, ?> map) {
                    parsed.add(OrderItemDTO.SelectedOptionDTO.builder()
                            .optionId(asLong(map.get("optionId")))
                        .optionName(firstNonBlank(
                            asString(map.get("optionName")),
                            asString(map.get("groupName")),
                            asString(map.get("groupLabel"))
                        ))
                            .valueId(asLong(map.get("valueId")))
                        .valueName(firstNonBlank(
                            asString(map.get("valueName")),
                            asString(map.get("optionValueName")),
                            asString(map.get("label")),
                            asString(map.get("name"))
                        ))
                        .priceModifier(scaleMoney(asBigDecimal(
                            map.containsKey("priceModifier") ? map.get("priceModifier") : map.get("price")
                        )))
                            .build());
                }
            }

            return List.copyOf(parsed);
        } catch (Exception ex) {
            log.debug("Impossible de parser selectedOptionsJson: {}", selectedOptionsJson, ex);
            return List.of();
        }
    }

    private List<OrderItemDTO.SelectedAddonDTO> parseSelectedAddons(String selectedAddonsJson) {
        if (selectedAddonsJson == null || selectedAddonsJson.isBlank()) {
            return List.of();
        }

        try {
            final List<?> rawAddons = objectMapper.readValue(selectedAddonsJson, new TypeReference<List<?>>() {});
            final List<OrderItemDTO.SelectedAddonDTO> parsed = new ArrayList<>();

            for (Object rawAddon : rawAddons) {
                if (!(rawAddon instanceof Map<?, ?> map)) {
                    continue;
                }

                final BigDecimal addonPrice = scaleMoney(asBigDecimal(map.get("price")));
                final Integer quantity = asInteger(map.get("quantity"), 1);
                final BigDecimal total = scaleMoney(addonPrice.multiply(BigDecimal.valueOf(quantity)));

                parsed.add(OrderItemDTO.SelectedAddonDTO.builder()
                        .addonId(asLong(map.get("addonId")))
                        .addonName(asString(map.get("addonName")))
                        .quantity(quantity)
                        .price(addonPrice)
                        .total(total)
                        .build());
            }

            return List.copyOf(parsed);
        } catch (Exception ex) {
            log.debug("Impossible de parser selectedAddonsJson: {}", selectedAddonsJson, ex);
            return List.of();
        }
    }

    private static final String PREP_NOTES_PREFIX = "PREP_MINUTES:";

    private List<OrderResponse.StatusHistoryDTO> mapStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(orderId).stream()
                .map(this::toStatusHistoryDto)
                .toList();
    }

    private OrderResponse.StatusHistoryDTO toStatusHistoryDto(OrderStatusHistory h) {
        final String rawNotes = h.getNotes();
        Integer prepMinutes = null;
        String notesForClient = rawNotes;
        if (rawNotes != null && rawNotes.startsWith(PREP_NOTES_PREFIX)) {
            try {
                prepMinutes = Integer.parseInt(rawNotes.substring(PREP_NOTES_PREFIX.length()).trim());
                notesForClient = null;
            } catch (NumberFormatException ignored) {
                notesForClient = rawNotes;
            }
        }
        return OrderResponse.StatusHistoryDTO.builder()
                .status(h.getStatus())
                .previousStatus(h.getPreviousStatus())
                .description(h.getDescription())
                .notes(notesForClient)
                .updatedBy(h.getUpdatedBy())
                .actorType(h.getActorType())
                .timestamp(h.getTimestamp())
                .estimatedPrepMinutes(prepMinutes)
                .build();
    }

    // ==================== FILTRAGE PARTENAIRE (server-side) ====================

    /** Sentinelle "pas de borne inférieure" : antérieure à toute commande réelle. */
    private static final LocalDateTime DATE_SENTINEL_FROM = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
    /** Sentinelle "pas de borne supérieure" : postérieure à toute commande réelle. */
    private static final LocalDateTime DATE_SENTINEL_TO   = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getPartnerOrdersFiltered(
            Long partnerId,
            OrderStatus status,
            String search,
            Pageable pageable,
            String sortByParam,
            LocalDateTime from,
            LocalDateTime to,
            String cancelledBy
    ) {
        final String trimmedSearch = search != null && !search.isBlank() ? search.trim() : "";
        final boolean prioritySort = "priority".equalsIgnoreCase(
                sortByParam == null ? "" : sortByParam.trim()
        );

        // PostgreSQL ne peut pas inférer le type d'un paramètre NULL seul dans IS NULL → on passe
        // toujours des bornes non-nulles ; les sentinelles couvrent "toutes les dates".
        final LocalDateTime effectiveFrom = from != null ? from : DATE_SENTINEL_FROM;
        final LocalDateTime effectiveTo   = to   != null ? to   : DATE_SENTINEL_TO;

        final String cancelledByFilter = resolveCancelledByForPartnerList(status, cancelledBy);

        final Page<Order> page;
        final int pageIdx = pageable.getPageNumber();
        final int pageSize = pageable.getPageSize();
        final Pageable unsortedPage = PageRequest.of(pageIdx, pageSize);

        if (prioritySort && status == null) {
            page = orderRepository.findByPartnerIdPriority(partnerId, effectiveFrom, effectiveTo, trimmedSearch, unsortedPage);
        } else if (status != null) {
            page = orderRepository.findByPartnerIdAndStatusFiltered(
                    partnerId, status, effectiveFrom, effectiveTo, trimmedSearch, cancelledByFilter, pageable);
        } else {
            page = orderRepository.findByPartnerIdAllStatuses(partnerId, effectiveFrom, effectiveTo, trimmedSearch, pageable);
        }

        return page.map(this::toResponse);
    }

    private static final int EXPORT_PAGE_SIZE = 500;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listAllPartnerOrdersForExport(
            Long partnerId,
            OrderStatus status,
            String search,
            LocalDateTime from,
            LocalDateTime to,
            String cancelledBy
    ) {
        final List<OrderResponse> out = new ArrayList<>();
        int page = 0;
        while (true) {
            final Pageable pageable = PageRequest.of(
                    page,
                    EXPORT_PAGE_SIZE,
                    Sort.by(Sort.Direction.DESC, "orderTime"));
            final Page<OrderResponse> chunk = getPartnerOrdersFiltered(
                    partnerId,
                    status,
                    search,
                    pageable,
                    "orderTime",
                    from,
                    to,
                    cancelledBy);
            out.addAll(chunk.getContent());
            if (!chunk.hasNext()) {
                break;
            }
            page++;
        }
        return out;
    }

    /**
     * Filtre {@code cancelledBy} uniquement pour les commandes annulées (ex. PARTNER = refus partenaire).
     */
    private static String resolveCancelledByForPartnerList(OrderStatus status, String cancelledBy) {
        if (status != OrderStatus.CANCELLED || cancelledBy == null || cancelledBy.isBlank()) {
            return null;
        }
        return cancelledBy.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse.StatusHistoryDTO> getOrderHistory(
            Long orderId,
            OrderStatus status,
            String actorType,
            String from,
            String to,
            Pageable pageable
    ) {
        getOrderOrThrow(orderId);
        final LocalDateTime fromDt = parseOptionalHistoryInstant(from);
        final LocalDateTime toDt = parseOptionalHistoryInstant(to);
        final String actorNorm = actorType == null ? null : actorType.trim();

        final List<OrderResponse.StatusHistoryDTO> all = mapStatusHistory(orderId).stream()
                .filter(dto -> status == null || dto.getStatus() == status)
                .filter(dto -> actorNorm == null || actorNorm.isEmpty()
                        || (dto.getActorType() != null
                        && actorNorm.equalsIgnoreCase(dto.getActorType())))
                .filter(dto -> fromDt == null || !dto.getTimestamp().isBefore(fromDt))
                .filter(dto -> toDt == null || !dto.getTimestamp().isAfter(toDt))
                .toList();

        final int start = (int) pageable.getOffset();
        final int end = Math.min(start + pageable.getPageSize(), all.size());
        final List<OrderResponse.StatusHistoryDTO> slice =
                start >= all.size() ? List.of() : all.subList(start, end);
        return new PageImpl<>(slice, pageable, all.size());
    }

    private static LocalDateTime parseOptionalHistoryInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            if (s.endsWith("Z")) {
                return LocalDateTime.ofInstant(Instant.parse(s), ZoneId.systemDefault());
            }
            if (s.length() == 16) {
                s = s + ":00";
            }
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Format de date invalide (ex. 2026-04-07T17:29:00)"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getPartnerOrderCounts(Long partnerId, LocalDateTime from, LocalDateTime to) {
        final LocalDateTime effectiveFrom = from != null ? from : DATE_SENTINEL_FROM;
        final LocalDateTime effectiveTo   = to   != null ? to   : DATE_SENTINEL_TO;
        final java.util.LinkedHashMap<String, Long> counts = new java.util.LinkedHashMap<>();
        counts.put("ALL",       orderRepository.countByPartnerIdAndOrderTimeBetween(partnerId, effectiveFrom, effectiveTo));
        counts.put("PENDING",   orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(partnerId, OrderStatus.PENDING, effectiveFrom, effectiveTo));
        counts.put("CONFIRMED", orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(partnerId, OrderStatus.CONFIRMED, effectiveFrom, effectiveTo));
        counts.put("PREPARING", orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(partnerId, OrderStatus.PREPARING, effectiveFrom, effectiveTo));
        counts.put("READY",     orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(partnerId, OrderStatus.READY_FOR_PICKUP, effectiveFrom, effectiveTo));
        counts.put("CANCELLED", orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(partnerId, OrderStatus.CANCELLED, effectiveFrom, effectiveTo));
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerOrderHistorySummaryDTO getPartnerOrderHistorySummary(
            Long partnerId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        final LocalDateTime effectiveFrom = from != null ? from : DATE_SENTINEL_FROM;
        final LocalDateTime effectiveTo   = to   != null ? to   : DATE_SENTINEL_TO;
        final long totalOrders = orderRepository.countByPartnerIdAndOrderTimeBetween(
                partnerId, effectiveFrom, effectiveTo);
        final long cancelledCount = orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(
                partnerId, OrderStatus.CANCELLED, effectiveFrom, effectiveTo);
        BigDecimal revenueTnd = orderRepository.sumTotalByPartnerIdStatusAndOrderTimeBetween(
                partnerId, OrderStatus.DELIVERED, effectiveFrom, effectiveTo);
        if (revenueTnd == null) {
            revenueTnd = BigDecimal.ZERO;
        }
        final double cancellationRatePercent = totalOrders == 0
                ? 0.0
                : BigDecimal.valueOf(cancelledCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                .doubleValue();
        return PartnerOrderHistorySummaryDTO.builder()
                .totalOrders(totalOrders)
                .revenueTnd(revenueTnd)
                .cancelledCount(cancelledCount)
                .cancellationRatePercent(cancellationRatePercent)
                .build();
    }

    private OrderResponse.DeliveryAddressDTO parseDeliveryAddress(String deliveryAddressJson) {
        if (deliveryAddressJson == null || deliveryAddressJson.isBlank()) {
            return null;
        }

        try {
            final Map<String, Object> payload = objectMapper.readValue(deliveryAddressJson, new TypeReference<Map<String, Object>>() {});
            final String addressId = asString(payload.get("addressId"));
            final String deliveryAddress = trimToNull(asString(payload.get("deliveryAddress")));
            final String deliveryLocation = trimToNull(asString(payload.get("deliveryLocation")));
            final String formattedAddress = firstNonBlank(
                asString(payload.get("formattedAddress")),
                deliveryAddress,
                deliveryLocation,
                addressId == null ? null : "addressId:" + addressId
            );

            return OrderResponse.DeliveryAddressDTO.builder()
                    .street(asString(payload.get("street")))
                    .building(asString(payload.get("building")))
                    .floor(asString(payload.get("floor")))
                    .apartment(asString(payload.get("apartment")))
                    .city(asString(payload.get("city")))
                    .postalCode(asString(payload.get("postalCode")))
                .latitude(firstNonNullDecimal(
                    asBigDecimalOrNull(payload.get("deliveryLatitude")),
                    asBigDecimalOrNull(payload.get("latitude"))
                ))
                .longitude(firstNonNullDecimal(
                    asBigDecimalOrNull(payload.get("deliveryLongitude")),
                    asBigDecimalOrNull(payload.get("longitude"))
                ))
                    .formattedAddress(formattedAddress)
                    .build();
        } catch (Exception ex) {
            return OrderResponse.DeliveryAddressDTO.builder()
                    .formattedAddress(deliveryAddressJson)
                    .build();
        }
    }

    private Order.PaymentMethod parsePaymentMethod(String paymentMethodRaw) {
        if (paymentMethodRaw == null || paymentMethodRaw.isBlank()) {
            throw badRequest("La méthode de paiement est obligatoire");
        }

        try {
            return Order.PaymentMethod.valueOf(paymentMethodRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw badRequest("Méthode de paiement invalide: " + paymentMethodRaw);
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private Integer asInteger(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        return fallback;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return ZERO;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        if (value instanceof String text) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return ZERO;
            }
        }

        return ZERO;
    }

    private BigDecimal asBigDecimalOrNull(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        if (value instanceof String text) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private BigDecimal firstNonNullDecimal(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record ResolvedCustomerInfo(String name, String email, String phone) {}

    private record ResolvedDeliveryAddress(
            String deliveryAddressJson,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            String deliveryInstructions
    ) {}

    // ==================== REFUND ====================

    @Override
    @Transactional
    public OrderResponse refundOrder(Long orderId, BigDecimal amount) {
        Order order = getOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le remboursement n'est possible que pour les commandes livrées ou annulées");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("Le montant du remboursement doit être supérieur à 0");
        }
        if (amount.compareTo(order.getTotal()) > 0) {
            throw badRequest("Le montant du remboursement ne peut pas dépasser le total de la commande");
        }
        boolean isPartial = amount.compareTo(order.getTotal()) < 0;
        order.setPaymentStatus(isPartial ? Order.PaymentStatus.PARTIALLY_REFUNDED : Order.PaymentStatus.REFUNDED);
        orderRepository.save(order);

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .orderId(order.getId())
                .previousStatus(order.getStatus())
                .status(order.getStatus())
                .description(isPartial ? "Remboursement partiel" : "Remboursement total")
                .notes("Montant remboursé : " + scaleMoney(amount).toPlainString() + " TND")
                .updatedBy("ADMIN")
                .actorType("ADMIN")
                .build());

        return toResponse(order);
    }

    // ==================== ADMIN EXPORT ====================

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listAllAdminOrdersForExport(
            OrderStatus status, String paymentMethod, String paymentStatus,
            String search, LocalDateTime startDate, LocalDateTime endDate,
            Long partnerId, Long courierId, BigDecimal amountMin, BigDecimal amountMax) {
        final List<OrderResponse> out = new ArrayList<>();
        int page = 0;
        while (true) {
            final Pageable pageable = PageRequest.of(page, EXPORT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
            final Page<OrderResponse> chunk = getAdminOrders(status, paymentMethod, paymentStatus,
                    search, startDate, endDate, partnerId, courierId, amountMin, amountMax, pageable);
            out.addAll(chunk.getContent());
            if (!chunk.hasNext()) break;
            page++;
        }
        return out;
    }

    // ==================== ADMIN LOGS ====================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminLogDTO> getAdminLogs(String actorType, OrderStatus status, Long orderId,
                                          LocalDateTime from, LocalDateTime to, Pageable pageable) {
        final String actor = (actorType != null && !actorType.isBlank()) ? actorType.trim().toUpperCase(Locale.ROOT) : null;
        Page<OrderStatusHistory> page = orderStatusHistoryRepository.findAllLogs(actor, status, from, to, orderId, pageable);
        return page.map(h -> {
            String orderNumber = orderRepository.findById(h.getOrderId())
                    .map(com.speedline.order.domain.Order::getOrderNumber).orElse(null);
            return AdminLogDTO.builder()
                    .id(h.getId())
                    .orderId(h.getOrderId())
                    .orderNumber(orderNumber)
                    .status(h.getStatus())
                    .previousStatus(h.getPreviousStatus())
                    .description(h.getDescription())
                    .notes(h.getNotes())
                    .actorType(h.getActorType())
                    .actorId(h.getActorId())
                    .updatedBy(h.getUpdatedBy())
                    .timestamp(h.getTimestamp())
                    .build();
        });
    }
}
