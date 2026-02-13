package com.speedline.user.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Permission associée à un rôle administrateur
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "role")
@EqualsAndHashCode(exclude = "role")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Module concerné (orders, clients, analytics, etc.)
     */
    @Column(nullable = false, length = 50)
    private String module;

    /**
     * Label d'affichage du module
     */
    @Column(nullable = false, length = 100)
    private String moduleLabel;

    /**
     * Icône Material pour l'affichage
     */
    @Column(length = 50)
    private String icon;

    /**
     * Description de la permission (français)
     */
    @Column(length = 255)
    private String description;

    /**
     * Description en anglais
     */
    @Column(name = "description_en", length = 255)
    private String descriptionEn;

    /**
     * Description en arabe
     */
    @Column(name = "description_ar", length = 255)
    private String descriptionAr;

    /**
     * Permission activée pour ce rôle
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /**
     * Rôle parent
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;
}
