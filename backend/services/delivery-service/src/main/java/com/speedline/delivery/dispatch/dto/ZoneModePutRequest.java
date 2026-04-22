package com.speedline.delivery.dispatch.dto;

import com.speedline.delivery.dispatch.config.DispatchMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZoneModePutRequest {
    @NotNull
    private DispatchMode mode;
}
