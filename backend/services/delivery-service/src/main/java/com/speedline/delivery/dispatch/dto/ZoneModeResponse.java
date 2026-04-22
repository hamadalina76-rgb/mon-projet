package com.speedline.delivery.dispatch.dto;

import com.speedline.delivery.dispatch.config.DispatchMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneModeResponse {
    private Long zoneId;
    private DispatchMode mode;
    /** True when {@link #mode} comes from Redis override; false when from application.yml only. */
    private boolean runtimeOverride;
}
