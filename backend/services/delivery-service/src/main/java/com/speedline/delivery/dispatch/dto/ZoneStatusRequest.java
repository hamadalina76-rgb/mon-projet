package com.speedline.delivery.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZoneStatusRequest {
    @NotNull
    private Boolean active;
}
