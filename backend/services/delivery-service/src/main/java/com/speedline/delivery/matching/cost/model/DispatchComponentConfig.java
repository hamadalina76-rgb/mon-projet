package com.speedline.delivery.matching.cost.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DispatchComponentConfig {
    CostComponentKey key;
    boolean enabled;
    int weight;
    int order;
    boolean mandatory;
}
