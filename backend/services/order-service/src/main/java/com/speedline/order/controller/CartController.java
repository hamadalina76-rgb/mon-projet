package com.speedline.order.controller;

import com.speedline.order.dto.cart.CartPatchRequest;
import com.speedline.order.dto.cart.CartResponse;
import com.speedline.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId
    ) {
        final Long userId = extractAuthenticatedUserId(authenticatedUserId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PatchMapping
    public ResponseEntity<CartResponse> patchCart(
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId,
            @Valid @RequestBody(required = false) CartPatchRequest request
    ) {
        final Long userId = extractAuthenticatedUserId(authenticatedUserId);
        final List<com.speedline.order.dto.cart.CartItemPayload> items =
                request == null ? List.of() : request.getItems();

        return ResponseEntity.ok(cartService.patchCart(userId, items));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId
    ) {
        final Long userId = extractAuthenticatedUserId(authenticatedUserId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
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
}
