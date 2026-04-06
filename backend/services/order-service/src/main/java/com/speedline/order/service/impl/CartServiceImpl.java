package com.speedline.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.order.dto.cart.CartItemPayload;
import com.speedline.order.dto.cart.CartResponse;
import com.speedline.order.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "cart:user:";
    private static final Duration MIN_TTL = Duration.ofHours(24);
    private static final Duration MAX_TTL = Duration.ofDays(3);

    private static final TypeReference<List<CartItemPayload>> CART_ITEMS_TYPE =
            new TypeReference<List<CartItemPayload>>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cart.redis.ttl-hours:24}")
    private long configuredTtlHours;

    @Override
    public CartResponse getCart(Long userId) {
        final String key = redisKey(userId);
        final String raw = redisTemplate.opsForValue().get(key);

        final List<CartItemPayload> items = deserializeItems(raw, key);
        final Long ttlSeconds = normalizeTtl(redisTemplate.getExpire(key, TimeUnit.SECONDS));

        return CartResponse.builder()
                .items(items)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    @Override
    public CartResponse patchCart(Long userId, List<CartItemPayload> items) {
        final String key = redisKey(userId);
        final List<CartItemPayload> sanitizedItems = sanitizeItems(items);

        if (sanitizedItems.isEmpty()) {
            redisTemplate.delete(key);
            return CartResponse.builder()
                    .items(List.of())
                    .ttlSeconds(0L)
                    .build();
        }

        final Duration ttl = resolveEffectiveTtl();
        final String payload = serializeItems(sanitizedItems);

        redisTemplate.opsForValue().set(key, payload, ttl);

        final Long ttlSeconds = normalizeTtl(redisTemplate.getExpire(key, TimeUnit.SECONDS));

        return CartResponse.builder()
                .items(sanitizedItems)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    @Override
    public void clearCart(Long userId) {
        redisTemplate.delete(redisKey(userId));
    }

    private String redisKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private Duration resolveEffectiveTtl() {
        final Duration configured = Duration.ofHours(Math.max(0, configuredTtlHours));

        if (configured.compareTo(MIN_TTL) < 0) {
            log.warn("cart.redis.ttl-hours={} is below 24h. Clamping to 24h.", configuredTtlHours);
            return MIN_TTL;
        }

        if (configured.compareTo(MAX_TTL) > 0) {
            log.warn("cart.redis.ttl-hours={} is above 72h. Clamping to 72h.", configuredTtlHours);
            return MAX_TTL;
        }

        return configured;
    }

    private List<CartItemPayload> sanitizeItems(List<CartItemPayload> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        final List<CartItemPayload> sanitized = new ArrayList<>();

        for (CartItemPayload item : items) {
            if (item == null) {
                continue;
            }

            final Integer quantity = item.getQuantity();
            if (quantity == null || quantity <= 0) {
                continue;
            }

            sanitized.add(CartItemPayload.builder()
                    .productId(trimToNull(item.getProductId()))
                    .partnerId(trimToNull(item.getPartnerId()))
                    .partnerName(trimToEmpty(item.getPartnerName()))
                    .partnerLogoUrl(trimToEmpty(item.getPartnerLogoUrl()))
                    .productName(trimToEmpty(item.getProductName()))
                    .unitPrice(item.getUnitPrice())
                    .quantity(quantity)
                    .selectedOptions(item.getSelectedOptions() == null ? List.of() : item.getSelectedOptions())
                    .kitchenNote(trimToNull(item.getKitchenNote()))
                    .build());
        }

        return List.copyOf(sanitized);
    }

    private String serializeItems(List<CartItemPayload> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize cart payload", e);
        }
    }

    private List<CartItemPayload> deserializeItems(String raw, String key) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            final List<CartItemPayload> parsed = objectMapper.readValue(raw, CART_ITEMS_TYPE);
            return parsed == null ? List.of() : List.copyOf(parsed.stream().filter(Objects::nonNull).toList());
        } catch (Exception e) {
            log.warn("Invalid cart payload in Redis for key {}. Clearing key.", key, e);
            redisTemplate.delete(key);
            return List.of();
        }
    }

    private Long normalizeTtl(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds < 0) {
            return 0L;
        }
        return ttlSeconds;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
