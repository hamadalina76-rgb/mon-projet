package com.speedline.delivery.matching.cost.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@Builder
public class DispatchConfigSnapshot {

    @Singular
    List<DispatchComponentConfig> components;

    public List<DispatchComponentConfig> orderedComponents() {
        return components.stream()
                .sorted(Comparator.comparingInt(DispatchComponentConfig::getOrder))
                .toList();
    }

    public Map<CostComponentKey, DispatchComponentConfig> byKey() {
        return components.stream().collect(Collectors.toMap(DispatchComponentConfig::getKey, Function.identity()));
    }
}
