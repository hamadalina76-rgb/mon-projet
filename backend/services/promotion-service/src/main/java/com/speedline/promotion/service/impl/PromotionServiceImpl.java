package com.speedline.promotion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.promotion.domain.*;
import com.speedline.promotion.domain.PromotionAuditLog.AuditAction;
import com.speedline.promotion.dto.*;
import com.speedline.promotion.event.PromotionEventPublisher;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.repository.*;
import com.speedline.promotion.service.DiscountCalculator;
import com.speedline.promotion.service.PromotionService;
import com.speedline.promotion.service.RedisPromotionCacheService;
import com.speedline.promotion.service.RedisQuotaService;
import com.speedline.promotion.validation.ValidationChain;
import com.speedline.promotion.validation.ValidationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository        promotionRepository;
    private final PromotionRuleRepository    ruleRepository;
    private final PromotionUsageLogRepository usageLogRepository;
    private final PromotionAuditLogRepository auditLogRepository;
    private final UserPromotionRepository    userPromotionRepository;
    private final PromotionEventPublisher    eventPublisher;
    private final ObjectMapper               objectMapper;
    private final ValidationChain            validationChain;
    private final DiscountCalculator         discountCalculator;
    private final RedisQuotaService          redisQuotaService;
    private final RedisPromotionCacheService cacheService;

    // ---------------------------------------------------------------- CRUD --

    @Override
    @Transactional
    public PromotionPageResponse getPromotions(String search, String statusStr, String typeStr,
                                                LocalDateTime startFrom, LocalDateTime startTo,
                                                String partnerId, String zoneId, Pageable pageable) {
        // Expire overdue promotions before reading
        promotionRepository.expirePromotions(LocalDateTime.now());

        String status = (statusStr != null && !statusStr.isBlank()) ? statusStr.toUpperCase() : null;
        String type   = (typeStr   != null && !typeStr.isBlank())   ? typeStr.toUpperCase()   : null;
        String pid    = (partnerId != null && !partnerId.isBlank()) ? partnerId               : null;
        String zid    = (zoneId    != null && !zoneId.isBlank())    ? zoneId                  : null;
        Page<Promotion> page = promotionRepository.findFiltered(search, status, type, startFrom, startTo, pid, zid, pageable);
        return new PromotionPageResponse(
            page.getContent().stream().map(PromotionDto::from).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("promotions:active")
    public List<PromotionDto> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now())
            .stream().map(PromotionDto::from).toList();
    }

    @Override
    @Transactional
    public PromotionDetailDto getById(Long id) {
        Promotion p = findById(id);

        // Auto-expire if end date has passed
        if (p.getEndDate() != null && p.getEndDate().isBefore(LocalDateTime.now())
                && p.getStatus() != PromotionStatus.EXPIRED) {
            p.setStatus(PromotionStatus.EXPIRED);
            p.setIsActive(false);
            promotionRepository.save(p);
        }

        long applied  = usageLogRepository.countByPromotionIdAndStatus(id, PromotionUsageLog.UsageStatus.APPLIED);
        long revoked  = usageLogRepository.countByPromotionIdAndStatus(id, PromotionUsageLog.UsageStatus.REVOKED);
        BigDecimal totalRevenue = usageLogRepository.sumDiscountByPromotion(id);
        long uniqueUsers = usageLogRepository.countUniqueUsersByPromotionId(id);
        List<PromotionRule> rules = ruleRepository.findByPromotionId(id);
        return new PromotionDetailDto(PromotionDto.from(p, rules), applied, revoked, totalRevenue, uniqueUsers);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDto getByCode(String code) {
        return PromotionDto.from(findByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCodeAvailable(String code) {
        return !promotionRepository.existsByCode(code.toUpperCase());
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public PromotionDto create(CreatePromotionRequest req) {
        // Validate business rules
        validateCreateRequest(req);

        if (promotionRepository.existsByCode(req.code())) {
            throw PromotionException.duplicateCode(req.code());
        }
        PromotionStatus status = req.status() != null ? req.status() : PromotionStatus.ACTIVE;

        // Auto-detect SCHEDULED: if startDate is in the future and status is ACTIVE
        if (status == PromotionStatus.ACTIVE
                && req.startDate() != null
                && req.startDate().isAfter(LocalDateTime.now())) {
            status = PromotionStatus.SCHEDULED;
        }

        Promotion promo = Promotion.builder()
            .code(req.code().toUpperCase())
            .name(req.name())
            .description(req.description())
            .type(req.type())
            .value(req.value())
            .maximumDiscount(req.maximumDiscount())
            .minimumOrder(req.minimumOrder())
            .usageLimit(req.usageLimitTotal())
            .usageLimitPerUser(req.usageLimitPerUser())
            .startDate(req.startDate())
            .endDate(req.endDate())
            .applicablePartnerIds(toJson(req.applicablePartnerIds()))
            .applicableCategoryIds(toJson(req.applicableCategoryIds()))
            .applicableZoneIds(toJson(req.applicableZoneIds()))
            .firstOrderOnly(req.firstOrderOnly() != null && req.firstOrderOnly())
            .status(status)
            .isActive(status == PromotionStatus.ACTIVE)
            .build();
        promo = promotionRepository.save(promo);

        // Save rules if provided
        List<com.speedline.promotion.domain.PromotionRule> savedRules = saveRules(promo.getId(), req.rules());

        PromotionDto dto = PromotionDto.from(promo, savedRules);
        eventPublisher.publishPromotionCreated(dto);
        if (Boolean.TRUE.equals(promo.getIsActive())) {
            cacheService.addCode(promo.getCode());
        }
        audit(promo.getId(), promo.getCode(), AuditAction.CREATED,
              "Promotion créée — type=" + promo.getType() + " valeur=" + promo.getValue()
              + " statut=" + promo.getStatus());
        log.info("Promotion created: {}", promo.getCode());
        return dto;
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public PromotionDto update(Long id, UpdatePromotionRequest req) {
        Promotion p = findById(id);

        // Validate dates if both provided
        LocalDateTime newStart = req.startDate() != null ? req.startDate() : p.getStartDate();
        LocalDateTime newEnd   = req.endDate()   != null ? req.endDate()   : p.getEndDate();
        if (newStart != null && newEnd != null && newEnd.isBefore(newStart)) {
            throw PromotionException.invalidDateRange();
        }
        // Validate percentage
        PromotionType newType = req.type() != null ? req.type() : p.getType();
        BigDecimal newValue   = req.value() != null ? req.value() : p.getValue();
        if (newType == PromotionType.PERCENTAGE && newValue != null && newValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw PromotionException.invalidPercentage(newValue);
        }

        if (req.name()              != null) p.setName(req.name());
        if (req.description()       != null) p.setDescription(req.description());
        if (req.type()              != null) p.setType(req.type());
        if (req.value()             != null) p.setValue(req.value());
        if (req.maximumDiscount()   != null) p.setMaximumDiscount(req.maximumDiscount());
        if (req.minimumOrder()      != null) p.setMinimumOrder(req.minimumOrder());
        if (req.usageLimitTotal()   != null) p.setUsageLimit(req.usageLimitTotal());
        if (req.usageLimitPerUser() != null) p.setUsageLimitPerUser(req.usageLimitPerUser());
        if (req.startDate()         != null) p.setStartDate(req.startDate());
        if (req.endDate()           != null) p.setEndDate(req.endDate());
        if (req.applicablePartnerIds()   != null) p.setApplicablePartnerIds(toJson(req.applicablePartnerIds()));
        if (req.applicableCategoryIds()  != null) p.setApplicableCategoryIds(toJson(req.applicableCategoryIds()));
        if (req.applicableZoneIds()       != null) p.setApplicableZoneIds(toJson(req.applicableZoneIds()));
        if (req.firstOrderOnly()    != null) p.setFirstOrderOnly(req.firstOrderOnly());
        if (req.status()            != null) {
            PromotionStatus newStatus = req.status();
            // Auto-detect SCHEDULED on update
            LocalDateTime effectiveStart = req.startDate() != null ? req.startDate() : p.getStartDate();
            if (newStatus == PromotionStatus.ACTIVE
                    && effectiveStart != null
                    && effectiveStart.isAfter(LocalDateTime.now())) {
                newStatus = PromotionStatus.SCHEDULED;
            }
            p.setStatus(newStatus);
            p.setIsActive(newStatus == PromotionStatus.ACTIVE);
        }
        Promotion saved = promotionRepository.save(p);

        // Replace rules if provided
        List<com.speedline.promotion.domain.PromotionRule> updatedRules;
        if (req.rules() != null) {
            ruleRepository.deleteByPromotionId(saved.getId());
            updatedRules = saveRules(saved.getId(), req.rules().stream()
                .map(r -> new CreatePromotionRequest.RuleRequest(r.ruleType(), r.operator(), r.targetValue()))
                .toList());
        } else {
            updatedRules = ruleRepository.findByPromotionId(saved.getId());
        }
        audit(saved.getId(), saved.getCode(), AuditAction.UPDATED,
              "Promotion mise à jour — statut=" + saved.getStatus());
        return PromotionDto.from(saved, updatedRules);
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void delete(Long id) {
        Promotion p = findById(id);
        // Soft delete — preserve data
        p.setDeleted(true);
        p.setDeletedAt(LocalDateTime.now());
        p.setIsActive(false);
        p.setStatus(PromotionStatus.INACTIVE);
        promotionRepository.save(p);
        cacheService.removeCode(p.getCode());
        audit(p.getId(), p.getCode(), AuditAction.DELETED, "Promotion supprimée (soft-delete)");
        log.info("Promotion soft-deleted: id={} code={}", id, p.getCode());
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void toggleActive(Long id) {
        Promotion p = findById(id);
        if (!Boolean.TRUE.equals(p.getIsActive())) {
            checkNotExpired(p);
            checkNotScheduled(p);
        }
        boolean newActive = !Boolean.TRUE.equals(p.getIsActive());
        p.setIsActive(newActive);
        p.setStatus(newActive ? PromotionStatus.ACTIVE : PromotionStatus.INACTIVE);
        promotionRepository.save(p);
        if (newActive) cacheService.addCode(p.getCode());
        else cacheService.removeCode(p.getCode());
        audit(p.getId(), p.getCode(), AuditAction.TOGGLED,
              newActive ? "Promotion activée (toggle)" : "Promotion désactivée (toggle)");
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void activate(Long id) {
        Promotion p = findById(id);
        checkNotExpired(p);
        checkNotScheduled(p);
        p.setIsActive(true);
        p.setStatus(PromotionStatus.ACTIVE);
        promotionRepository.save(p);
        cacheService.addCode(p.getCode());
        audit(p.getId(), p.getCode(), AuditAction.ACTIVATED, "Promotion activée");
        log.info("Promotion activated: id={} code={}", id, p.getCode());
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void deactivate(Long id) {
        Promotion p = findById(id);
        p.setIsActive(false);
        p.setStatus(PromotionStatus.INACTIVE);
        promotionRepository.save(p);
        cacheService.removeCode(p.getCode());
        audit(p.getId(), p.getCode(), AuditAction.DEACTIVATED, "Promotion désactivée");
        log.info("Promotion deactivated: id={} code={}", id, p.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionStatisticsDto getStatistics() {
        long active   = promotionRepository.countByStatus(PromotionStatus.ACTIVE);
        long inactive = promotionRepository.countByStatus(PromotionStatus.INACTIVE);
        long expired  = promotionRepository.countByStatus(PromotionStatus.EXPIRED);
        long total    = promotionRepository.count();
        long totalUsages  = promotionRepository.sumAllUsageCount();
        BigDecimal totalDiscount = usageLogRepository.sumAllAppliedDiscount();
        return new PromotionStatisticsDto(active, inactive, expired, total, totalUsages, totalDiscount);
    }

    // ----------------------------------------------------------- SIMULATE --

    @Override
    public SimulateDiscountResponse simulate(SimulateDiscountRequest req) {
        BigDecimal subtotal = req.orderSubtotal();
        BigDecimal deliveryFee = req.deliveryFee() != null ? req.deliveryFee() : BigDecimal.ZERO;
        BigDecimal value = req.value() != null ? req.value() : BigDecimal.ZERO;
        BigDecimal discount;

        if (req.type() == PromotionType.PERCENTAGE) {
            discount = subtotal.multiply(value)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (req.type() == PromotionType.FIXED_AMOUNT) {
            discount = value.min(subtotal);
        } else { // FREE_DELIVERY
            discount = deliveryFee;
        }

        // Apply max discount cap
        if (req.maximumDiscount() != null && discount.compareTo(req.maximumDiscount()) > 0) {
            discount = req.maximumDiscount();
        }

        discount = discount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal newSubtotal = subtotal.subtract(
                req.type() == PromotionType.FREE_DELIVERY ? BigDecimal.ZERO : discount);
        BigDecimal total = newSubtotal.add(
                req.type() == PromotionType.FREE_DELIVERY ? BigDecimal.ZERO : deliveryFee);

        String message = String.format("Réduction %.2f TND — Total %.2f TND", discount, total);

        return new SimulateDiscountResponse(discount, newSubtotal, deliveryFee, total, message);
    }

    // ---------------------------------------------------------- VALIDATION --

    @Override
    @Transactional(readOnly = true)
    public ValidatePromotionResponse validate(ValidatePromotionRequest req) {
        Promotion p = findByCode(req.code());
        validationChain.validate(buildValidationContext(p, req.userId(), req.orderSubtotal(),
                req.deliveryFee(), req.partnerId(), req.categoryIds(), req.itemCount()));
        BigDecimal discount = discountCalculator.compute(p, req.orderSubtotal(), req.deliveryFee());
        return buildResponse(p, req.orderSubtotal(), req.deliveryFee(), discount,
            "Code appliqué ! Vous économisez " + discount + " TND");
    }

    // --------------------------------------------------------------- APPLY --

    @Override
    public ValidatePromotionResponse apply(ApplyPromotionRequest req) {
        Promotion p = findByCode(req.code());
        validationChain.validate(buildValidationContext(p, req.userId(), req.orderSubtotal(),
                req.deliveryFee(), req.partnerId(), req.categoryIds(), req.itemCount()));

        // Redis atomic DECR for global quota
        if (p.getUsageLimit() != null
                && !redisQuotaService.tryConsume(p.getId(), p.getUsageLimit(), p.getUsageCount())) {
            throw PromotionException.quotaExceeded(p.getCode());
        }

        BigDecimal discount = discountCalculator.compute(p, req.orderSubtotal(), req.deliveryFee());

        // Atomic: increment counters + log
        promotionRepository.incrementUsageCount(p.getId());
        userPromotionRepository.upsertUsage(req.userId(), p.getId());
        usageLogRepository.save(PromotionUsageLog.builder()
            .promotionId(p.getId())
            .userId(req.userId())
            .orderId(req.orderId())
            .discountAmount(discount)
            .status(PromotionUsageLog.UsageStatus.APPLIED)
            .build());

        ValidatePromotionResponse resp = buildResponse(p, req.orderSubtotal(), req.deliveryFee(), discount,
            "Code appliqué ! Vous économisez " + discount + " TND");

        eventPublisher.publishPromotionApplied(p.getId(), p.getCode(), req.userId(), req.orderId(), resp);
        audit(p.getId(), p.getCode(), AuditAction.APPLIED,
              "Utilisée par user=" + req.userId() + " commande=" + req.orderId()
              + " réduction=" + discount + " TND");
        log.info("Promotion applied: code={} user={} order={} discount={}",
            p.getCode(), req.userId(), req.orderId(), discount);
        return resp;
    }

    // -------------------------------------------------------------- REVOKE --

    @Override
    public void revoke(RevokePromotionRequest req) {
        Promotion p = findByCode(req.code());
        int rows = usageLogRepository.revoke(p.getId(), req.userId(), req.orderId(), LocalDateTime.now());
        if (rows > 0) {
            promotionRepository.decrementUsageCount(p.getId());
            redisQuotaService.release(p.getId());
            eventPublisher.publishPromotionRevoked(p.getId(), p.getCode(), req.userId(), req.orderId());
            audit(p.getId(), p.getCode(), AuditAction.REVOKED,
                  "Révoquée pour user=" + req.userId() + " commande=" + req.orderId());
            log.info("Promotion revoked: code={} user={} order={}", p.getCode(), req.userId(), req.orderId());
        } else {
            log.warn("Revoke found no APPLIED log: code={} user={} order={}", p.getCode(), req.userId(), req.orderId());
        }
    }

    // ----------------------------------------------------------- ANALYTICS --

    @Override
    @Transactional(readOnly = true)
    public PromotionAnalyticsDto getAnalytics(Long promotionId) {
        Promotion p = findById(promotionId);
        long applied = usageLogRepository.countByPromotionIdAndStatus(promotionId, PromotionUsageLog.UsageStatus.APPLIED);
        long revoked = usageLogRepository.countByPromotionIdAndStatus(promotionId, PromotionUsageLog.UsageStatus.REVOKED);
        BigDecimal totalDiscount = usageLogRepository.sumDiscountByPromotion(promotionId);
        long uniqueUsers = usageLogRepository.countUniqueUsersByPromotionId(promotionId);
        BigDecimal avgDiscount = applied > 0
            ? totalDiscount.divide(BigDecimal.valueOf(applied), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        double rate = p.getUsageLimit() != null && p.getUsageLimit() > 0
            ? (applied * 100.0 / p.getUsageLimit())
            : 0;

        List<PromotionAnalyticsDto.UsageLogEntry> logs = usageLogRepository
            .findByPromotionId(promotionId).stream()
            .map(l -> new PromotionAnalyticsDto.UsageLogEntry(
                l.getUserId(), l.getOrderId(), l.getDiscountAmount(),
                l.getStatus().name(), l.getCreatedAt()))
            .toList();

        return new PromotionAnalyticsDto(
            p.getId(), p.getCode(), p.getName(),
            applied, revoked, applied - revoked,
            totalDiscount, uniqueUsers, avgDiscount,
            p.getUsageLimit(), rate, logs
        );
    }

    // --------------------------------------------------------- DASHBOARD --

    @Override
    @Transactional(readOnly = true)
    public PromotionDashboardDto getDashboard() {
        LocalDateTime now = LocalDateTime.now();

        // KPIs
        long activeCount = promotionRepository.countActivePromotions(now);
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        long usagesThisMonth = usageLogRepository.countAppliedBetween(startOfMonth, now);
        BigDecimal totalDiscountTnd = usageLogRepository.sumAllAppliedDiscount();
        long totalUsages = promotionRepository.sumAllUsageCount();
        long totalLimit  = promotionRepository.sumUsageLimitOfActive();
        double usageRatePct = totalLimit > 0 ? (totalUsages * 100.0 / totalLimit) : 0;

        // Top 5 promotions by usage
        List<PromotionDashboardDto.TopPromotion> top5 = promotionRepository
            .findTopByUsage(org.springframework.data.domain.PageRequest.of(0, 5))
            .stream()
            .map(p -> new PromotionDashboardDto.TopPromotion(p.getId(), p.getCode(), p.getName(), p.getUsageCount()))
            .toList();

        // Daily usage last 30 days (fill gaps with 0)
        LocalDateTime thirtyDaysAgo = now.minusDays(30).toLocalDate().atStartOfDay();
        List<Object[]> raw = usageLogRepository.countDailyUsageSince(thirtyDaysAgo);
        java.util.Map<java.time.LocalDate, Long> dayMap = new java.util.LinkedHashMap<>();
        for (Object[] row : raw) {
            java.time.LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
            long cnt = ((Number) row[1]).longValue();
            dayMap.put(d, cnt);
        }
        List<PromotionDashboardDto.DailyUsage> dailyUsages = new java.util.ArrayList<>();
        for (java.time.LocalDate d = thirtyDaysAgo.toLocalDate(); !d.isAfter(now.toLocalDate()); d = d.plusDays(1)) {
            dailyUsages.add(new PromotionDashboardDto.DailyUsage(d, dayMap.getOrDefault(d, 0L)));
        }

        // Expiring alerts (within 7 days)
        LocalDateTime sevenDaysLater = now.plusDays(7);
        List<PromotionDashboardDto.ExpiringPromotion> expiringAlerts = promotionRepository
            .findExpiringBetween(now, sevenDaysLater)
            .stream()
            .map(p -> {
                long daysRemaining = java.time.Duration.between(now, p.getEndDate()).toDays();
                return new PromotionDashboardDto.ExpiringPromotion(
                    p.getId(), p.getCode(), p.getName(), p.getEndDate(), daysRemaining);
            })
            .toList();

        return new PromotionDashboardDto(activeCount, usagesThisMonth, totalDiscountTnd,
            usageRatePct, top5, dailyUsages, expiringAlerts);
    }

    // -------------------------------------------------- INTERNAL HELPERS ---

    private Promotion findById(Long id) {
        return promotionRepository.findById(id)
            .orElseThrow(() -> new PromotionException("PROMOTION_NOT_FOUND", "Promotion introuvable : id=" + id));
    }

    private Promotion findByCode(String code) {
        return promotionRepository.findByCode(code.toUpperCase())
            .orElseThrow(() -> PromotionException.notFound(code));
    }

    private void validateCreateRequest(CreatePromotionRequest req) {
        // FREE_DELIVERY doesn't need a value; others must have value > 0
        if (req.type() != PromotionType.FREE_DELIVERY) {
            if (req.value() == null || req.value().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PromotionException("INVALID_VALUE", "La valeur est obligatoire et doit être positive");
            }
        }
        // end_date < start_date → 400
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw PromotionException.invalidDateRange();
        }
        // discount_value > 100 for PERCENTAGE → 400
        if (req.type() == PromotionType.PERCENTAGE && req.value() != null && req.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw PromotionException.invalidPercentage(req.value());
        }
    }

    private void checkNotExpired(Promotion p) {
        if (p.getEndDate() != null && p.getEndDate().isBefore(LocalDateTime.now())) {
            throw PromotionException.cannotActivateExpired(p.getCode());
        }
    }

    private void checkNotScheduled(Promotion p) {
        if (p.getStatus() == PromotionStatus.SCHEDULED) {
            throw PromotionException.cannotActivateScheduled(p.getCode());
        }
    }

    private ValidationContext buildValidationContext(Promotion p, Long userId,
                                                     BigDecimal subtotal, BigDecimal deliveryFee,
                                                     Long partnerId, List<Long> categoryIds,
                                                     Integer itemCount) {
        return ValidationContext.builder()
                .promotion(p)
                .userId(userId)
                .orderSubtotal(subtotal)
                .deliveryFee(deliveryFee)
                .partnerId(partnerId)
                .categoryIds(categoryIds)
                .itemCount(itemCount)
                .build();
    }

    private ValidatePromotionResponse buildResponse(Promotion p, BigDecimal subtotal,
                                                     BigDecimal deliveryFee, BigDecimal discount,
                                                     String message) {
        BigDecimal newSubtotal = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal fee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        if (p.getType() == PromotionType.FREE_DELIVERY) fee = BigDecimal.ZERO;
        return new ValidatePromotionResponse(
            true, p.getId(), discount, p.getType(), subtotal, newSubtotal, fee,
            newSubtotal.add(fee), message
        );
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(ids); }
        catch (JsonProcessingException e) { return ids.toString(); }
    }

    private List<PromotionRule> saveRules(Long promotionId, List<CreatePromotionRequest.RuleRequest> rules) {
        if (rules == null || rules.isEmpty()) return List.of();
        List<PromotionRule> entities = rules.stream()
            .map(r -> PromotionRule.builder()
                .promotionId(promotionId)
                .ruleType(r.ruleType())
                .operator(r.operator())
                .targetValue(r.targetValue())
                .build())
            .toList();
        return ruleRepository.saveAll(entities);
    }

    // --------------------------------------------------------------- CSV --

    @Override
    @Transactional
    public String exportCsv(String search, String status, String type) {
        // Re-use the same filtered query but without pagination
        var page = getPromotions(search, status, type, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10_000,
                        org.springframework.data.domain.Sort.by("created_at").descending()));
        var sb = new StringBuilder();
        sb.append("Code,Nom,Type,Valeur,Statut,Utilisations,Limite,Date Début,Date Fin\n");
        for (var dto : page.content()) {
            sb.append(escapeCsv(dto.code())).append(',')
              .append(escapeCsv(dto.name())).append(',')
              .append(dto.type()).append(',')
              .append(dto.value()).append(',')
              .append(dto.status()).append(',')
              .append(dto.usageCount() != null ? dto.usageCount() : 0).append(',')
              .append(dto.usageLimitTotal() != null ? dto.usageLimitTotal() : "∞").append(',')
              .append(dto.startDate() != null ? dto.startDate().toLocalDate() : "").append(',')
              .append(dto.endDate() != null ? dto.endDate().toLocalDate() : "")
              .append('\n');
        }
        return sb.toString();
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    // ----------------------------------------------------------- AUDIT ---

    private void audit(Long promotionId, String code, AuditAction action, String details) {
        auditLogRepository.save(PromotionAuditLog.builder()
            .promotionId(promotionId)
            .promotionCode(code)
            .action(action)
            .details(details)
            .build());
    }
}

