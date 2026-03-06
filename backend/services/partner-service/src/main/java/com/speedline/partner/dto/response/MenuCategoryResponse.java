package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Réponse représentant une catégorie de menu d'un partenaire.
 */
@Data
@Builder
public class MenuCategoryResponse {

    private Long id;
    private Long partnerId;
    private String name;
    private String description;
    private String imageUrl;
    private Integer position;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
