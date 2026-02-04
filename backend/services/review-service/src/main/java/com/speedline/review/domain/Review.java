package com.speedline.review.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité Review - Avis clients (MongoDB)
 */
@Document(collection = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    @Indexed
    private Long customerId;

    private String customerName;

    @Indexed
    private TargetType targetType;

    @Indexed
    private Long targetId;

    private Integer rating; // 1-5

    private String comment;

    private List<String> imageUrls;

    private String response;

    private LocalDateTime responseDate;

    private Long respondedBy;

    @Builder.Default
    private Boolean isVerified = false;

    @Builder.Default
    private Boolean isVisible = true;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum TargetType {
        PARTNER,
        COURIER,
        PRODUCT
    }
}
