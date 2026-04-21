package com.speedline.delivery.dispatch.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ManualBundleRequest {
    @NotNull
    private Long zoneId;
    @NotEmpty
    private List<Long> orderIds;
}
