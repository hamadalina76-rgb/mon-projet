package com.speedline.order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.order.client.PartnerServiceClient;
import com.speedline.order.client.PromotionServiceClient;
import com.speedline.order.client.dto.PartnerSnapshot;
import com.speedline.order.client.dto.ProductSnapshot;
import com.speedline.order.client.dto.promotion.PromotionApiResponse;
import com.speedline.order.client.dto.promotion.PromotionValidateRequest;
import com.speedline.order.client.dto.promotion.PromotionValidateResponse;
import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderItem;
import com.speedline.order.domain.OrderStatus;
import com.speedline.order.domain.OrderStatusHistory;
import com.speedline.order.dto.CreateOrderRequest;
import com.speedline.order.dto.OrderItemDTO;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.dto.cart.CartItemPayload;
import com.speedline.order.dto.cart.CartResponse;
import com.speedline.order.dto.checkout.CheckoutOrderRequest;
import com.speedline.order.event.OrderCreatedEvent;
import com.speedline.order.event.producer.OrderEventProducer;
import com.speedline.order.repository.OrderItemRepository;
import com.speedline.order.repository.OrderRepository;
import com.speedline.order.repository.OrderStatusHistoryRepository;
import com.speedline.order.service.CartService;
import com.speedline.order.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
                cartItems,
                request.getPromoCode(),
                addressId,
                paymentMethod,
            false,
            request.getIsScheduled(),
            request.getScheduledDeliveryTime()
        );
    }

    @Override
    @Transactional
    public OrderResponse createOrderFromCheckout(Long customerId, CheckoutOrderRequest request) {
        if (customerId == null) {
            throw badRequest("L'identifiant client est obligatoire");
        }

        if (request == null) {
            throw badRequest("Le payload checkout est obligatoire");
        }

        final List<CartItemPayload> cartItems = resolveCartItemsForCheckout(customerId, request);
        if (cartItems.isEmpty()) {
            throw badRequest("Le panier est vide");
        }

        return createOrderInternal(
                customerId,
                cartItems,
                request.getPromoCode(),
                request.getAddressId(),
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
        final String normalizedActorType = normalizeActorType(actorType);
        applyStatusTransition(
                order,
                newStatus,
                normalizedActorType,
                actorId,
                notes,
                "Statut mis à jour"
        );
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

        applyStatusTransition(
                order,
                OrderStatus.PREPARING,
                "PARTNER",
                partnerId,
                null,
                "Commande confirmée par le partenaire"
        );

        if (estimatedPrepTime != null && estimatedPrepTime > 0 && !Boolean.TRUE.equals(order.getIsScheduled())) {
            final int prepMinutes = Math.max(10, estimatedPrepTime);
            order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(prepMinutes + 15L));
            orderRepository.save(order);
        }

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

    private OrderResponse createOrderInternal(
            Long customerId,
            List<CartItemPayload> cartItems,
            String promoCode,
            String addressId,
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

        final List<OrderItem> orderItems = buildOrderItems(partnerId, cartItems);
        if (orderItems.isEmpty()) {
            throw badRequest("Aucun article valide à commander");
        }

        final BigDecimal subtotal = scaleMoney(orderItems.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        validateMinimumOrder(partner, subtotal);

        final String normalizedPromoCode = trimToNull(promoCode);
        final BigDecimal deliveryFee = scaleMoney(partner.getDeliveryFee());
        final BigDecimal serviceFee = calculateServiceFee(partner, subtotal);
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

        final Order order = Order.builder()
                .customerId(customerId)
                .partnerId(partnerId)
                .status(OrderStatus.PENDING)
                .type(Order.OrderType.DELIVERY)
                .customerName("Client #" + customerId)
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
                .deliveryAddressJson(buildDeliveryAddressJson(addressId))
                .paymentMethod(paymentMethod)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .isScheduled(normalizedScheduledDeliveryTime != null)
                .scheduledDeliveryTime(normalizedScheduledDeliveryTime)
                .estimatedDeliveryTime(
                    normalizedScheduledDeliveryTime != null
                        ? normalizedScheduledDeliveryTime
                        : estimateDeliveryTime(partner)
                )
                .build();

        final Order savedOrder = orderRepository.save(order);

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

        if (clearCartAfterSuccess) {
            cartService.clearCart(customerId);
        }

        return getOrderById(savedOrder.getId());
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
                    final List<String> normalizedOptions = item.getSelectedOptions() == null
                            ? List.of()
                            : item.getSelectedOptions().stream()
                            .filter(Objects::nonNull)
                            .map(option -> option.trim().toLowerCase(Locale.ROOT))
                            .filter(option -> !option.isEmpty())
                            .sorted()
                            .toList();

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

    private BigDecimal calculateServiceFee(PartnerSnapshot partner, BigDecimal subtotal) {
        final BigDecimal explicitServiceFee = partner.getServiceFee();
        if (explicitServiceFee != null && explicitServiceFee.compareTo(BigDecimal.ZERO) > 0) {
            return scaleMoney(explicitServiceFee);
        }

        final BigDecimal commissionRate = partner.getCommissionRate();
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }

        return scaleMoney(subtotal
                .multiply(commissionRate)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
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

            final BigDecimal maxDiscount = subtotal.add(deliveryFee).add(serviceFee);
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service promotion indisponible");
        } catch (Exception ex) {
            log.error("Erreur lors de la validation du code promo {}", promoCode, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service promotion indisponible");
        }
    }

    private void publishOrderCreated(Order order, List<OrderItem> orderItems) {
        try {
            final int itemCount = orderItems.stream()
                    .map(OrderItem::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(0, Integer::sum);

            final OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .customerId(order.getCustomerId())
                    .partnerId(order.getPartnerId())
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

    private LocalDateTime estimateDeliveryTime(PartnerSnapshot partner) {
        final int partnerPreparation = partner.getPreparationTime() == null ? 20 : Math.max(0, partner.getPreparationTime());
        final int totalMinutes = Math.max(30, partnerPreparation + 15);
        return LocalDateTime.now().plusMinutes(totalMinutes);
    }

    private List<OrderItem> buildOrderItems(Long partnerId, List<CartItemPayload> cartItems) {
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

            final OrderItem orderItem = OrderItem.builder()
                    .productId(productId)
                    .productName(resolveProductName(product, cartItem, productId))
                    .productDescription(product.getDescription())
                    .productImage(product.getImageUrl())
                    .quantity(quantity)
                    .unitPrice(normalizedUnitPrice)
                    .modifiersTotal(ZERO)
                    .subtotal(scaleMoney(normalizedUnitPrice.multiply(BigDecimal.valueOf(quantity))))
                    .selectedOptionsJson(writeJson(cartItem.getSelectedOptions() == null ? List.of() : cartItem.getSelectedOptions()))
                    .selectedAddonsJson("[]")
                    .specialInstructions(trimToNull(cartItem.getKitchenNote()))
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
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
            return "[]";
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
                            .optionName(asString(map.get("optionName")))
                            .valueId(asLong(map.get("valueId")))
                            .valueName(asString(map.get("valueName")))
                            .priceModifier(scaleMoney(asBigDecimal(map.get("priceModifier"))))
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

    private List<OrderResponse.StatusHistoryDTO> mapStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(orderId).stream()
                .map(history -> OrderResponse.StatusHistoryDTO.builder()
                        .status(history.getStatus())
                        .description(history.getDescription())
                        .updatedBy(history.getUpdatedBy())
                        .timestamp(history.getTimestamp())
                        .build())
                .toList();
    }

    private OrderResponse.DeliveryAddressDTO parseDeliveryAddress(String deliveryAddressJson) {
        if (deliveryAddressJson == null || deliveryAddressJson.isBlank()) {
            return null;
        }

        try {
            final Map<String, Object> payload = objectMapper.readValue(deliveryAddressJson, new TypeReference<Map<String, Object>>() {});
            final String addressId = asString(payload.get("addressId"));
            final String formattedAddress = trimToNull(asString(payload.get("formattedAddress"))) != null
                    ? asString(payload.get("formattedAddress"))
                    : (addressId == null ? null : "addressId:" + addressId);

            return OrderResponse.DeliveryAddressDTO.builder()
                    .street(asString(payload.get("street")))
                    .building(asString(payload.get("building")))
                    .floor(asString(payload.get("floor")))
                    .apartment(asString(payload.get("apartment")))
                    .city(asString(payload.get("city")))
                    .postalCode(asString(payload.get("postalCode")))
                    .latitude(asBigDecimal(payload.get("latitude")))
                    .longitude(asBigDecimal(payload.get("longitude")))
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
}
