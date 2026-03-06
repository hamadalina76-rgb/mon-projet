package com.speedline.partner.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Corps de requête pour ajouter une option (OptionValue) à un groupe.
 * TC-17 : extraPrice=0 → retourné, pas d'erreur.
 * TC-18 : position auto = max_position + 1.
 */
@Data
public class CreateOptionRequest {

    @NotBlank(message = "Le nom de l'option est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    /**
     * Supplément de prix (0 = gratuit). TC-17 : valeur 0 acceptée sans erreur.
     * Peut être null → traité comme 0.
     */
    @DecimalMin(value = "0.00", message = "Le prix supplémentaire ne peut pas être négatif")
    private BigDecimal priceModifier;

    /** Option pré-sélectionnée par défaut (défaut = false). */
    private Boolean isDefault;

    /** Disponible (défaut = true). */
    private Boolean isAvailable;

    /**
     * Position dans le groupe (null = auto : max + 1). TC-18.
     */
    private Integer position;
}
