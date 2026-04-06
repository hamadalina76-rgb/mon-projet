package com.speedline.order.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    @Builder.Default
    private List<CartItemPayload> items = List.of();

    /**
     * Remaining Redis TTL in seconds. 0 means key does not exist.
     */
    @Builder.Default
    private Long ttlSeconds = 0L;
}
