package com.speedline.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneCourierCountDTO {
    private Long zoneId;
    private Long internalCouriersCount;
    private Long externalCouriersCount;
}
