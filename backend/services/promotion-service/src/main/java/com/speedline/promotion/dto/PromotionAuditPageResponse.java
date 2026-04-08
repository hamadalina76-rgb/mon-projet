package com.speedline.promotion.dto;

import java.util.List;

public record PromotionAuditPageResponse(
    List<PromotionAuditLogDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {}
