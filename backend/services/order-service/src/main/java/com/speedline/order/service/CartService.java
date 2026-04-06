package com.speedline.order.service;

import com.speedline.order.dto.cart.CartItemPayload;
import com.speedline.order.dto.cart.CartResponse;

import java.util.List;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse patchCart(Long userId, List<CartItemPayload> items);

    void clearCart(Long userId);
}
