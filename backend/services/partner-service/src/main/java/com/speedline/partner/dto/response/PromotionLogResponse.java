package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PromotionLogResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String promotionLabel;
    private LocalDate promotionStartDate;
    private LocalDate promotionEndDate;
    private BigDecimal discountPercentage;
    private LocalDateTime appliedAt;
}
