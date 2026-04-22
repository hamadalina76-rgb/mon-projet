package com.speedline.delivery.dispatch.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.config.api.dto.DispatchConfigDtos;
import com.speedline.delivery.dispatch.config.persistence.DispatchCycleCaptureEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchCycleCaptureRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchReplayPairScoreEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchReplayPairScoreRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchReplaySessionEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchReplaySessionRepository;
import com.speedline.delivery.dispatch.contract.model.AvailableCourier;
import com.speedline.delivery.dispatch.contract.model.PendingOrder;
import com.speedline.delivery.matching.cost.model.CostContribution;
import com.speedline.delivery.matching.cost.model.CostResult;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.model.ScoringContext;
import com.speedline.delivery.matching.cost.service.CostFunctionService;
import com.speedline.delivery.matching.cost.service.DispatchConfigService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchReplayService {

    private final DispatchCycleCaptureRepository cycleCaptureRepository;
    private final DispatchReplaySessionRepository replaySessionRepository;
    private final DispatchReplayPairScoreRepository pairScoreRepository;
    private final DispatchCycleCaptureRecorder captureRecorder;
    private final CostFunctionService costFunctionService;
    private final DispatchConfigService dispatchConfigService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public DispatchConfigDtos.ReplayResponse replay(String cycleId, String actorUserId) {
        meterRegistry.counter("dispatch.replay.runs").increment();
        UUID uuid;
        try {
            uuid = UUID.fromString(cycleId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid cycleId");
        }
        DispatchCycleCaptureEntity cap = cycleCaptureRepository.findByExternalCycleId(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cycle not found"));
        try {
            List<PendingOrder> orders = captureRecorder.readPending(cap.getPendingOrdersJson());
            List<AvailableCourier> couriers = captureRecorder.readCouriers(cap.getCouriersJson());
            DispatchConfigSnapshot scoring = dispatchConfigService.getCurrentConfig();

            DispatchReplaySessionEntity session = new DispatchReplaySessionEntity();
            session.setCycleCapture(cap);
            session.setActorUserId(actorUserId);
            session.setPairCount(orders.size() * Math.max(1, couriers.size()));
            replaySessionRepository.save(session);

            List<DispatchConfigDtos.SimulatePairResultDto> pairs = new ArrayList<>();
            for (PendingOrder o : orders) {
                for (AvailableCourier c : couriers) {
                    ScoringContext ctx = DispatchScoringContextMapper.from(o, c);
                    CostResult r = costFunctionService.calculate(ctx, scoring);
                    DispatchConfigDtos.SimulatePairResultDto dto = toPair(o.getId(), c.getId(), r);
                    pairs.add(dto);

                    DispatchReplayPairScoreEntity row = new DispatchReplayPairScoreEntity();
                    row.setReplaySession(session);
                    row.setOrderId(o.getId());
                    row.setCourierId(c.getId());
                    row.setTotalCost(r.getTotalCost());
                    row.setEliminated(r.isEliminated());
                    row.setEliminationReason(r.getEliminationReason() != null ? r.getEliminationReason().name() : null);
                    row.setComponentsJson(objectMapper.writeValueAsString(dto.getComponents()));
                    pairScoreRepository.save(row);
                }
            }
            session.setPairCount(pairs.size());
            replaySessionRepository.save(session);

            return DispatchConfigDtos.ReplayResponse.builder()
                    .cycleId(cycleId)
                    .replaySessionId(session.getId())
                    .pairResults(pairs)
                    .build();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Replay failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "replay failed", ex);
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
