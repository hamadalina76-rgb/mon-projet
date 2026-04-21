package com.speedline.delivery.dispatch.dto;

import com.speedline.delivery.dispatch.contract.model.CourierType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourierPositionView {
    private Long courierId;
    private Long zoneId;
    private CourierType type;
    private String status;
    private Double lat;
    private Double lon;
    private boolean online;
}
