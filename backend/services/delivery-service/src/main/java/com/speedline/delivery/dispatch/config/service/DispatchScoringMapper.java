package com.speedline.delivery.dispatch.config.service;

import com.speedline.delivery.dispatch.config.api.dto.DispatchConfigDtos;
import com.speedline.delivery.matching.cost.model.CostComponentKey;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DispatchScoringMapper {

    private DispatchScoringMapper() {
    }

    public static DispatchConfigSnapshot toSnapshot(List<DispatchConfigDtos.ScoringComponentDto> rows) {
        Map<CostComponentKey, DispatchConfigDtos.ScoringComponentDto> byKey = rows == null ? Map.of()
                : rows.stream().collect(Collectors.toMap(DispatchConfigDtos.ScoringComponentDto::getKey, Function.identity(), (a, b) -> a));

        List<DispatchComponentConfig> components = new ArrayList<>();
        for (CostComponentKey key : EnumSet.allOf(CostComponentKey.class)) {
            DispatchConfigDtos.ScoringComponentDto row = byKey.get(key);
            if (row == null) {
                components.add(defaultRow(key));
            } else {
                components.add(DispatchComponentConfig.builder()
                        .key(key)
                        .enabled(row.isEnabled())
                        .weight(Math.max(0, Math.min(10, row.getWeight())))
                        .order(row.getOrder())
                        .mandatory(isMandatory(key))
                        .build());
            }
        }
        components.sort(Comparator.comparingInt(DispatchComponentConfig::getOrder));
        return DispatchConfigSnapshot.builder().components(components).build();
    }

    private static boolean isMandatory(CostComponentKey key) {
        return key == CostComponentKey.GUARANTEED_DEADLINE || key == CostComponentKey.VEHICLE_COMPATIBILITY;
    }

    private static DispatchComponentConfig defaultRow(CostComponentKey key) {
        return DispatchComponentConfig.builder()
                .key(key)
                .enabled(true)
                .weight(10)
                .order(switch (key) {
                    case ETA_TOTAL_ESTIMATED -> 1;
                    case COURIER_TYPE -> 2;
                    case AVAILABILITY -> 3;
                    case ROUTE_ALIGNMENT -> 4;
                    case TOUR_COMPATIBILITY -> 5;
                    case PERFORMANCE_RATING -> 6;
                    case GUARANTEED_DEADLINE -> 7;
                    case RECENT_REFUSAL -> 8;
                    case WORKLOAD_FAIRNESS -> 9;
                    case MERCHANT_KNOWLEDGE -> 10;
                    case VEHICLE_COMPATIBILITY -> 11;
                })
                .mandatory(isMandatory(key))
                .build();
    }
}
