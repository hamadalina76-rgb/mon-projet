package com.speedline.delivery.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualAssignRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private Long courierId;
}
