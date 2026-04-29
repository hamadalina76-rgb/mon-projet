package com.speedline.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.order.dto.cart.CartItemPayload;
import com.speedline.order.dto.cart.CartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    private static final String CART_KEY_PREFIX = "cart:user:";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartService, "configuredTtlHours", 24L);
    }

    private CartItemPayload sampleItem(String productId, int quantity) {
        return CartItemPayload.builder()
                .productId(productId)
                .partnerId("partner-1")
                .partnerName("Test Restaurant")
                .partnerLogoUrl("https://example.com/logo.png")
                .productName("Margherita Pizza")
                .unitPrice(new BigDecimal("12.50"))
                .quantity(quantity)
                .selectedOptions(List.of())
                .kitchenNote(null)
                .build();
    }

    // =====================================================================
    // getCart
    // =====================================================================

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("should return empty cart when Redis key does not exist")
        void shouldReturnEmptyCartWhenKeyDoesNotExist() {
            String key = CART_KEY_PREFIX + 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(null);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTtlSeconds()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return empty cart when Redis value is blank")
        void shouldReturnEmptyCartWhenValueIsBlank() {
            String key = CART_KEY_PREFIX + 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn("   ");
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
        }

        @Test
        @DisplayName("should return cart items when Redis has valid data")
        void shouldReturnCartItemsWhenRedisHasValidData() throws Exception {
            String key = CART_KEY_PREFIX + 42L;
            String json = "[{\"productId\":\"p1\",\"quantity\":2}]";
            List<CartItemPayload> items = List.of(sampleItem("p1", 2));

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(json);
            when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(items);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(3600L);

            CartResponse response = cartService.getCart(42L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getTtlSeconds()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("should return empty cart and delete key when JSON is invalid")
        void shouldReturnEmptyCartWhenJsonIsInvalid() throws Exception {
            String key = CART_KEY_PREFIX + 1L;
            String badJson = "{invalid-json}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(badJson);
            when(objectMapper.readValue(eq(badJson), any(TypeReference.class)))
                    .thenThrow(new RuntimeException("parse error"));
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L);

            CartResponse response = cartService.getCart(1L);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should normalize negative TTL to zero")
        void shouldNormalizeNegativeTtlToZero() {
            String key = CART_KEY_PREFIX + 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(null);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-1L);

            CartResponse response = cartService.getCart(1L);

            assertThat(response.getTtlSeconds()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should normalize null TTL to zero")
        void shouldNormalizeNullTtlToZero() {
            String key = CART_KEY_PREFIX + 1L;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(null);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(null);

            CartResponse response = cartService.getCart(1L);

            assertThat(response.getTtlSeconds()).isEqualTo(0L);
        }
    }

    // =====================================================================
    // patchCart
    // =====================================================================

    @Nested
    @DisplayName("patchCart")
    class PatchCart {

        @Test
        @DisplayName("should save items to Redis when items are valid")
        void shouldSaveItemsToRedis() throws Exception {
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = List.of(sampleItem("p1", 2));
            String serialized = "[{\"productId\":\"p1\"}]";

            when(objectMapper.writeValueAsString(any())).thenReturn(serialized);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(86400L);

            CartResponse response = cartService.patchCart(1L, items);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(1);
            verify(valueOperations).set(eq(key), eq(serialized), any(Duration.class));
        }

        @Test
        @DisplayName("should delete key when items list is empty")
        void shouldDeleteKeyWhenItemsListIsEmpty() {
            String key = CART_KEY_PREFIX + 1L;

            CartResponse response = cartService.patchCart(1L, List.of());

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTtlSeconds()).isEqualTo(0L);
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should delete key when items list is null")
        void shouldDeleteKeyWhenItemsListIsNull() {
            String key = CART_KEY_PREFIX + 1L;

            CartResponse response = cartService.patchCart(1L, null);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).isEmpty();
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should filter out items with zero quantity")
        void shouldFilterOutItemsWithZeroQuantity() {
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = List.of(sampleItem("p1", 0));

            CartResponse response = cartService.patchCart(1L, items);

            assertThat(response.getItems()).isEmpty();
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should filter out items with negative quantity")
        void shouldFilterOutItemsWithNegativeQuantity() {
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = List.of(sampleItem("p1", -1));

            CartResponse response = cartService.patchCart(1L, items);

            assertThat(response.getItems()).isEmpty();
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should filter out null items")
        void shouldFilterOutNullItems() {
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = new ArrayList<>();
            items.add(null);

            CartResponse response = cartService.patchCart(1L, items);

            assertThat(response.getItems()).isEmpty();
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should filter out items with null quantity")
        void shouldFilterOutItemsWithNullQuantity() {
            String key = CART_KEY_PREFIX + 1L;
            CartItemPayload item = CartItemPayload.builder()
                    .productId("p1")
                    .quantity(null)
                    .build();

            CartResponse response = cartService.patchCart(1L, List.of(item));

            assertThat(response.getItems()).isEmpty();
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should trim product name and partner name")
        void shouldTrimNames() throws Exception {
            String key = CART_KEY_PREFIX + 1L;
            CartItemPayload item = CartItemPayload.builder()
                    .productId("  p1  ")
                    .partnerId("  partner-1  ")
                    .partnerName("  My Restaurant  ")
                    .partnerLogoUrl("  https://logo.png  ")
                    .productName("  Pizza  ")
                    .unitPrice(new BigDecimal("10.00"))
                    .quantity(1)
                    .selectedOptions(null)
                    .kitchenNote("  extra cheese  ")
                    .build();

            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(86400L);

            CartResponse response = cartService.patchCart(1L, List.of(item));

            assertThat(response.getItems()).hasSize(1);
            CartItemPayload saved = response.getItems().get(0);
            assertThat(saved.getProductId()).isEqualTo("p1");
            assertThat(saved.getPartnerId()).isEqualTo("partner-1");
            assertThat(saved.getPartnerName()).isEqualTo("My Restaurant");
            assertThat(saved.getProductName()).isEqualTo("Pizza");
            assertThat(saved.getKitchenNote()).isEqualTo("extra cheese");
        }

        @Test
        @DisplayName("should set null selectedOptions to empty list")
        void shouldSetNullSelectedOptionsToEmptyList() throws Exception {
            String key = CART_KEY_PREFIX + 1L;
            CartItemPayload item = CartItemPayload.builder()
                    .productId("p1")
                    .productName("Pizza")
                    .unitPrice(new BigDecimal("10.00"))
                    .quantity(1)
                    .selectedOptions(null)
                    .build();

            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(86400L);

            CartResponse response = cartService.patchCart(1L, List.of(item));

            assertThat(response.getItems().get(0).getSelectedOptions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should throw IllegalStateException when serialization fails")
        void shouldThrowWhenSerializationFails() throws Exception {
            List<CartItemPayload> items = List.of(sampleItem("p1", 1));
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("fail") {});

            assertThatThrownBy(() -> cartService.patchCart(1L, items))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("serialize");
        }
    }

    // =====================================================================
    // clearCart
    // =====================================================================

    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("should delete the Redis key")
        void shouldDeleteRedisKey() {
            String key = CART_KEY_PREFIX + 1L;

            cartService.clearCart(1L);

            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("should construct correct key for different user IDs")
        void shouldConstructCorrectKeyForDifferentUserIds() {
            cartService.clearCart(999L);

            verify(redisTemplate).delete(CART_KEY_PREFIX + 999L);
        }
    }

    // =====================================================================
    // TTL clamping
    // =====================================================================

    @Nested
    @DisplayName("TTL clamping")
    class TtlClamping {

        @Test
        @DisplayName("should clamp TTL to 24h minimum when configured below")
        void shouldClampToMinimumTtl() throws Exception {
            ReflectionTestUtils.setField(cartService, "configuredTtlHours", 1L);
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = List.of(sampleItem("p1", 1));

            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(86400L);

            cartService.patchCart(1L, items);

            verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofHours(24)));
        }

        @Test
        @DisplayName("should clamp TTL to 72h maximum when configured above")
        void shouldClampToMaximumTtl() throws Exception {
            ReflectionTestUtils.setField(cartService, "configuredTtlHours", 100L);
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = List.of(sampleItem("p1", 1));

            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(259200L);

            cartService.patchCart(1L, items);

            verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofDays(3)));
        }

        @Test
        @DisplayName("should use configured TTL when within bounds")
        void shouldUseConfiguredTtlWhenWithinBounds() throws Exception {
            ReflectionTestUtils.setField(cartService, "configuredTtlHours", 48L);
            String key = CART_KEY_PREFIX + 1L;
            List<CartItemPayload> items = List.of(sampleItem("p1", 1));

            when(objectMapper.writeValueAsString(any())).thenReturn("[]");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(172800L);

            cartService.patchCart(1L, items);

            verify(valueOperations).set(eq(key), anyString(), eq(Duration.ofHours(48)));
        }
    }
}
