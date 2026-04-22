package com.speedline.delivery.dispatch.config.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.speedline.delivery.dispatch.config.DispatchProperties;
import com.speedline.delivery.dispatch.config.api.dto.DispatchConfigDtos;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigActiveEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigActiveRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigAuditEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigAuditRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigSnapshotEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigSnapshotRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigVersionEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigVersionRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchExclusivityCellEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchExclusivityCellRepository;
import com.speedline.delivery.dispatch.config.persistence.DispatchExclusivityPartnerOverrideEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchExclusivityPartnerOverrideRepository;
import com.speedline.delivery.matching.cost.model.DispatchComponentConfig;
import com.speedline.delivery.matching.cost.model.DispatchConfigSnapshot;
import com.speedline.delivery.matching.cost.service.impl.RedisDispatchConfigService;
import com.speedline.delivery.matching.cost.service.DispatchConfigRuntimeWriter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatchConfigManagementService {

    private final DispatchConfigActiveRepository activeRepository;
    private final DispatchConfigSnapshotRepository snapshotRepository;
    private final DispatchConfigVersionRepository versionRepository;
    private final DispatchConfigAuditRepository auditRepository;
    private final DispatchExclusivityCellRepository exclusivityCellRepository;
    private final DispatchExclusivityPartnerOverrideRepository exclusivityPartnerOverrideRepository;
    private final ObjectMapper objectMapper;
    private final DispatchProperties dispatchProperties;
    private final RedisDispatchConfigService redisDispatchConfigService;
    private final DispatchConfigRuntimeWriter runtimeWriter;
    private final MeterRegistry meterRegistry;

    private void recordConfigUpdate(String group) {
        meterRegistry.counter("dispatch.config.updates", "group", group).increment();
    }

    public DispatchConfigDtos.MetaResponse meta() {
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        DispatchConfigVersionEntity ver = versionRepository.findByVersion(active.getVersion()).orElseThrow();
        return meta(active.getVersion(), ver.getOptimisticLock(), runtimeWriter.readPublishedVersion());
    }

    private static DispatchConfigDtos.MetaResponse meta(long activeVersion, int optimisticLock, long redisVersion) {
        DispatchConfigDtos.MetaResponse m = new DispatchConfigDtos.MetaResponse();
        m.setActiveVersion(activeVersion);
        m.setOptimisticLock(optimisticLock);
        m.setRedisConfigVersion(redisVersion);
        return m;
    }

    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.GeneralConfigDto> getGeneral() {
        DispatchConfigSnapshotEntity snap = currentSnapshot();
        try {
            DispatchConfigDtos.GeneralConfigDto dto = readGeneral(snap.getGeneralJson());
            DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
            DispatchConfigVersionEntity ver = versionRepository.findByVersion(active.getVersion()).orElseThrow();
            return envelope(meta(active.getVersion(), ver.getOptimisticLock(), runtimeWriter.readPublishedVersion()), dto);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "config read failed", ex);
        }
    }

    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ScoringConfigDto> getScoring() {
        DispatchConfigSnapshotEntity snap = currentSnapshot();
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        DispatchConfigVersionEntity ver = versionRepository.findByVersion(active.getVersion()).orElseThrow();
        try {
            DispatchConfigDtos.ScoringConfigDto dto = readScoring(snap.getScoringJson());
            return envelope(meta(active.getVersion(), ver.getOptimisticLock(), runtimeWriter.readPublishedVersion()), dto);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "config read failed", ex);
        }
    }

    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.InternalExternalConfigDto> getInternalExternal() {
        DispatchConfigSnapshotEntity snap = currentSnapshot();
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        DispatchConfigVersionEntity ver = versionRepository.findByVersion(active.getVersion()).orElseThrow();
        try {
            DispatchConfigDtos.InternalExternalConfigDto dto =
                    objectMapper.readValue(snap.getInternalExternalJson(), DispatchConfigDtos.InternalExternalConfigDto.class);
            return envelope(meta(active.getVersion(), ver.getOptimisticLock(), runtimeWriter.readPublishedVersion()), dto);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "config read failed", ex);
        }
    }

    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.BundlingConfigDto> getBundling() {
        DispatchConfigSnapshotEntity snap = currentSnapshot();
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        DispatchConfigVersionEntity ver = versionRepository.findByVersion(active.getVersion()).orElseThrow();
        try {
            DispatchConfigDtos.BundlingConfigDto dto =
                    objectMapper.readValue(snap.getBundlingJson(), DispatchConfigDtos.BundlingConfigDto.class);
            return envelope(meta(active.getVersion(), ver.getOptimisticLock(), runtimeWriter.readPublishedVersion()), dto);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "config read failed", ex);
        }
    }

    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ExclusivityConfigDto> getExclusivity() {
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        DispatchConfigVersionEntity ver = versionRepository.findByVersion(active.getVersion()).orElseThrow();
        List<DispatchConfigDtos.ExclusivityCellDto> cells = exclusivityCellRepository.findAll().stream().map(c -> {
            DispatchConfigDtos.ExclusivityCellDto d = new DispatchConfigDtos.ExclusivityCellDto();
            d.setZoneId(c.getZoneId());
            d.setCommerceType(c.getCommerceType());
            d.setAllowed(c.isAllowed());
            return d;
        }).toList();
        List<DispatchConfigDtos.ExclusivityPartnerOverrideDto> po = exclusivityPartnerOverrideRepository.findAll().stream().map(o -> {
            DispatchConfigDtos.ExclusivityPartnerOverrideDto d = new DispatchConfigDtos.ExclusivityPartnerOverrideDto();
            d.setPartnerId(o.getPartnerId());
            d.setZoneId(o.getZoneId());
            d.setCommerceType(o.getCommerceType());
            d.setAllowed(o.isAllowed());
            return d;
        }).toList();
        DispatchConfigDtos.ExclusivityConfigDto dto = new DispatchConfigDtos.ExclusivityConfigDto();
        dto.setCells(cells);
        dto.setPartnerOverrides(po);
        return envelope(meta(active.getVersion(), ver.getOptimisticLock(), runtimeWriter.readPublishedVersion()), dto);
    }

    @Transactional
    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.GeneralConfigDto> putGeneral(
            DispatchConfigDtos.GeneralPutRequest req, String actorUserId, String actorEmail) {
        assertVersion(req.getBaseVersion());
        DispatchConfigSnapshotEntity cur = currentSnapshot();
        try {
            DispatchConfigDtos.GeneralConfigDto merged = mergeGeneral(cur.getGeneralJson(), req.getData());
            String generalJson = objectMapper.writeValueAsString(merged);
            long nv = persistNewVersion(cur, generalJson, cur.getScoringJson(), cur.getInternalExternalJson(),
                    cur.getBundlingJson(), cur.getExclusivityJson(), actorUserId, actorEmail,
                    "GENERAL", "/dispatch/dispatch-config/general", "general updated");
            publishFull(nv, cur.getScoringJson(), generalJson, cur.getInternalExternalJson(), cur.getBundlingJson(), cur.getExclusivityJson());
            recordConfigUpdate("general");
            return getGeneral();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "save failed", ex);
        }
    }

    @Transactional
    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ScoringConfigDto> putScoring(
            DispatchConfigDtos.ScoringPutRequest req, String actorUserId, String actorEmail) {
        assertVersion(req.getBaseVersion());
        DispatchConfigSnapshotEntity cur = currentSnapshot();
        try {
            DispatchConfigSnapshot snap = DispatchScoringMapper.toSnapshot(req.getData().getComponents());
            String scoringJson = buildScoringJson(req.getData(), snap);
            long nv = persistNewVersion(cur, cur.getGeneralJson(), scoringJson, cur.getInternalExternalJson(),
                    cur.getBundlingJson(), cur.getExclusivityJson(), actorUserId, actorEmail,
                    "SCORING", "/dispatch/dispatch-config/scoring", "scoring updated");
            String componentsArray = scoringArrayJson(snap);
            publishFull(nv, scoringJson, cur.getGeneralJson(), cur.getInternalExternalJson(), cur.getBundlingJson(), cur.getExclusivityJson(), componentsArray);
            recordConfigUpdate("scoring");
            return getScoring();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "save failed", ex);
        }
    }

    @Transactional
    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.InternalExternalConfigDto> putInternalExternal(
            DispatchConfigDtos.InternalExternalPutRequest req, String actorUserId, String actorEmail) {
        assertVersion(req.getBaseVersion());
        DispatchConfigSnapshotEntity cur = currentSnapshot();
        try {
            String json = objectMapper.writeValueAsString(req.getData());
            long nv = persistNewVersion(cur, cur.getGeneralJson(), cur.getScoringJson(), json,
                    cur.getBundlingJson(), cur.getExclusivityJson(), actorUserId, actorEmail,
                    "INTERNAL_EXTERNAL", "/dispatch/dispatch-config/internal-external", "internal/external updated");
            publishFull(nv, cur.getScoringJson(), cur.getGeneralJson(), json, cur.getBundlingJson(), cur.getExclusivityJson());
            recordConfigUpdate("internal-external");
            return getInternalExternal();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "save failed", ex);
        }
    }

    @Transactional
    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.BundlingConfigDto> putBundling(
            DispatchConfigDtos.BundlingPutRequest req, String actorUserId, String actorEmail) {
        assertVersion(req.getBaseVersion());
        DispatchConfigSnapshotEntity cur = currentSnapshot();
        try {
            DispatchConfigDtos.BundlingConfigDto merged = mergeBundling(req.getData());
            String bundlingJson = objectMapper.writeValueAsString(merged);
            long nv = persistNewVersion(cur, cur.getGeneralJson(), cur.getScoringJson(), cur.getInternalExternalJson(),
                    bundlingJson, cur.getExclusivityJson(), actorUserId, actorEmail,
                    "BUNDLING", "/dispatch/dispatch-config/bundling", "bundling updated");
            publishFull(nv, cur.getScoringJson(), cur.getGeneralJson(), cur.getInternalExternalJson(), bundlingJson, cur.getExclusivityJson());
            recordConfigUpdate("bundling");
            return getBundling();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "save failed", ex);
        }
    }

    @Transactional
    public DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ExclusivityConfigDto> putExclusivity(
            DispatchConfigDtos.ExclusivityPutRequest req, String actorUserId, String actorEmail) {
        assertVersion(req.getBaseVersion());
        exclusivityCellRepository.deleteAll();
        exclusivityPartnerOverrideRepository.deleteAll();
        if (req.getData().getCells() != null) {
            for (DispatchConfigDtos.ExclusivityCellDto c : req.getData().getCells()) {
                DispatchExclusivityCellEntity e = new DispatchExclusivityCellEntity();
                e.setZoneId(c.getZoneId());
                e.setCommerceType(c.getCommerceType());
                e.setAllowed(c.isAllowed());
                exclusivityCellRepository.save(e);
            }
        }
        if (req.getData().getPartnerOverrides() != null) {
            for (DispatchConfigDtos.ExclusivityPartnerOverrideDto o : req.getData().getPartnerOverrides()) {
                DispatchExclusivityPartnerOverrideEntity e = new DispatchExclusivityPartnerOverrideEntity();
                e.setPartnerId(o.getPartnerId());
                e.setZoneId(o.getZoneId());
                e.setCommerceType(o.getCommerceType());
                e.setAllowed(o.isAllowed());
                exclusivityPartnerOverrideRepository.save(e);
            }
        }
        DispatchConfigSnapshotEntity cur = currentSnapshot();
        try {
            String exclJson = objectMapper.writeValueAsString(req.getData());
            long nv = persistNewVersion(cur, cur.getGeneralJson(), cur.getScoringJson(), cur.getInternalExternalJson(),
                    cur.getBundlingJson(), exclJson, actorUserId, actorEmail,
                    "EXCLUSIVITY", "/dispatch/dispatch-config/exclusivity", "exclusivity matrix updated");
            publishFull(nv, cur.getScoringJson(), cur.getGeneralJson(), cur.getInternalExternalJson(), cur.getBundlingJson(), exclJson);
            recordConfigUpdate("exclusivity");
            return getExclusivity();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "save failed", ex);
        }
    }

    private void publishFull(long nv, String scoringJsonColumn, String generalJson, String ieJson, String bundlingJson, String exclusivityJson) throws Exception {
        DispatchConfigSnapshot snap = parseScoringColumn(scoringJsonColumn);
        publishFull(nv, scoringJsonColumn, generalJson, ieJson, bundlingJson, exclusivityJson, scoringArrayJson(snap));
    }

    private void publishFull(long nv, String scoringJsonColumn, String generalJson, String ieJson, String bundlingJson,
                             String exclusivityJson, String componentsArrayOverride) throws Exception {
        String mergedGeneral = mergeGeneralJsonForRedis(generalJson);
        String mergedBundling = mergeBundlingJsonForRedis(bundlingJson);
        runtimeWriter.publishAtomic(nv, componentsArrayOverride, mergedGeneral, ieJson, mergedBundling, exclusivityJson);
    }

    private String mergeGeneralJsonForRedis(String generalJson) throws Exception {
        DispatchConfigDtos.GeneralConfigDto dto = readGeneral(generalJson);
        ObjectNode n = objectMapper.valueToTree(dto);
        if (!n.has("intervalSeconds") || n.get("intervalSeconds").isNull()) {
            n.put("intervalSeconds", dispatchProperties.getIntervalSeconds());
        }
        if (!n.has("responseTimeoutSeconds") || n.get("responseTimeoutSeconds").isNull()) {
            n.put("responseTimeoutSeconds", dispatchProperties.getResponseTimeout().getDeadlineSeconds());
        }
        if (!n.has("lockTtlSeconds") || n.get("lockTtlSeconds").isNull()) {
            n.put("lockTtlSeconds", dispatchProperties.getLock().getTtlSeconds());
        }
        return objectMapper.writeValueAsString(n);
    }

    private String mergeBundlingJsonForRedis(String bundlingJson) throws Exception {
        if (bundlingJson == null || bundlingJson.isBlank() || bundlingJson.equals("{}")) {
            return objectMapper.writeValueAsString(dispatchProperties.getBundling());
        }
        DispatchProperties.Bundling yaml = dispatchProperties.getBundling();
        DispatchConfigDtos.BundlingConfigDto dto = objectMapper.readValue(bundlingJson, DispatchConfigDtos.BundlingConfigDto.class);
        DispatchProperties.Bundling merged = objectMapper.readValue(objectMapper.writeValueAsString(yaml), DispatchProperties.Bundling.class);
        if (dto.getEnabled() != null) {
            merged.setEnabled(dto.getEnabled());
        }
        if (dto.getMaxBundleSize() != null) {
            merged.setMaxBundleSize(dto.getMaxBundleSize());
        }
        if (dto.getDropoffRadiusMeters() != null) {
            merged.setDropoffRadiusMeters(dto.getDropoffRadiusMeters());
        }
        if (dto.getMerchantRadiusMeters() != null) {
            merged.setMerchantRadiusMeters(dto.getMerchantRadiusMeters());
        }
        if (dto.getTimeWindowSeconds() != null) {
            merged.setTimeWindowSeconds(dto.getTimeWindowSeconds());
        }
        if (dto.getInternalOnly() != null) {
            merged.setInternalOnly(dto.getInternalOnly());
        }
        if (dto.getAverageSpeedKmh() != null) {
            merged.setAverageSpeedKmh(dto.getAverageSpeedKmh());
        }
        if (dto.getRespectUrgentFlag() != null) {
            merged.setRespectUrgentFlag(dto.getRespectUrgentFlag());
        }
        if (dto.getAssignmentStrategy() != null) {
            merged.setAssignmentStrategy(dto.getAssignmentStrategy());
        }
        if (merged.getCompensation() != null && dto.getCompensationEnabled() != null) {
            merged.getCompensation().setEnabled(dto.getCompensationEnabled());
        }
        if (merged.getCompensation() != null && dto.getCompensationThresholdMinutes() != null) {
            merged.getCompensation().setThresholdMinutes(dto.getCompensationThresholdMinutes());
        }
        if (merged.getCompensation() != null && dto.getCompensationPercentOfTotal() != null) {
            merged.getCompensation().setPercentOfTotal(dto.getCompensationPercentOfTotal());
        }
        if (merged.getCompensation() != null && dto.getCompensationCurrency() != null) {
            merged.getCompensation().setCurrency(dto.getCompensationCurrency());
        }
        return objectMapper.writeValueAsString(merged);
    }

    private DispatchConfigDtos.GeneralConfigDto readGeneral(String json) throws Exception {
        return mergeGeneral(json == null || json.isBlank() ? "{}" : json, new DispatchConfigDtos.GeneralConfigDto());
    }

    private DispatchConfigDtos.GeneralConfigDto mergeGeneral(String currentJson, DispatchConfigDtos.GeneralConfigDto patch) throws Exception {
        DispatchConfigDtos.GeneralConfigDto base = objectMapper.readValue(
                currentJson == null || currentJson.isBlank() ? "{}" : currentJson, DispatchConfigDtos.GeneralConfigDto.class);
        if (patch.getIntervalSeconds() != null) {
            base.setIntervalSeconds(patch.getIntervalSeconds());
        }
        if (patch.getResponseTimeoutSeconds() != null) {
            base.setResponseTimeoutSeconds(patch.getResponseTimeoutSeconds());
        }
        if (patch.getLockTtlSeconds() != null) {
            base.setLockTtlSeconds(patch.getLockTtlSeconds());
        }
        if (patch.getPreAssignmentFinishWindowSeconds() != null) {
            base.setPreAssignmentFinishWindowSeconds(patch.getPreAssignmentFinishWindowSeconds());
        }
        if (patch.getPreAssignmentCostPenalty() != null) {
            base.setPreAssignmentCostPenalty(patch.getPreAssignmentCostPenalty());
        }
        if (patch.getEligibilityOrderWaitingThresholdSeconds() != null) {
            base.setEligibilityOrderWaitingThresholdSeconds(patch.getEligibilityOrderWaitingThresholdSeconds());
        }
        if (patch.getEligibilityInternalShortageThreshold() != null) {
            base.setEligibilityInternalShortageThreshold(patch.getEligibilityInternalShortageThreshold());
        }
        if (base.getIntervalSeconds() == null) {
            base.setIntervalSeconds(dispatchProperties.getIntervalSeconds());
        }
        if (base.getResponseTimeoutSeconds() == null) {
            base.setResponseTimeoutSeconds(dispatchProperties.getResponseTimeout().getDeadlineSeconds());
        }
        if (base.getLockTtlSeconds() == null) {
            base.setLockTtlSeconds(dispatchProperties.getLock().getTtlSeconds());
        }
        if (base.getPreAssignmentFinishWindowSeconds() == null) {
            base.setPreAssignmentFinishWindowSeconds(dispatchProperties.getPreAssignment().getFinishWindowSeconds());
        }
        if (base.getPreAssignmentCostPenalty() == null) {
            base.setPreAssignmentCostPenalty(dispatchProperties.getPreAssignment().getCostPenalty());
        }
        if (base.getEligibilityOrderWaitingThresholdSeconds() == null) {
            base.setEligibilityOrderWaitingThresholdSeconds(dispatchProperties.getEligibility().getOrderWaitingThresholdSeconds());
        }
        if (base.getEligibilityInternalShortageThreshold() == null) {
            base.setEligibilityInternalShortageThreshold(dispatchProperties.getEligibility().getInternalShortageThreshold());
        }
        return base;
    }

    private DispatchConfigDtos.BundlingConfigDto mergeBundling(DispatchConfigDtos.BundlingConfigDto patch) throws Exception {
        String yaml = objectMapper.writeValueAsString(dispatchProperties.getBundling());
        DispatchConfigDtos.BundlingConfigDto base = objectMapper.readValue(yaml, DispatchConfigDtos.BundlingConfigDto.class);
        if (patch.getEnabled() != null) {
            base.setEnabled(patch.getEnabled());
        }
        if (patch.getMaxBundleSize() != null) {
            base.setMaxBundleSize(patch.getMaxBundleSize());
        }
        if (patch.getDropoffRadiusMeters() != null) {
            base.setDropoffRadiusMeters(patch.getDropoffRadiusMeters());
        }
        if (patch.getMerchantRadiusMeters() != null) {
            base.setMerchantRadiusMeters(patch.getMerchantRadiusMeters());
        }
        if (patch.getTimeWindowSeconds() != null) {
            base.setTimeWindowSeconds(patch.getTimeWindowSeconds());
        }
        if (patch.getInternalOnly() != null) {
            base.setInternalOnly(patch.getInternalOnly());
        }
        if (patch.getAverageSpeedKmh() != null) {
            base.setAverageSpeedKmh(patch.getAverageSpeedKmh());
        }
        if (patch.getRespectUrgentFlag() != null) {
            base.setRespectUrgentFlag(patch.getRespectUrgentFlag());
        }
        if (patch.getAssignmentStrategy() != null) {
            base.setAssignmentStrategy(patch.getAssignmentStrategy());
        }
        if (patch.getCompensationEnabled() != null) {
            base.setCompensationEnabled(patch.getCompensationEnabled());
        }
        if (patch.getCompensationThresholdMinutes() != null) {
            base.setCompensationThresholdMinutes(patch.getCompensationThresholdMinutes());
        }
        if (patch.getCompensationPercentOfTotal() != null) {
            base.setCompensationPercentOfTotal(patch.getCompensationPercentOfTotal());
        }
        if (patch.getCompensationCurrency() != null) {
            base.setCompensationCurrency(patch.getCompensationCurrency());
        }
        return base;
    }

    private DispatchConfigDtos.ScoringConfigDto readScoring(String json) throws Exception {
        DispatchConfigSnapshot snap = parseScoringColumn(json);
        List<DispatchConfigDtos.ScoringComponentDto> rows = new ArrayList<>();
        for (DispatchComponentConfig c : snap.getComponents()) {
            DispatchConfigDtos.ScoringComponentDto d = new DispatchConfigDtos.ScoringComponentDto();
            d.setKey(c.getKey());
            d.setEnabled(c.isEnabled());
            d.setWeight(c.getWeight());
            d.setOrder(c.getOrder());
            d.setMandatory(c.isMandatory());
            rows.add(d);
        }
        DispatchConfigDtos.ScoringConfigDto dto = new DispatchConfigDtos.ScoringConfigDto();
        dto.setComponents(rows);
        dto.setLegacyComponents(rows);
        return dto;
    }

    private DispatchConfigSnapshot parseScoringColumn(String scoringJson) throws Exception {
        if (scoringJson == null || scoringJson.isBlank() || scoringJson.equals("{}")) {
            return redisDispatchConfigService.parseComponentsArray("[]");
        }
        JsonNode root = objectMapper.readTree(scoringJson);
        JsonNode arr = root.has("components") ? root.get("components") : root;
        if (arr.isArray()) {
            return redisDispatchConfigService.parseComponentsArray(objectMapper.writeValueAsString(arr));
        }
        return redisDispatchConfigService.parseComponentsArray("[]");
    }

    private String buildScoringJson(DispatchConfigDtos.ScoringConfigDto ui, DispatchConfigSnapshot snap) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode components = objectMapper.createArrayNode();
        for (DispatchComponentConfig c : snap.getComponents()) {
            ObjectNode o = objectMapper.createObjectNode();
            o.put("key", c.getKey().name());
            o.put("enabled", c.isEnabled());
            o.put("weight", c.getWeight());
            o.put("order", c.getOrder());
            o.put("mandatory", c.isMandatory());
            components.add(o);
        }
        root.set("components", components);
        root.set("legacyComponents", components.deepCopy());
        return objectMapper.writeValueAsString(root);
    }

    private String scoringArrayJson(DispatchConfigSnapshot snap) throws Exception {
        ArrayNode arr = objectMapper.createArrayNode();
        for (DispatchComponentConfig c : snap.getComponents()) {
            ObjectNode o = objectMapper.createObjectNode();
            o.put("key", c.getKey().name());
            o.put("enabled", c.isEnabled());
            o.put("weight", c.getWeight());
            o.put("order", c.getOrder());
            o.put("mandatory", c.isMandatory());
            arr.add(o);
        }
        return objectMapper.writeValueAsString(arr);
    }

    private void assertVersion(Long baseVersion) {
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        if (!active.getVersion().equals(baseVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "baseVersion mismatch");
        }
    }

    private long persistNewVersion(DispatchConfigSnapshotEntity cur, String general, String scoring, String ie,
                                   String bundling, String exclusivity,
                                   String actorUserId, String actorEmail, String group, String path, String diff) {
        DispatchConfigActiveEntity activeBefore = activeRepository.findById(1).orElseThrow();
        DispatchConfigVersionEntity prev = versionRepository.findByVersion(activeBefore.getVersion()).orElseThrow();
        long nv = versionRepository.maxVersion() + 1;
        DispatchConfigVersionEntity ver = new DispatchConfigVersionEntity();
        ver.setVersion(nv);
        ver.setOptimisticLock(prev.getOptimisticLock() + 1);
        ver.setCreatedByUserId(actorUserId);
        ver.setCreatedByEmail(actorEmail);
        versionRepository.save(ver);

        DispatchConfigSnapshotEntity next = new DispatchConfigSnapshotEntity();
        next.setVersion(nv);
        next.setGeneralJson(general);
        next.setScoringJson(scoring);
        next.setInternalExternalJson(ie);
        next.setBundlingJson(bundling);
        next.setExclusivityJson(exclusivity);
        snapshotRepository.save(next);

        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        active.setVersion(nv);
        active.setUpdatedAt(Instant.now());
        activeRepository.save(active);

        DispatchConfigAuditEntity audit = new DispatchConfigAuditEntity();
        audit.setActorUserId(actorUserId);
        audit.setActorEmail(actorEmail);
        audit.setHttpMethod("PUT");
        audit.setConfigGroup(group);
        audit.setEndpointPath(path);
        audit.setDiffSummary(diff);
        audit.setConfigVersion(nv);
        auditRepository.save(audit);
        return nv;
    }

    private DispatchConfigSnapshotEntity currentSnapshot() {
        DispatchConfigActiveEntity active = activeRepository.findById(1).orElseThrow();
        return snapshotRepository.findById(active.getVersion()).orElseThrow();
    }

    private static <T> DispatchConfigDtos.GroupEnvelope<T> envelope(DispatchConfigDtos.MetaResponse meta, T data) {
        DispatchConfigDtos.GroupEnvelope<T> g = new DispatchConfigDtos.GroupEnvelope<>();
        g.setMeta(meta);
        g.setData(data);
        return g;
    }
}
