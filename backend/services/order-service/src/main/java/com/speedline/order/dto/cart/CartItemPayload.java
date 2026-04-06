package com.speedline.order.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemPayload {

    private String productId;
    private String partnerId;
    private String partnerName;
    private String partnerLogoUrl;

    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;

    @Builder.Default
    private List<String> selectedOptions = List.of();

    private String kitchenNote;
}
