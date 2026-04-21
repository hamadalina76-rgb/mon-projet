package com.speedline.delivery.dispatch.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispatchDashboardKpisResponse {
    private double firstCycleDispatchRate;
    private double averageAssignmentDelaySeconds;
    private double deliveriesPerCourierPerHour;
    private double bundlingRate;
    private double failureRate;
    private double onTimeRate;
}
