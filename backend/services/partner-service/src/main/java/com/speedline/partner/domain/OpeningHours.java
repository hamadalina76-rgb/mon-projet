package com.speedline.partner.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Horaires d'ouverture pour un jour de la semaine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class OpeningHours {

    /**
     * Jour de la semaine (1=Lundi, 7=Dimanche)
     */
    private Integer dayOfWeek;

    /**
     * Heure d'ouverture
     */
    private LocalTime openTime;

    /**
     * Heure de fermeture
     */
    private LocalTime closeTime;

    /**
     * Fermé ce jour
     */
    @Builder.Default
    private Boolean isClosed = false;

    /**
     * Ouvert 24h/24 ce jour
     */
    @Builder.Default
    private Boolean is24Hours = false;

    /**
     * Pause déjeuner - heure de début (optionnel)
     */
    private LocalTime breakStartTime;

    /**
     * Pause déjeuner - heure de fin (optionnel)
     */
    private LocalTime breakEndTime;

    /**
     * Vérifier si le partenaire est ouvert à une heure donnée
     */
    public boolean isOpenAt(LocalTime time) {
        if (isClosed) return false;
        if (is24Hours) return true;
        
        // Vérifier les horaires principaux
        boolean inMainHours = !time.isBefore(openTime) && !time.isAfter(closeTime);
        
        // Vérifier la pause si définie
        if (breakStartTime != null && breakEndTime != null) {
            boolean inBreak = !time.isBefore(breakStartTime) && !time.isAfter(breakEndTime);
            return inMainHours && !inBreak;
        }
        
        return inMainHours;
    }
}
