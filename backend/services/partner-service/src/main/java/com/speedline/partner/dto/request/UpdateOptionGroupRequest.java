package com.speedline.partner.dto.request;

import com.speedline.partner.domain.ProductOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corps de requête pour modifier un groupe d'options.
 * Tous les champs sont optionnels (patch sémantique).
 */
@Data
public class UpdateOptionGroupRequest {

    @Size(max = 100)
    private String name;

    private ProductOption.OptionType type;

    private Boolean isRequired;

    @Min(0)
    private Integer minSelection;

    @Min(1)
    private Integer maxSelection;

    private Integer position;
}
