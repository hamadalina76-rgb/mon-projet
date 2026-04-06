package com.speedline.order.dto.cart;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPatchRequest {

    @NotNull
    @Builder.Default
    private List<CartItemPayload> items = List.of();
}
