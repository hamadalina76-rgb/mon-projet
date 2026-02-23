package com.speedline.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les calculs géométriques d'une zone
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneGeometryDTO {
    /**
     * Surface de la zone en km²
     */
    private BigDecimal areaKm2;
    
    /**
     * Centre géométrique [latitude, longitude]
     */
    private Double[] center;
    
    /**
     * Périmètre en km
     */
    private BigDecimal perimeterKm;
    
    /**
     * Nombre de points dans le polygone
     */
    private Integer pointCount;
}
