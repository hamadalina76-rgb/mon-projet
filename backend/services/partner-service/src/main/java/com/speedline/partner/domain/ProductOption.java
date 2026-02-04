package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité ProductOption
 * Options de personnalisation d'un produit (taille, cuisson, etc.)
 * Exemple: "Taille" avec valeurs "Small", "Medium", "Large"
 */
@Entity
@Table(name = "product_options", indexes = {
    @Index(name = "idx_option_product", columnList = "productId"),
    @Index(name = "idx_option_display_order", columnList = "displayOrder")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers le produit
     */
    @Column(nullable = false)
    private Long productId;

    /**
     * Nom de l'option (ex: "Taille", "Cuisson", "Sauce")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description de l'option
     */
    @Column(length = 255)
    private String description;

    /**
     * Type d'option
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OptionType type = OptionType.SINGLE;

    /**
     * Option obligatoire (le client doit choisir)
     */
    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Nombre minimum de sélections (pour type MULTIPLE)
     */
    @Builder.Default
    private Integer minSelection = 0;

    /**
     * Nombre maximum de sélections (pour type MULTIPLE)
     */
    @Builder.Default
    private Integer maxSelection = 1;

    /**
     * Ordre d'affichage
     */
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Option active
     */
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==================== RELATIONS ====================

    /**
     * Valeurs possibles pour cette option
     */
    @OneToMany(mappedBy = "optionId", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OptionValue> values = new ArrayList<>();

    // ==================== ENUMS ====================

    public enum OptionType {
        /**
         * Sélection unique (radio buttons)
         * Ex: Taille (Small OU Medium OU Large)
         */
        SINGLE,
        
        /**
         * Sélection multiple (checkboxes)
         * Ex: Garnitures (Oignons ET Tomates ET Fromage)
         */
        MULTIPLE
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier si la sélection est valide
     * @param selectedCount Nombre de valeurs sélectionnées
     * @return true si la sélection respecte les contraintes
     */
    public boolean isValidSelection(int selectedCount) {
        if (isRequired && selectedCount == 0) {
            return false;
        }
        if (selectedCount < minSelection) {
            return false;
        }
        if (selectedCount > maxSelection) {
            return false;
        }
        return true;
    }
}
