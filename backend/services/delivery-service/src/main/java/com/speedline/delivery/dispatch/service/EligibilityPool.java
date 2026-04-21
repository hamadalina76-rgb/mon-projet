package com.speedline.delivery.dispatch.service;

import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import lombok.Builder;

import java.util.List;

@Builder
public record EligibilityPool(
        List<AvailableCourier> pool,
        List<AvailableCourier> internalOnly,
        boolean fallbackApplied
) {
}
