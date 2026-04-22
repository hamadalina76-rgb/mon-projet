package com.speedline.delivery.controller;

import com.speedline.delivery.dispatch.dto.DispatchProposalView;
import com.speedline.delivery.dispatch.dto.ManualAssignRequest;
import com.speedline.delivery.dispatch.dto.ManualBundleRequest;
import com.speedline.delivery.dispatch.dto.ZoneModePutRequest;
import com.speedline.delivery.dispatch.dto.ZoneModeResponse;
import com.speedline.delivery.dispatch.dto.ZoneStatusRequest;
import com.speedline.delivery.dispatch.service.DispatchAdminActionService;
import com.speedline.delivery.dispatch.service.DispatchDashboardQueryService;
import com.speedline.delivery.dispatch.service.DispatchProposalService;
import com.speedline.delivery.dispatch.service.DispatchRealtimePublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dispatch")
@RequiredArgsConstructor
@Validated
public class DispatchDashboardController {

    private final DispatchDashboardQueryService queryService;
    private final DispatchAdminActionService adminActionService;
    private final DispatchRealtimePublisher realtimePublisher;
    private final DispatchProposalService proposalService;

    @GetMapping("/dashboard/kpis")
    public ResponseEntity<?> getDashboardKpis() {
        return ResponseEntity.ok(queryService.getKpis());
    }

    @GetMapping("/zones/{id}/metrics")
    public ResponseEntity<?> getZoneMetrics(@PathVariable("id") Long zoneId) {
        return ResponseEntity.ok(queryService.getZoneMetrics(zoneId));
    }

    @GetMapping("/zones/{id}/mode")
    public ResponseEntity<ZoneModeResponse> getZoneMode(@PathVariable("id") Long zoneId) {
        return ResponseEntity.ok(adminActionService.getZoneMode(zoneId));
    }

    @PutMapping("/zones/{id}/mode")
    public ResponseEntity<ZoneModeResponse> putZoneMode(
            @PathVariable("id") Long zoneId, @Valid @RequestBody ZoneModePutRequest request) {
        return ResponseEntity.ok(adminActionService.setZoneMode(zoneId, request.getMode()));
    }

    @GetMapping("/zones/{id}/proposals")
    public ResponseEntity<List<DispatchProposalView>> listProposals(@PathVariable("id") Long zoneId) {
        return ResponseEntity.ok(proposalService.listForZone(zoneId));
    }

    @PostMapping("/proposals/{orderId}/approve")
    public ResponseEntity<Map<String, Object>> approveProposal(@PathVariable Long orderId) {
        return ResponseEntity.ok(proposalService.approve(orderId));
    }

    @PostMapping("/proposals/{orderId}/reject")
    public ResponseEntity<Map<String, Object>> rejectProposal(@PathVariable Long orderId) {
        return ResponseEntity.ok(proposalService.reject(orderId));
    }

    @GetMapping("/couriers/positions")
    public ResponseEntity<?> getCourierPositions() {
        return ResponseEntity.ok(queryService.getCourierPositions());
    }

    @GetMapping("/orders/pending")
    public ResponseEntity<?> getPendingOrders() {
        return ResponseEntity.ok(queryService.getPendingOrders());
    }

    @PostMapping("/manual-assign")
    public ResponseEntity<Map<String, Object>> manualAssign(@Valid @RequestBody ManualAssignRequest request) {
        return ResponseEntity.ok(adminActionService.manualAssign(request));
    }

    @PostMapping("/manual-bundle")
    public ResponseEntity<Map<String, Object>> manualBundle(@Valid @RequestBody ManualBundleRequest request) {
        return ResponseEntity.ok(adminActionService.manualBundle(request));
    }

    @PutMapping("/zones/{id}/status")
    public ResponseEntity<?> updateZoneStatus(@PathVariable("id") Long zoneId, @Valid @RequestBody ZoneStatusRequest request) {
        var response = adminActionService.updateZoneStatus(zoneId, request.getActive());
        realtimePublisher.publishZoneStatus(zoneId, request.getActive());
        return ResponseEntity.ok(response);
    }
}
