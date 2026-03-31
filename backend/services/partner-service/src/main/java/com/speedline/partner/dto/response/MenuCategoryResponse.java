package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Réponse représentant une catégorie de menu d'un partenaire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryResponse {

    private Long id;
    private Long partnerId;
    private String name;
    private String description;
    private String imageUrl;
    private Integer position;
    private Boolean isVisible;
    /** Nombre de produits actifs (non supprimés) dans cette catégorie. */
    private Long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
