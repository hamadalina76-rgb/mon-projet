package com.speedline.delivery.dispatch.impl.bundling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleRouteResponse {

    @Builder.Default
    private List<Long> orderSequence = new ArrayList<>();

    @Builder.Default
    private List<Integer> cumulativeEtaMinutes = new ArrayList<>();
}
