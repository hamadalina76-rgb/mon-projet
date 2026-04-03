package com.speedline.promotion.dto;

import java.util.List;

public record PromotionPageResponse(
    List<PromotionDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {}
