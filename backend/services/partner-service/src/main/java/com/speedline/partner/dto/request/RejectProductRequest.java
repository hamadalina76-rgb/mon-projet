package com.speedline.partner.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body used when an admin rejects a product.
 */
@Data
public class RejectProductRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}

