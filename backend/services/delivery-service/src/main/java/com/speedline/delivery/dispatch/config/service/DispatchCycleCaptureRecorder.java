package com.speedline.delivery.dispatch.config.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.persistence.DispatchCycleCaptureEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchCycleCaptureRepository;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.matching.cost.service.DispatchConfigRuntimeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchCycleCaptureRecorder {

    private final DispatchCycleCaptureRepository captureRepository;
    private final ObjectMapper objectMapper;
    private final DispatchConfigRuntimeWriter runtimeWriter;

    public void captureSnapshot(Long zoneId, List<PendingOrder> pending, List<AvailableCourier> couriers, boolean poolInternalOnly) {
        if (zoneId == null || pending == null || couriers == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                DispatchCycleCaptureEntity e = new DispatchCycleCaptureEntity();
                e.setZoneId(zoneId);
                e.setPoolInternalOnly(poolInternalOnly);
                e.setConfigVersion(runtimeWriter.readPublishedVersion());
                e.setPendingOrdersJson(objectMapper.writeValueAsString(pending));
                e.setCouriersJson(objectMapper.writeValueAsString(couriers));
                captureRepository.save(e);
            } catch (Exception ex) {
                log.warn("Cycle capture failed zoneId={}: {}", zoneId, ex.getMessage());
            }
        });
    }

    public List<PendingOrder> readPending(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    public List<AvailableCourier> readCouriers(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }
}
