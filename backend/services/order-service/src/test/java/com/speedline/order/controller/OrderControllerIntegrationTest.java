package com.speedline.order.controller;

import com.speedline.order.client.PartnerServiceClient;
import com.speedline.order.client.PromotionServiceClient;
import com.speedline.order.client.dto.PartnerSnapshot;
import com.speedline.order.client.dto.ProductSnapshot;
import com.speedline.order.client.dto.promotion.PromotionApiResponse;
import com.speedline.order.client.dto.promotion.PromotionValidateRequest;
import com.speedline.order.client.dto.promotion.PromotionValidateResponse;
import com.speedline.order.dto.cart.CartItemPayload;
import com.speedline.order.dto.cart.CartResponse;
import com.speedline.order.event.producer.OrderEventProducer;
import com.speedline.order.domain.Order;
import com.speedline.order.domain.OrderStatus;
import com.speedline.order.repository.OrderItemRepository;
import com.speedline.order.repository.OrderRepository;
import com.speedline.order.repository.OrderStatusHistoryRepository;
import com.speedline.order.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @MockBean
    private CartService cartService;

    @MockBean
    private PartnerServiceClient partnerServiceClient;

    @MockBean
    private PromotionServiceClient promotionServiceClient;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        orderStatusHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        when(cartService.getCart(6L)).thenReturn(CartResponse.builder().items(List.of()).ttlSeconds(0L).build());
        when(partnerServiceClient.getPartnerById(2L)).thenReturn(openPartner());
    }

    @Test
    void postOrdersShouldRecalculatePriceFromPartnerCatalog() throws Exception {
        when(partnerServiceClient.getProductById(2L, 9L)).thenReturn(product(9L, "Burger", "20.00"));
        when(partnerServiceClient.getProductById(2L, 11L)).thenReturn(product(11L, "Pizza", "30.00"));

        String payload = """
                {
                  "cartItems": [
                    {
                      "productId": "9",
                      "partnerId": "2",
                      "quantity": 1,
                      "unitPrice": 1.0,
                      "selectedOptions": ["Taille: Moyenne"]
                    },
                    {
                      "productId": "11",
                      "partnerId": "2",
                      "quantity": 1,
                      "unitPrice": 2.0,
                      "selectedOptions": []
                    }
                  ],
                  "promoCode": null,
                  "addressId": null,
                  "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(50.0))
                .andExpect(jsonPath("$.deliveryFee").value(5.0))
                .andExpect(jsonPath("$.discount").value(0.0))
                .andExpect(jsonPath("$.total").value(55.0));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void postOrdersShouldApplyValidPromoCode() throws Exception {
        when(partnerServiceClient.getProductById(2L, 9L)).thenReturn(product(9L, "Burger", "20.00"));

        PromotionApiResponse<PromotionValidateResponse> promoResponse = PromotionApiResponse.<PromotionValidateResponse>builder()
                .success(true)
                .message("OK")
                .data(PromotionValidateResponse.builder()
                        .isValid(true)
                        .discountAmount(new BigDecimal("4.00"))
                        .message("Promo valide")
                        .build())
                .build();
        when(promotionServiceClient.validatePromotion(any(PromotionValidateRequest.class))).thenReturn(promoResponse);

        String payload = """
                {
                  "cartItems": [
                    {
                      "productId": "9",
                      "partnerId": "2",
                      "quantity": 1,
                      "unitPrice": 1.0,
                      "selectedOptions": []
                    }
                  ],
                  "promoCode": "PROMO10",
                  "addressId": null,
                  "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(20.0))
                .andExpect(jsonPath("$.deliveryFee").value(5.0))
                .andExpect(jsonPath("$.discount").value(4.0))
                .andExpect(jsonPath("$.total").value(21.0));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void postOrdersShouldRejectInvalidPromoCode() throws Exception {
        when(partnerServiceClient.getProductById(2L, 9L)).thenReturn(product(9L, "Burger", "20.00"));

        PromotionApiResponse<PromotionValidateResponse> promoResponse = PromotionApiResponse.<PromotionValidateResponse>builder()
                .success(true)
                .message("Code promo invalide")
                .data(PromotionValidateResponse.builder()
                        .isValid(false)
                        .discountAmount(BigDecimal.ZERO)
                        .message("Code promo invalide")
                        .build())
                .build();
        when(promotionServiceClient.validatePromotion(any(PromotionValidateRequest.class))).thenReturn(promoResponse);

        String payload = """
                {
                  "cartItems": [
                    {
                      "productId": "9",
                      "partnerId": "2",
                      "quantity": 1,
                      "unitPrice": 1.0,
                      "selectedOptions": []
                    }
                  ],
                  "promoCode": "BADCODE",
                  "addressId": null,
                  "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void postOrdersShouldRejectWhenCartSignatureMismatch() throws Exception {
        CartItemPayload serverCartItem = CartItemPayload.builder()
                .productId("9")
                .partnerId("2")
                .quantity(2)
                .selectedOptions(List.of())
                .build();
        when(cartService.getCart(6L)).thenReturn(CartResponse.builder()
                .items(List.of(serverCartItem))
                .ttlSeconds(1200L)
                .build());

        String payload = """
                {
                  "cartItems": [
                    {
                      "productId": "9",
                      "partnerId": "2",
                      "quantity": 1,
                      "unitPrice": 1.0,
                      "selectedOptions": []
                    }
                  ],
                  "promoCode": null,
                  "addressId": null,
                  "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void partnerListDateFilterIncludesScheduledDeliveryWindow() {
        final long partnerId = 42L;
        final LocalDate today = LocalDate.now();
        final LocalDateTime windowStart = today.atStartOfDay();
        final LocalDateTime windowEnd = today.atTime(23, 59, 59);
        final LocalDateTime orderPlacedYesterday = today.minusDays(1).atTime(10, 0);
        final LocalDateTime slotToday = today.atTime(14, 30);

        orderRepository.save(Order.builder()
                .orderNumber("ORD-SCHED-IN-WINDOW")
                .customerId(1L)
                .partnerId(partnerId)
                .status(OrderStatus.PENDING)
                .total(BigDecimal.valueOf(25.00))
                .subtotal(BigDecimal.valueOf(25.00))
                .orderTime(orderPlacedYesterday)
                .isScheduled(true)
                .scheduledDeliveryTime(slotToday)
                .paymentMethod(Order.PaymentMethod.CASH)
                .build());

        orderRepository.save(Order.builder()
                .orderNumber("ORD-NOT-IN-WINDOW")
                .customerId(1L)
                .partnerId(partnerId)
                .status(OrderStatus.PENDING)
                .total(BigDecimal.TEN)
                .subtotal(BigDecimal.TEN)
                .orderTime(orderPlacedYesterday)
                .isScheduled(false)
                .scheduledDeliveryTime(null)
                .paymentMethod(Order.PaymentMethod.CASH)
                .build());

        var page = orderRepository.findByPartnerIdPriority(
                partnerId, windowStart, windowEnd, "", PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(Order::getOrderNumber)
                .contains("ORD-SCHED-IN-WINDOW")
                .doesNotContain("ORD-NOT-IN-WINDOW");

        long pendingCount = orderRepository.countByPartnerIdAndStatusAndOrderTimeBetween(
                partnerId, OrderStatus.PENDING, windowStart, windowEnd);
        assertThat(pendingCount).isEqualTo(1L);
    }

    private PartnerSnapshot openPartner() {
        return PartnerSnapshot.builder()
                .id(2L)
                .businessName("Partner 2")
                .acceptsOrders(true)
                .isCurrentlyOpen(true)
                .deliveryFee(new BigDecimal("5.00"))
                .minimumOrder(BigDecimal.ZERO)
                .build();
    }

    private ProductSnapshot product(Long id, String name, String unitPrice) {
        return ProductSnapshot.builder()
                .id(id)
                .partnerId(2L)
                .name(name)
                .price(new BigDecimal(unitPrice))
                .isAvailable(true)
                .build();
    }
}
