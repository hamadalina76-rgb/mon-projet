package com.speedline.promotion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.promotion.domain.*;
import com.speedline.promotion.dto.*;
import com.speedline.promotion.event.PromotionEventPublisher;
import com.speedline.promotion.exception.PromotionException;
import com.speedline.promotion.repository.*;
import com.speedline.promotion.service.PromotionService;
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
    private final UserPromotionRepository    userPromotionRepository;
    private final PromotionEventPublisher    eventPublisher;
    private final ObjectMapper               objectMapper;

    // ---------------------------------------------------------------- CRUD --

    @Override
    @Transactional
    public PromotionPageResponse getPromotions(String search, String statusStr, String typeStr,
                                                LocalDateTime startFrom, LocalDateTime startTo,
                                                String partnerId, Pageable pageable) {
        // Expire overdue promotions before reading
        promotionRepository.expirePromotions(LocalDateTime.now());

        String status = (statusStr != null && !statusStr.isBlank()) ? statusStr.toUpperCase() : null;
        String type   = (typeStr   != null && !typeStr.isBlank())   ? typeStr.toUpperCase()   : null;
        String pid    = (partnerId != null && !partnerId.isBlank()) ? partnerId               : null;
        Page<Promotion> page = promotionRepository.findFiltered(search, status, type, startFrom, startTo, pid, pageable);
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
        return new PromotionDetailDto(PromotionDto.from(p), applied, revoked, totalRevenue, uniqueUsers);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDto getByCode(String code) {
        return PromotionDto.from(findByCode(code));
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
        PromotionDto dto = PromotionDto.from(promo);
        eventPublisher.publishPromotionCreated(dto);
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
            p.setStatus(req.status());
            p.setIsActive(req.status() == PromotionStatus.ACTIVE);
        }
        return PromotionDto.from(promotionRepository.save(p));
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
        log.info("Promotion soft-deleted: id={} code={}", id, p.getCode());
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void toggleActive(Long id) {
        Promotion p = findById(id);
        if (!Boolean.TRUE.equals(p.getIsActive())) {
            // Trying to activate — check if expired
            checkNotExpired(p);
        }
        boolean newActive = !Boolean.TRUE.equals(p.getIsActive());
        p.setIsActive(newActive);
        p.setStatus(newActive ? PromotionStatus.ACTIVE : PromotionStatus.INACTIVE);
        promotionRepository.save(p);
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void activate(Long id) {
        Promotion p = findById(id);
        checkNotExpired(p);
        p.setIsActive(true);
        p.setStatus(PromotionStatus.ACTIVE);
        promotionRepository.save(p);
        log.info("Promotion activated: id={} code={}", id, p.getCode());
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public void deactivate(Long id) {
        Promotion p = findById(id);
        p.setIsActive(false);
        p.setStatus(PromotionStatus.INACTIVE);
        promotionRepository.save(p);
        log.info("Promotion deactivated: id={} code={}", id, p.getCode());
    }

    @Override
    @CacheEvict(value = "promotions:active", allEntries = true)
    public PromotionDto duplicate(Long id) {
        Promotion original = findById(id);
        Promotion copy = Promotion.builder()
            .code(null)  // code must be set by admin
            .name(original.getName() + " (copie)")
            .description(original.getDescription())
            .type(original.getType())
            .value(original.getValue())
            .maximumDiscount(original.getMaximumDiscount())
            .minimumOrder(original.getMinimumOrder())
            .usageLimit(original.getUsageLimit())
            .usageLimitPerUser(original.getUsageLimitPerUser())
            .usageCount(0)
            .startDate(original.getStartDate())
            .endDate(original.getEndDate())
            .applicablePartnerIds(original.getApplicablePartnerIds())
            .applicableCategoryIds(original.getApplicableCategoryIds())
            .applicableZoneIds(original.getApplicableZoneIds())
            .firstOrderOnly(original.getFirstOrderOnly())
            .status(PromotionStatus.INACTIVE)
            .isActive(false)
            .build();
        copy = promotionRepository.save(copy);
        log.info("Promotion duplicated: original={} copy={}", id, copy.getId());
        return PromotionDto.from(copy);
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

    // ---------------------------------------------------------- VALIDATION --

    @Override
    @Transactional(readOnly = true)
    public ValidatePromotionResponse validate(ValidatePromotionRequest req) {
        Promotion p = findByCode(req.code());
        doValidate(p, req.userId(), req.orderSubtotal(), req.partnerId(), req.categoryIds());
        BigDecimal discount = computeDiscount(p, req.orderSubtotal(), req.deliveryFee());
        return buildResponse(p, req.orderSubtotal(), req.deliveryFee(), discount,
            "Code appliqué ! Vous économisez " + discount + " TND");
    }

    // --------------------------------------------------------------- APPLY --

    @Override
    public ValidatePromotionResponse apply(ApplyPromotionRequest req) {
        Promotion p = findByCode(req.code());
        doValidate(p, req.userId(), req.orderSubtotal(), req.partnerId(), req.categoryIds());
        BigDecimal discount = computeDiscount(p, req.orderSubtotal(), req.deliveryFee());

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
            eventPublisher.publishPromotionRevoked(p.getId(), p.getCode(), req.userId(), req.orderId());
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
        // end_date < start_date → 400
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw PromotionException.invalidDateRange();
        }
        // discount_value > 100 for PERCENTAGE → 400
        if (req.type() == PromotionType.PERCENTAGE && req.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw PromotionException.invalidPercentage(req.value());
        }
    }

    private void checkNotExpired(Promotion p) {
        if (p.getEndDate() != null && p.getEndDate().isBefore(LocalDateTime.now())) {
            throw PromotionException.cannotActivateExpired(p.getCode());
        }
    }

    private void doValidate(Promotion p, Long userId, BigDecimal subtotal, Long partnerId, List<Long> categoryIds) {
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(p.getIsActive())) throw PromotionException.inactive(p.getCode());
        if (p.getStartDate() != null && now.isBefore(p.getStartDate())) throw PromotionException.notStarted(p.getCode());
        if (p.getEndDate()   != null && now.isAfter(p.getEndDate()))    throw PromotionException.expired(p.getCode());
        if (p.getUsageLimit() != null && p.getUsageCount() >= p.getUsageLimit())
            throw PromotionException.quotaExceeded(p.getCode());

        // Per-user quota
        if (p.getUsageLimitPerUser() != null && userId != null) {
            long userUsage = userPromotionRepository.findByUserIdAndPromotionId(userId, p.getId())
                .map(u -> (long) u.getUsageCount()).orElse(0L);
            if (userUsage >= p.getUsageLimitPerUser()) throw PromotionException.userQuotaExceeded(p.getCode());
        }

        // Minimum order
        if (p.getMinimumOrder() != null && subtotal.compareTo(p.getMinimumOrder()) < 0)
            throw PromotionException.minimumOrderNotMet(p.getCode());

        // Partner restriction
        if (p.getApplicablePartnerIds() != null && !p.getApplicablePartnerIds().isBlank()
                && partnerId != null) {
            List<Long> allowed = PromotionDto.parseIds(p.getApplicablePartnerIds());
            if (!allowed.isEmpty() && !allowed.contains(partnerId))
                throw PromotionException.partnerNotEligible(p.getCode());
        }

        // Category restriction
        if (p.getApplicableCategoryIds() != null && !p.getApplicableCategoryIds().isBlank()
                && categoryIds != null && !categoryIds.isEmpty()) {
            List<Long> allowed = PromotionDto.parseIds(p.getApplicableCategoryIds());
            if (!allowed.isEmpty() && categoryIds.stream().noneMatch(allowed::contains))
                throw PromotionException.categoryNotEligible(p.getCode());
        }
    }

    private BigDecimal computeDiscount(Promotion p, BigDecimal subtotal, BigDecimal deliveryFee) {
        BigDecimal discount;
        if (p.getType() == PromotionType.PERCENTAGE) {
            discount = subtotal.multiply(p.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (p.getType() == PromotionType.FIXED_AMOUNT) {
            discount = p.getValue().min(subtotal);
        } else { // FREE_DELIVERY
            discount = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        }
        // Apply maximum discount cap
        if (p.getMaximumDiscount() != null && discount.compareTo(p.getMaximumDiscount()) > 0) {
            discount = p.getMaximumDiscount();
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private ValidatePromotionResponse buildResponse(Promotion p, BigDecimal subtotal,
                                                     BigDecimal deliveryFee, BigDecimal discount,
                                                     String message) {
        BigDecimal newSubtotal = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal fee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        if (p.getType() == PromotionType.FREE_DELIVERY) fee = BigDecimal.ZERO;
        return new ValidatePromotionResponse(
            true, discount, p.getType(), subtotal, newSubtotal, fee,
            newSubtotal.add(fee), message
        );
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(ids); }
        catch (JsonProcessingException e) { return ids.toString(); }
    }
}

