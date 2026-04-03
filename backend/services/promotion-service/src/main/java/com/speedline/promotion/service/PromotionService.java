package com.speedline.promotion.service;

import com.speedline.promotion.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    /** CRUD ---------------------------------------------------------------- */
    PromotionPageResponse getPromotions(String search, String status, String type,
                                        LocalDateTime startFrom, LocalDateTime startTo,
                                        String partnerId, Pageable pageable);

    List<PromotionDto> getActivePromotions();

    PromotionDetailDto getById(Long id);

    PromotionDto getByCode(String code);

    PromotionDto create(CreatePromotionRequest request);

    PromotionDto update(Long id, UpdatePromotionRequest request);

    void delete(Long id);

    void toggleActive(Long id);

    void activate(Long id);

    void deactivate(Long id);

    PromotionDto duplicate(Long id);

    PromotionStatisticsDto getStatistics();

    /** Validation (before order confirmation) ------------------------------ */
    ValidatePromotionResponse validate(ValidatePromotionRequest request);

    /** Apply  (at payment — re-validates + increments counters atomically) - */
    ValidatePromotionResponse apply(ApplyPromotionRequest request);

    /** Revoke (on order cancellation) -------------------------------------- */
    void revoke(RevokePromotionRequest request);

    /** Analytics ----------------------------------------------------------- */
    PromotionAnalyticsDto getAnalytics(Long promotionId);
}
