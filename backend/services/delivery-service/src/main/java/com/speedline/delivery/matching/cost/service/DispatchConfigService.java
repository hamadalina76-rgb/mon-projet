package com.speedline.delivery.matching.cost.service;

import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;

public interface DispatchConfigService {
    DispatchConfigSnapshot getCurrentConfig();
}
