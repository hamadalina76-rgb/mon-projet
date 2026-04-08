package com.speedline.promotion.service;

import com.speedline.promotion.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    /** CRUD ---------------------------------------------------------------- */
    PromotionPageResponse getPromotions(String search, String status, String type,
                                        LocalDateTime startFrom, LocalDateTime startTo,
                                        String partnerId, String zoneId, Pageable pageable);

    List<PromotionDto> getActivePromotions();

    PromotionDetailDto getById(Long id);

    PromotionDto getByCode(String code);

    boolean isCodeAvailable(String code);

    PromotionDto create(CreatePromotionRequest request);

    PromotionDto update(Long id, UpdatePromotionRequest request);

    void delete(Long id);

    void toggleActive(Long id);

    void activate(Long id);

    void deactivate(Long id);

    PromotionStatisticsDto getStatistics();

    /** Simulate discount (before creation — stepper step 4) --------------- */
    SimulateDiscountResponse simulate(SimulateDiscountRequest request);

    /** Validation (before order confirmation) ------------------------------ */
    ValidatePromotionResponse validate(ValidatePromotionRequest request);

    /** Apply  (at payment — re-validates + increments counters atomically) - */
    ValidatePromotionResponse apply(ApplyPromotionRequest request);

    /** Revoke (on order cancellation) -------------------------------------- */
    void revoke(RevokePromotionRequest request);

    /** Analytics ----------------------------------------------------------- */
    PromotionAnalyticsDto getAnalytics(Long promotionId);

    /** Dashboard analytics (global KPIs, charts, alerts) ------------------- */
    PromotionDashboardDto getDashboard();

    /** CSV export ---------------------------------------------------------- */
    String exportCsv(String search, String status, String type);
}
