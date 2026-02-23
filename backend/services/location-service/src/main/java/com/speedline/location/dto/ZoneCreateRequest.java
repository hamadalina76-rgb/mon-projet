package com.speedline.location.dto;

import com.speedline.location.domain.Zone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Requête pour créer une nouvelle zone
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneCreateRequest {
    
    @NotBlank(message = "Le nom de la zone est requis")
    private String name;
    
    private String description;
    
    @NotBlank(message = "La ville est requise")
    private String city;
    
    @NotNull(message = "Le type de zone est requis")
    private Zone.ZoneType type;
    
    @NotBlank(message = "Le polygone de la zone est requis")
    private String boundaryJson; // Format: [[lat,lon],[lat,lon],...]
    
    private BigDecimal deliveryFee;
    
    private Integer minDeliveryTime;
    
    private Integer maxDeliveryTime;
    
    @Builder.Default
    private Boolean isActive = true;
}
