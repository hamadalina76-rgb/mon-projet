package com.speedline.partner.dto;

import com.speedline.partner.domain.Category.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour Category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private String image;
    private Long parentId;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isFeatured;
    private CategoryType categoryType;
    private String backgroundColor;
    private String textColor;
    private Integer partnerCount;
    private Integer productCount;
    
    // Sous-catégories (si applicable)
    private List<CategoryDTO> subcategories;
}
