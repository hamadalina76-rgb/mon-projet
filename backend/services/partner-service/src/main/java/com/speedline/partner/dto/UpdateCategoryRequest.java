package com.speedline.partner.dto;

import com.speedline.partner.domain.CategoryBusinessType;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @NotNull(message = "Les noms multilingues sont requis")
    @NotEmpty(message = "Au moins une locale est requise")
    private Map<String, String> nameI18n;

    private String description;
    private String icon;
    private String image;
    private Integer displayOrder;
    private Boolean isFeatured;
    private Boolean isActive;

    @NotNull(message = "Le type métier est requis")
    private CategoryBusinessType categoryBusinessType; // ✅ Majuscule

    private String categoryType;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Format couleur invalide (#RRGGBB)")
    private String backgroundColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Format couleur invalide (#RRGGBB)")
    private String textColor;
}