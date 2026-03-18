package com.speedline.partner.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {

    private Long id;

    @NotNull(message = "Les noms multilingues sont requis")
    @NotEmpty(message = "Au moins une locale est requise")
    private Map<String, String> nameI18n;

    private String slug;
    private String description;
    private String icon;
    private String image;
    private Long parentId;
    private Integer depth;

    @NotNull
    private Integer displayOrder;

    @NotNull
    private Boolean isActive;

    @NotNull
    private Boolean isFeatured;

    private String categoryType;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Format couleur invalide (#RRGGBB)")
    private String backgroundColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Format couleur invalide (#RRGGBB)")
    private String textColor;

    private Integer partnerCount;
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
}