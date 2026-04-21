package com.speedline.delivery.dispatch.engine.bundling;

import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class BundleCandidate {
    Long bundleId;
    PendingOrder seed;
    List<PendingOrder> members;
    Map<Long, Integer> perOrderEtaMin;
}
