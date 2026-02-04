package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entité OptionValue
 * Valeurs possibles pour une option de produit
 * Exemple: Pour l'option "Taille", les valeurs sont "Small", "Medium", "Large"
 */
@Entity
@Table(name = "option_values", indexes = {
    @Index(name = "idx_value_option", columnList = "optionId"),
    @Index(name = "idx_value_display_order", columnList = "displayOrder")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'option parente
     */
    @Column(nullable = false)
    private Long optionId;

    /**
     * Nom/Label de la valeur (ex: "Medium", "Bien cuit", "Extra fromage")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description de la valeur
     */
    @Column(length = 255)
    private String description;

    /**
     * Modificateur de prix (ajouté au prix de base)
     * Positif = supplément, Négatif = réduction, Zéro = pas de changement
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceModifier = BigDecimal.ZERO;

    /**
     * Cette valeur est-elle disponible actuellement
     */
    @Builder.Default
    private Boolean isAvailable = true;

    /**
     * Valeur par défaut (pré-sélectionnée)
     */
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Ordre d'affichage
     */
    @Builder.Default
    private Integer displayOrder = 0;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtenir le texte à afficher avec le prix
     * Ex: "Medium (+2.00 TND)" ou "Small" si pas de supplément
     */
    public String getDisplayText() {
        if (priceModifier.compareTo(BigDecimal.ZERO) > 0) {
            return name + " (+" + priceModifier + " TND)";
        } else if (priceModifier.compareTo(BigDecimal.ZERO) < 0) {
            return name + " (" + priceModifier + " TND)";
        }
        return name;
    }
}
