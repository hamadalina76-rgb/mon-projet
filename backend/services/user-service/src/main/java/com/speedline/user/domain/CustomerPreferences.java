package com.speedline.user.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Préférences utilisateur (notifications, langue, etc.)
 * Classe Embeddable pour être intégrée dans Customer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CustomerPreferences {

    /**
     * Langue préférée (fr, en, ar, etc.)
     */
    private String language;

    /**
     * Devise préférée (TND, EUR, USD)
     */
    private String currency;

    /**
     * Activer les notifications push
     */
    @Builder.Default
    private Boolean pushNotificationsEnabled = true;

    /**
     * Activer les notifications email
     */
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    /**
     * Activer les notifications SMS
     */
    @Builder.Default
    private Boolean smsNotificationsEnabled = false;

    /**
     * Recevoir des promotions par email
     */
    @Builder.Default
    private Boolean marketingEmailsEnabled = true;

    /**
     * Mode sombre activé
     */
    @Builder.Default
    private Boolean darkModeEnabled = false;

    /**
     * Rayon de recherche par défaut (en mètres)
     */
    @Builder.Default
    private Integer defaultSearchRadius = 5000;

    /**
     * Catégories favorites (IDs séparés par virgule)
     */
    private String favoriteCategories;

    /**
     * Restrictions alimentaires (végétarien, halal, etc.)
     */
    private String dietaryRestrictions;

    /**
     * Allergènes à éviter
     */
    private String allergens;
}
