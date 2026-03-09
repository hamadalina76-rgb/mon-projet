package com.speedline.partner.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Type(JsonBinaryType.class)
    @Column(name = "name_i18n", columnDefinition = "jsonb")
    private String nameI18n;

    private String slug; // ✅ AJOUT du champ slug manquant

    private String description;
    private String icon;
    private String image;
    private Long parentId;
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean isActive;

    private Boolean isFeatured;
    @Enumerated(EnumType.STRING) // ✅ Mapping Enum JPA
    @Column(name = "category_business_type")
    private CategoryBusinessType categoryBusinessType; // ✅
    private String categoryType;
    private String backgroundColor;
    private String textColor;

    private Integer partnerCount;
    private Integer productCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;




    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.isFeatured == null) this.isFeatured = false;
        if (this.partnerCount == null) this.partnerCount = 0;
        if (this.productCount == null) this.productCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}