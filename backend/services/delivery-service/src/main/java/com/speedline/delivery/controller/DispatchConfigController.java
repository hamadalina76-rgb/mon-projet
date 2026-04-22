package com.speedline.delivery.controller;

import com.speedline.delivery.dispatch.config.api.dto.DispatchConfigDtos;
import com.speedline.delivery.dispatch.config.service.DispatchConfigAuditCsvService;
import com.speedline.delivery.dispatch.config.service.DispatchConfigManagementService;
import com.speedline.delivery.dispatch.config.service.DispatchReplayService;
import com.speedline.delivery.dispatch.config.service.DispatchSimulationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
@RequestMapping("/dispatch/dispatch-config")
@RequiredArgsConstructor
@Validated
public class DispatchConfigController {

    private final DispatchConfigManagementService managementService;
    private final DispatchSimulationService simulationService;
    private final DispatchReplayService replayService;
    private final DispatchConfigAuditCsvService auditCsvService;

    @GetMapping("/meta")
    public ResponseEntity<DispatchConfigDtos.MetaResponse> meta() {
        return ResponseEntity.ok(managementService.meta());
    }

    @GetMapping("/general")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.GeneralConfigDto>> getGeneral() {
        return ResponseEntity.ok(managementService.getGeneral());
    }

    @PutMapping("/general")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.GeneralConfigDto>> putGeneral(
            HttpServletRequest request,
            @Valid @RequestBody DispatchConfigDtos.GeneralPutRequest body) {
        return ResponseEntity.ok(managementService.putGeneral(body, actorId(request), actorEmail(request)));
    }

    @GetMapping("/scoring")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ScoringConfigDto>> getScoring() {
        return ResponseEntity.ok(managementService.getScoring());
    }

    @PutMapping("/scoring")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ScoringConfigDto>> putScoring(
            HttpServletRequest request,
            @Valid @RequestBody DispatchConfigDtos.ScoringPutRequest body) {
        return ResponseEntity.ok(managementService.putScoring(body, actorId(request), actorEmail(request)));
    }

    @GetMapping("/internal-external")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.InternalExternalConfigDto>> getInternalExternal() {
        return ResponseEntity.ok(managementService.getInternalExternal());
    }

    @PutMapping("/internal-external")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.InternalExternalConfigDto>> putInternalExternal(
            HttpServletRequest request,
            @Valid @RequestBody DispatchConfigDtos.InternalExternalPutRequest body) {
        return ResponseEntity.ok(managementService.putInternalExternal(body, actorId(request), actorEmail(request)));
    }

    @GetMapping("/bundling")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.BundlingConfigDto>> getBundling() {
        return ResponseEntity.ok(managementService.getBundling());
    }

    @PutMapping("/bundling")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.BundlingConfigDto>> putBundling(
            HttpServletRequest request,
            @Valid @RequestBody DispatchConfigDtos.BundlingPutRequest body) {
        return ResponseEntity.ok(managementService.putBundling(body, actorId(request), actorEmail(request)));
    }

    @GetMapping("/exclusivity")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ExclusivityConfigDto>> getExclusivity() {
        return ResponseEntity.ok(managementService.getExclusivity());
    }

    @PutMapping("/exclusivity")
    public ResponseEntity<DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ExclusivityConfigDto>> putExclusivity(
            HttpServletRequest request,
            @Valid @RequestBody DispatchConfigDtos.ExclusivityPutRequest body) {
        return ResponseEntity.ok(managementService.putExclusivity(body, actorId(request), actorEmail(request)));
    }

    @PostMapping("/simulate")
    public ResponseEntity<DispatchConfigDtos.SimulateResponse> simulate(
            HttpServletRequest request,
            @Valid @RequestBody DispatchConfigDtos.SimulateRequest body) {
        return ResponseEntity.ok(simulationService.simulate(body, actorId(request), actorEmail(request)));
    }

    @GetMapping("/replay/{cycleId}")
    public ResponseEntity<DispatchConfigDtos.ReplayResponse> replay(
            HttpServletRequest request,
            @PathVariable("cycleId") String cycleId) {
        return ResponseEntity.ok(replayService.replay(cycleId, actorId(request)));
    }

    @GetMapping(value = "/audit/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportAudit(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromUtc,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toUtcExclusive) {
        StreamingResponseBody body = outputStream -> {
            Writer w = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            auditCsvService.export(fromUtc, toUtcExclusive, w);
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dispatch-config-audit.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private static String actorId(HttpServletRequest request) {
        String v = request.getHeader("X-User-Id");
        return v == null ? "" : v;
    }

    private static String actorEmail(HttpServletRequest request) {
        String v = request.getHeader("X-User-Email");
        return v == null ? "" : v;
    }
}
