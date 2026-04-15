package com.speedline.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsResponse {

    private KpiData kpis;
    private ChartData chart;
    private DistributionData distribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiData {
        private long totalOrders;
        private long activeOrders;
        private double cancelRate;
        private double avgDeliveryMinutes;
        private BigDecimal totalRevenueTND;
        // Trend: previous period values for comparison
        private long prevTotalOrders;
        private double prevCancelRate;
        private double prevAvgDeliveryMinutes;
        private BigDecimal prevRevenueTND;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private List<String> labels;
        private List<Long> newOrders;
        private List<Long> deliveredOrders;
        private List<Long> cancelledOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionData {
        private long pending;
        private long confirmed;
        private long preparing;
        private long inDelivery;
        private long delivered;
        private long cancelled;
    }
}
