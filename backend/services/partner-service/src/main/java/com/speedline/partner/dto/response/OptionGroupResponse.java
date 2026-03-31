package com.speedline.partner.dto.response;

import com.speedline.partner.domain.ProductOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse représentant un groupe d'options (ProductOption) et ses valeurs (OptionValue).
 * TC-14 : SINGLE minSelection=1 maxSelection=1.
 * TC-15 : MULTIPLE minSelection=0 maxSelection=3.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionGroupResponse {

    private Long id;

    /** ID du produit auquel ce groupe appartient. */
    private Long productId;

    private String name;

    /** SINGLE = sélection unique / MULTIPLE = sélection multiple. */
    private ProductOption.OptionType type;

    private Boolean isRequired;

    private Integer minSelection;

    private Integer maxSelection;

    /** Position dans la liste du produit (ordre croissant). */
    private Integer position;

    /** Options disponibles dans ce groupe. */
    private List<OptionResponse> options;
}
