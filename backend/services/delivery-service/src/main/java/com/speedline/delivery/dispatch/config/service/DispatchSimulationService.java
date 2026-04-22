package com.speedline.delivery.dispatch.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.api.dto.DispatchConfigDtos;
import com.speedline.delivery.dispatch.config.persistence.DispatchSimulationRunEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchSimulationRunRepository;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.dispatch.service.CourierAvailabilityService;
import com.speedline.delivery.dispatch.service.PendingOrderRedisRepository;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.CostResult;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.matching.cost.service.CostFunctionService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchSimulationService {

    private final DispatchSimulationRunRepository simulationRunRepository;
    private final PendingOrderRedisRepository pendingOrderRedisRepository;
    private final CourierAvailabilityService courierAvailabilityService;
    private final CostFunctionService costFunctionService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public DispatchConfigDtos.SimulateResponse simulate(DispatchConfigDtos.SimulateRequest request,
                                                      String actorUserId, String actorEmail) {
        meterRegistry.counter("dispatch.simulation.runs").increment();
        DispatchSimulationRunEntity run = new DispatchSimulationRunEntity();
        run.setActorUserId(actorUserId);
        run.setActorEmail(actorEmail);
        run.setZoneId(request.getZoneId());
        run.setStatus("RUNNING");
        try {
            run.setOverlayJson(objectMapper.writeValueAsString(request));
        } catch (Exception ex) {
            run.setOverlayJson("{}");
        }
        simulationRunRepository.save(run);

        try {
            Long zoneId = request.getZoneId();
            List<PendingOrder> orders = zoneId == null ? List.of() : pendingOrderRedisRepository.findByZone(zoneId);
            List<AvailableCourier> couriers = zoneId == null ? List.of() : courierAvailabilityService.findOnlineByZone(zoneId);
            DispatchConfigSnapshot scoring = DispatchScoringMapper.toSnapshot(
                    request.getScoringOverlay() != null ? request.getScoringOverlay().getComponents() : List.of());

            List<DispatchConfigDtos.SimulatePairResultDto> pairs = new ArrayList<>();
            for (PendingOrder o : orders) {
                for (AvailableCourier c : couriers) {
                    ScoringContext ctx = DispatchScoringContextMapper.from(o, c);
                    CostResult r = costFunctionService.calculate(ctx, scoring);
                    pairs.add(toPair(o.getId(), c.getId(), r));
                }
            }

            DispatchConfigDtos.SimulateResponse resp = DispatchConfigDtos.SimulateResponse.builder()
                    .simulationRunId(run.getId())
                    .status("SUCCESS")
                    .pairResults(pairs)
                    .note("Simulation is read-only; production Redis pending keys were not modified.")
                    .build();
            run.setStatus("SUCCESS");
            run.setCompletedAt(Instant.now());
            run.setResultJson(objectMapper.writeValueAsString(resp));
            simulationRunRepository.save(run);
            return resp;
        } catch (Exception ex) {
            log.warn("Simulation failed: {}", ex.getMessage());
            run.setStatus("FAILED");
            run.setCompletedAt(Instant.now());
            simulationRunRepository.save(run);
            return DispatchConfigDtos.SimulateResponse.builder()
                    .simulationRunId(run.getId())
                    .status("FAILED")
                    .pairResults(List.of())
                    .note(ex.getMessage())
                    .build();
        }
    }

    private static DispatchConfigDtos.SimulatePairResultDto toPair(long orderId, long courierId, CostResult r) {
        List<DispatchConfigDtos.ComponentScoreDto> comps = new ArrayList<>();
        if (r.getContributions() != null) {
            for (CostContribution c : r.getContributions()) {
                comps.add(DispatchConfigDtos.ComponentScoreDto.builder()
                        .key(c.getKey())
                        .weightedCost(c.getWeightedCost())
                        .eliminated(c.isEliminated())
                        .detail(c.getNote())
                        .build());
            }
        }
        return DispatchConfigDtos.SimulatePairResultDto.builder()
                .orderId(orderId)
                .courierId(courierId)
                .totalCost(r.getTotalCost())
                .eliminated(r.isEliminated())
                .eliminationReason(r.getEliminationReason() != null ? r.getEliminationReason().name() : null)
                .components(comps)
                .build();
    }
}
