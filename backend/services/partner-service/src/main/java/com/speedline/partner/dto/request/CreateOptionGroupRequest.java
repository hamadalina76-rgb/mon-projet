package com.speedline.partner.dto.request;

import com.speedline.partner.domain.ProductOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de requête pour créer un groupe d'options (ProductOption).
 * TC-14 : SINGLE minSelection=1 maxSelection=1.
 * TC-15 : MULTIPLE minSelection=0 maxSelection=3.
 */
@Data
public class CreateOptionGroupRequest {

    @NotBlank(message = "Le nom du groupe d'options est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    /**
     * Type de sélection : SINGLE (radio) ou MULTIPLE (checkbox).
     */
    @NotNull(message = "Le type est obligatoire")
    private ProductOption.OptionType type;

    /** Le client doit obligatoirement faire un choix (défaut = false). */
    private Boolean isRequired;

    /** Nombre minimum de valeurs à sélectionner (défaut = 0). */
    @Min(value = 0, message = "minSelection doit être ≥ 0")
    private Integer minSelection;

    /** Nombre maximum de valeurs sélectionnables (défaut = 1). */
    @Min(value = 1, message = "maxSelection doit être ≥ 1")
    private Integer maxSelection;

    /**
     * Position dans la liste du produit (null = auto).
     */
    private Integer position;
}
