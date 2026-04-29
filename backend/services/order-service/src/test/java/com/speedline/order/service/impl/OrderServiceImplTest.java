package com.speedline.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.order.client.LocationServiceClient;
import com.speedline.order.client.PartnerServiceClient;
import com.speedline.order.client.PromotionServiceClient;
import com.speedline.order.client.UserServiceClient;
import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderItem;
import com.speedline.order.domain.OrderStatus;
import com.speedline.order.domain.OrderStatusHistory;
import com.speedline.order.dto.OrderItemDTO;
import com.speedline.order.dto.OrderResponse;
import com.speedline.order.event.producer.OrderEventProducer;
import com.speedline.order.repository.OrderItemRepository;
import com.speedline.order.repository.OrderRepository;
import com.speedline.order.repository.OrderStatusHistoryRepository;
import com.speedline.order.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartService cartService;

    @Mock
    private PartnerServiceClient partnerServiceClient;

    @Mock
    private PromotionServiceClient promotionServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private LocationServiceClient locationServiceClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-2026-00001")
                .customerId(100L)
                .partnerId(200L)
                .courierId(null)
                .status(OrderStatus.PENDING)
                .type(Order.OrderType.DELIVERY)
                .subtotal(new BigDecimal("25.00"))
                .deliveryFee(new BigDecimal("3.00"))
                .serviceFee(new BigDecimal("1.50"))
                .tax(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .tip(BigDecimal.ZERO)
                .total(new BigDecimal("29.50"))
                .paymentMethod(Order.PaymentMethod.CASH)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .orderTime(LocalDateTime.now())
                .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(45))
                .isScheduled(false)
                .build();
    }

    // =====================================================================
    // getOrderById
    // =====================================================================

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.getOrderById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getOrderNumber()).isEqualTo("ORD-2026-00001");
            assertThat(response.getCustomerId()).isEqualTo(100L);
            assertThat(response.getPartnerId()).isEqualTo(200L);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("29.50"));
        }

        @Test
        @DisplayName("should throw NOT_FOUND when order does not exist")
        void shouldThrowNotFoundWhenOrderDoesNotExist() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Commande introuvable");
        }
    }

    // =====================================================================
    // getOrderByNumber
    // =====================================================================

    @Nested
    @DisplayName("getOrderByNumber")
    class GetOrderByNumber {

        @Test
        @DisplayName("should return order when found by number")
        void shouldReturnOrderWhenFoundByNumber() {
            when(orderRepository.findByOrderNumber("ORD-2026-00001")).thenReturn(Optional.of(sampleOrder));
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.getOrderByNumber("ORD-2026-00001");

            assertThat(response).isNotNull();
            assertThat(response.getOrderNumber()).isEqualTo("ORD-2026-00001");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when order number is blank")
        void shouldThrowBadRequestWhenOrderNumberIsBlank() {
            assertThatThrownBy(() -> orderService.getOrderByNumber(""))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when order number is null")
        void shouldThrowBadRequestWhenOrderNumberIsNull() {
            assertThatThrownBy(() -> orderService.getOrderByNumber(null))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when order number does not exist")
        void shouldThrowNotFoundWhenOrderNumberDoesNotExist() {
            when(orderRepository.findByOrderNumber("ORD-0000-99999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderByNumber("ORD-0000-99999"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Commande introuvable");
        }
    }

    // =====================================================================
    // updateStatus
    // =====================================================================

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should transition PENDING to CONFIRMED")
        void shouldTransitionPendingToConfirmed() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                    .thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.updateStatus(1L, OrderStatus.CONFIRMED, "ADMIN", 1L, "Test notes");

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        }

        @Test
        @DisplayName("should transition CONFIRMED to PREPARING")
        void shouldTransitionConfirmedToPreparing() {
            sampleOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.updateStatus(1L, OrderStatus.PREPARING, "PARTNER", 200L, null);

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should reject invalid status transition PENDING to DELIVERED")
        void shouldRejectInvalidStatusTransition() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() ->
                    orderService.updateStatus(1L, OrderStatus.DELIVERED, "SYSTEM", null, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Transition de statut invalide");
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when new status is null")
        void shouldThrowBadRequestWhenNewStatusIsNull() {
            assertThatThrownBy(() ->
                    orderService.updateStatus(1L, null, "SYSTEM", null, null))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should reject invalid actor type")
        void shouldRejectInvalidActorType() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() ->
                    orderService.updateStatus(1L, OrderStatus.CONFIRMED, "INVALID_TYPE", 1L, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Type d'acteur invalide");
        }

        @Test
        @DisplayName("should allow same-status transition without error")
        void shouldAllowSameStatusTransition() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.updateStatus(1L, OrderStatus.PENDING, "SYSTEM", null, null);

            assertThat(response).isNotNull();
        }
    }

    // =====================================================================
    // confirmOrder
    // =====================================================================

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("should confirm order and transition to PREPARING when not scheduled")
        void shouldConfirmOrderAndTransitionToPreparing() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            sampleOrder.setIsScheduled(false);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.confirmOrder(1L, 200L, 30);

            assertThat(response).isNotNull();
            verify(orderRepository, atLeastOnce()).save(any(Order.class));
        }

        @Test
        @DisplayName("should confirm scheduled order and stay at CONFIRMED")
        void shouldConfirmScheduledOrder() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            sampleOrder.setIsScheduled(true);
            sampleOrder.setScheduledDeliveryTime(LocalDateTime.now().plusHours(2));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.confirmOrder(1L, 200L, null);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when partnerId is null")
        void shouldThrowBadRequestWhenPartnerIdIsNull() {
            assertThatThrownBy(() -> orderService.confirmOrder(1L, null, 30))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw FORBIDDEN when partner does not own order")
        void shouldThrowForbiddenWhenPartnerDoesNotOwnOrder() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.confirmOrder(1L, 999L, 30))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("n'appartient pas");
        }
    }

    // =====================================================================
    // markAsReady
    // =====================================================================

    @Nested
    @DisplayName("markAsReady")
    class MarkAsReady {

        @Test
        @DisplayName("should mark PREPARING order as READY_FOR_PICKUP")
        void shouldMarkPreparingOrderAsReady() {
            sampleOrder.setStatus(OrderStatus.PREPARING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.markAsReady(1L, 200L);

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when partnerId is null")
        void shouldThrowBadRequestWhenPartnerIdIsNull() {
            assertThatThrownBy(() -> orderService.markAsReady(1L, null))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw FORBIDDEN when partner does not own order")
        void shouldThrowForbiddenWhenWrongPartner() {
            sampleOrder.setStatus(OrderStatus.PREPARING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.markAsReady(1L, 999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("n'appartient pas");
        }
    }

    // =====================================================================
    // cancelOrder
    // =====================================================================

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("should cancel PENDING order")
        void shouldCancelPendingOrder() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.cancelOrder(1L, "CUSTOMER", 100L, "Changed my mind");

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should cancel CONFIRMED order")
        void shouldCancelConfirmedOrder() {
            sampleOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.cancelOrder(1L, "PARTNER", 200L, "Out of stock");

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should reject cancellation of DELIVERED order")
        void shouldRejectCancellationOfDeliveredOrder() {
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() ->
                    orderService.cancelOrder(1L, "CUSTOMER", 100L, "Too late"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("ne peut plus être annulée");
        }

        @Test
        @DisplayName("should reject cancellation of already CANCELLED order")
        void shouldRejectCancellationOfAlreadyCancelledOrder() {
            sampleOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() ->
                    orderService.cancelOrder(1L, "ADMIN", 1L, "Duplicate"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("ne peut plus être annulée");
        }

        @Test
        @DisplayName("should throw NOT_FOUND when order does not exist")
        void shouldThrowNotFoundWhenOrderDoesNotExist() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderService.cancelOrder(999L, "CUSTOMER", 100L, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Commande introuvable");
        }
    }

    // =====================================================================
    // assignCourier
    // =====================================================================

    @Nested
    @DisplayName("assignCourier")
    class AssignCourier {

        @Test
        @DisplayName("should assign courier to PENDING order (pre-assignment)")
        void shouldAssignCourierToPendingOrder() {
            sampleOrder.setStatus(OrderStatus.PENDING);
            sampleOrder.setCourierId(null);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.assignCourier(1L, 300L);

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should assign courier to READY_FOR_PICKUP order")
        void shouldAssignCourierToReadyOrder() {
            sampleOrder.setStatus(OrderStatus.READY_FOR_PICKUP);
            sampleOrder.setCourierId(null);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.assignCourier(1L, 300L);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when courierId is null")
        void shouldThrowBadRequestWhenCourierIdIsNull() {
            assertThatThrownBy(() -> orderService.assignCourier(1L, null))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should reject assignment on DELIVERED order")
        void shouldRejectAssignmentOnDeliveredOrder() {
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.assignCourier(1L, 300L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Impossible d'assigner");
        }

        @Test
        @DisplayName("should reject assignment on CANCELLED order")
        void shouldRejectAssignmentOnCancelledOrder() {
            sampleOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.assignCourier(1L, 300L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Impossible d'assigner");
        }

        @Test
        @DisplayName("should reject when different courier is already assigned")
        void shouldRejectWhenDifferentCourierAlreadyAssigned() {
            sampleOrder.setStatus(OrderStatus.PREPARING);
            sampleOrder.setCourierId(300L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.assignCourier(1L, 400L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("deja assigne");
        }

        @Test
        @DisplayName("should allow re-assignment of same courier")
        void shouldAllowReAssignmentOfSameCourier() {
            sampleOrder.setStatus(OrderStatus.PREPARING);
            sampleOrder.setCourierId(300L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.assignCourier(1L, 300L);

            assertThat(response).isNotNull();
        }
    }

    // =====================================================================
    // markAsPickedUp / markAsInDelivery / markAsDelivered
    // =====================================================================

    @Nested
    @DisplayName("courier lifecycle transitions")
    class CourierLifecycle {

        @Test
        @DisplayName("should mark order as picked up")
        void shouldMarkAsPickedUp() {
            sampleOrder.setStatus(OrderStatus.READY_FOR_PICKUP);
            sampleOrder.setCourierId(300L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.markAsPickedUp(1L, 300L);

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw FORBIDDEN when courier is not assigned for pickup")
        void shouldThrowForbiddenOnPickupWhenNotAssigned() {
            sampleOrder.setStatus(OrderStatus.READY_FOR_PICKUP);
            sampleOrder.setCourierId(300L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> orderService.markAsPickedUp(1L, 999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("n'est pas assigné");
        }

        @Test
        @DisplayName("should mark order as in delivery")
        void shouldMarkAsInDelivery() {
            sampleOrder.setStatus(OrderStatus.PICKED_UP);
            sampleOrder.setCourierId(300L);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.markAsInDelivery(1L, 300L);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should mark order as delivered and set payment to COMPLETED for CASH")
        void shouldMarkAsDeliveredAndCompletePaymentForCash() {
            sampleOrder.setStatus(OrderStatus.IN_DELIVERY);
            sampleOrder.setCourierId(300L);
            sampleOrder.setPaymentMethod(Order.PaymentMethod.CASH);
            sampleOrder.setPaymentStatus(Order.PaymentStatus.PENDING);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.markAsDelivered(1L, 300L);

            assertThat(response).isNotNull();
            assertThat(sampleOrder.getPaymentStatus()).isEqualTo(Order.PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when courierId is null for pickup")
        void shouldThrowBadRequestWhenCourierIdIsNullForPickup() {
            assertThatThrownBy(() -> orderService.markAsPickedUp(1L, null))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when courierId is null for delivery")
        void shouldThrowBadRequestWhenCourierIdIsNullForInDelivery() {
            assertThatThrownBy(() -> orderService.markAsInDelivery(1L, null))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when courierId is null for delivered")
        void shouldThrowBadRequestWhenCourierIdIsNullForDelivered() {
            assertThatThrownBy(() -> orderService.markAsDelivered(1L, null))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // =====================================================================
    // getCustomerOrders / getActiveOrdersByCustomer
    // =====================================================================

    @Nested
    @DisplayName("customer order queries")
    class CustomerOrderQueries {

        @Test
        @DisplayName("should return paginated customer orders")
        void shouldReturnPaginatedCustomerOrders() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(sampleOrder), pageable, 1);
            when(orderRepository.findByCustomerId(100L, pageable)).thenReturn(page);
            when(orderItemRepository.findByOrderId(anyLong())).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(anyLong())).thenReturn(List.of());

            Page<OrderResponse> result = orderService.getCustomerOrders(100L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should return active orders for customer")
        void shouldReturnActiveOrdersForCustomer() {
            when(orderRepository.findByCustomerIdAndStatusIn(eq(100L), any()))
                    .thenReturn(List.of(sampleOrder));
            when(orderItemRepository.findByOrderId(anyLong())).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(anyLong())).thenReturn(List.of());

            List<OrderResponse> result = orderService.getActiveOrdersByCustomer(100L);

            assertThat(result).hasSize(1);
        }
    }

    // =====================================================================
    // getPartnerOrders / getActiveOrdersByPartner
    // =====================================================================

    @Nested
    @DisplayName("partner order queries")
    class PartnerOrderQueries {

        @Test
        @DisplayName("should return paginated partner orders")
        void shouldReturnPaginatedPartnerOrders() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Order> page = new PageImpl<>(List.of(sampleOrder), pageable, 1);
            when(orderRepository.findByPartnerId(200L, pageable)).thenReturn(page);
            when(orderItemRepository.findByOrderId(anyLong())).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(anyLong())).thenReturn(List.of());

            Page<OrderResponse> result = orderService.getPartnerOrders(200L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return active orders for partner")
        void shouldReturnActiveOrdersForPartner() {
            when(orderRepository.findByPartnerIdAndStatusIn(eq(200L), any()))
                    .thenReturn(List.of(sampleOrder));
            when(orderItemRepository.findByOrderId(anyLong())).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(anyLong())).thenReturn(List.of());

            List<OrderResponse> result = orderService.getActiveOrdersByPartner(200L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when querying partner orders by null status")
        void shouldThrowBadRequestWhenNullStatusForPartnerOrders() {
            assertThatThrownBy(() ->
                    orderService.getPartnerOrdersByStatus(200L, null, PageRequest.of(0, 10)))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // =====================================================================
    // updateEstimatedDeliveryTime
    // =====================================================================

    @Nested
    @DisplayName("updateEstimatedDeliveryTime")
    class UpdateEstimatedDeliveryTime {

        @Test
        @DisplayName("should update estimated delivery time")
        void shouldUpdateEstimatedDeliveryTime() {
            LocalDateTime newTime = LocalDateTime.now().plusHours(1);
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
            when(orderStatusHistoryRepository.save(any())).thenReturn(OrderStatusHistory.builder().build());
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.updateEstimatedDeliveryTime(1L, newTime);

            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when estimated time is null")
        void shouldThrowBadRequestWhenEstimatedTimeIsNull() {
            assertThatThrownBy(() -> orderService.updateEstimatedDeliveryTime(1L, null))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // =====================================================================
    // trackOrder
    // =====================================================================

    @Nested
    @DisplayName("trackOrder")
    class TrackOrder {

        @Test
        @DisplayName("should return order for tracking (delegates to getOrderById)")
        void shouldReturnOrderForTracking() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
            when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
            when(orderStatusHistoryRepository.findByOrderIdOrderByTimestampAsc(1L)).thenReturn(List.of());

            OrderResponse response = orderService.trackOrder(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
        }
    }

    // =====================================================================
    // getOrdersByDateRange
    // =====================================================================

    @Nested
    @DisplayName("getOrdersByDateRange")
    class GetOrdersByDateRange {

        @Test
        @DisplayName("should throw BAD_REQUEST when start is null")
        void shouldThrowBadRequestWhenStartIsNull() {
            assertThatThrownBy(() ->
                    orderService.getOrdersByDateRange(null, LocalDateTime.now(), PageRequest.of(0, 10)))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when end is null")
        void shouldThrowBadRequestWhenEndIsNull() {
            assertThatThrownBy(() ->
                    orderService.getOrdersByDateRange(LocalDateTime.now(), null, PageRequest.of(0, 10)))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when end is before start")
        void shouldThrowBadRequestWhenEndIsBeforeStart() {
            LocalDateTime now = LocalDateTime.now();
            assertThatThrownBy(() ->
                    orderService.getOrdersByDateRange(now, now.minusDays(1), PageRequest.of(0, 10)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("postérieure");
        }
    }

    // =====================================================================
    // getOrdersByStatus
    // =====================================================================

    @Nested
    @DisplayName("getOrdersByStatus")
    class GetOrdersByStatus {

        @Test
        @DisplayName("should throw BAD_REQUEST when status is null")
        void shouldThrowBadRequestWhenStatusIsNull() {
            assertThatThrownBy(() ->
                    orderService.getOrdersByStatus(null, PageRequest.of(0, 10)))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }
}
