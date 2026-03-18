package com.speedline.partner.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité Partner (Restaurant/Magasin)
 * Représente un partenaire commercial sur la plateforme
 */
@Entity
@Table(name = "partners", indexes = {
    @Index(name = "idx_partner_slug", columnList = "slug", unique = true),
    @Index(name = "idx_partner_status", columnList = "status"),
    @Index(name = "idx_partner_type", columnList = "type"),
    @Index(name = "idx_partner_city", columnList = "city"),
    @Index(name = "idx_partner_active", columnList = "isActive, acceptsOrders")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur propriétaire dans auth-service
     */
    @Column(nullable = false)
    private Long userId;

    // ==================== INFORMATIONS DE BASE ====================

    /**
     * Nom commercial du partenaire
     */
    @Column(nullable = false, length = 255)
    private String businessName;

    /**
     * Nom de marque (brand name)
     */
    @Column(length = 255)
    private String brandName;

    /**
     * Slug URL unique (ex: "pizza-house-tunis")
     */
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    /**
     * Type de partenaire
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnerType type;

    /**
     * Description du partenaire
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Description courte pour les listes
     */
    @Column(length = 500)
    private String shortDescription;

    /**
     * URL du logo
     */
    @Column(length = 500)
    private String logo;

    /**
     * URL de l'image de couverture
     */
    @Column(length = 500)
    private String coverImage;

    /**
     * Numéro de téléphone principal
     */
    @Column(length = 20)
    private String phoneNumber;

    /**
     * Email de contact
     */
    @Column(length = 255)
    private String email;

    // ==================== INFORMATIONS LÉGALES ====================

    /**
     * Statut juridique (SARL, SA, Auto-Entrepreneur, etc.)
     */
    @Column(length = 100)
    private String legalStatus;

    /**
     * Numéro TVA intracommunautaire
     */
    @Column(length = 50)
    private String tva;

    /**
     * Prénom du représentant légal
     */
    @Column(length = 100)
    private String legalRepFirstName;

    /**
     * Nom du représentant légal
     */
    @Column(length = 100)
    private String legalRepLastName;

    /**
     * Position/Fonction du représentant légal
     */
    @Column(length = 100)
    private String position;

    // ==================== INFORMATIONS BANCAIRES ====================

    /**
     * Nom du titulaire du compte bancaire
     */
    @Column(length = 100)
    private String accountHolderName;

    /**
     * IBAN du compte bancaire
     */
    @Column(length = 50)
    private String iban;

    /**
     * Nom de la banque
     */
    @Column(length = 100)
    private String bankName;

    /**
     * Devise du compte (TND, EUR, USD, etc.)
     */
    @Column(length = 10)
    @Builder.Default
    private String currency = "TND";

    // ==================== ADRESSE ====================

    /**
     * Adresse complète (rue)
     */
    @Column(length = 255)
    private String address;

    /**
     * Ville
     */
    @Column(length = 100)
    private String city;

    /**
     * Code postal
     */
    @Column(length = 20)
    private String postalCode;

    /**
     * Gouvernorat/Région
     */
    @Column(length = 100)
    private String state;

    /**
     * Pays
     */
    @Column(length = 100)
    @Builder.Default
    private String country = "Tunisie";

    // ==================== LOCALISATION GPS ====================

    /**
     * Latitude GPS
     */
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * Longitude GPS
     */
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * Rayon de livraison (en mètres)
     */
    @Builder.Default
    private Integer deliveryRadius = 5000;

    // ==================== STATUT ET DISPONIBILITÉ ====================

    /**
     * Statut du compte partenaire
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.PENDING;

    /**
     * Partenaire actif (peut apparaître dans les recherches)
     */
    @Builder.Default
    private Boolean isActive = false;

    /**
     * Accepte actuellement les commandes
     */
    @Builder.Default
    private Boolean acceptsOrders = false;

    /**
     * Partenaire vérifié (badge de confiance)
     */
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Partenaire premium (mise en avant)
     */
    @Builder.Default
    private Boolean isPremium = false;

    /**
     * Partenaire en vedette (featured)
     */
    @Builder.Default
    private Boolean isFeatured = false;

    // ==================== HORAIRES ====================

    /**
     * Horaires d'ouverture (JSON stocké en String)
     * Format: [{"dayOfWeek":1,"openTime":"08:00","closeTime":"22:00"},...]
     */
    @Column(columnDefinition = "TEXT")
    private String openingHoursJson;

    /**
     * Exceptions d'horaires : jours fériés et fermetures exceptionnelles (JSON)
     * Format: [{"date":"2025-12-25","label":"Noël","type":"CLOSED"}, ...]
     */
    @Column(columnDefinition = "TEXT")
    private String scheduleExceptionsJson;

    /**
     * Actuellement ouvert (calculé)
     */
    @Transient
    private Boolean isCurrentlyOpen;

    // ==================== PARAMÈTRES DE LIVRAISON ====================

    /**
     * Temps de préparation moyen (en minutes)
     */
    @Builder.Default
    private Integer preparationTime = 30;

    /**
     * Frais de livraison de base
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    /**
     * Commande minimum pour livraison
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumOrder = BigDecimal.ZERO;

    /**
     * Livraison gratuite à partir de ce montant (null = pas de livraison gratuite)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    // ==================== MODES DE PAIEMENT ====================

    /**
     * Accepte le paiement en ligne (carte bancaire, etc.)
     */
    @Builder.Default
    private Boolean acceptOnlinePayment = true;

    /**
     * Accepte le paiement en espèces à la livraison
     */
    @Builder.Default
    private Boolean acceptCashPayment = true;

    // ==================== STATISTIQUES ====================

    /**
     * Note moyenne (1-5)
     */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Nombre total d'évaluations
     */
    @Builder.Default
    private Integer totalRatings = 0;

    /**
     * Nombre total de commandes
     */
    @Builder.Default
    private Integer totalOrders = 0;

    /**
     * Chiffre d'affaires total
     */
    @Column(precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    // ==================== COMMISSION ====================

    /**
     * Type de commission (PERCENTAGE ou MARKUP)
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommissionType commissionType = CommissionType.PERCENTAGE;

    /**
     * Taux de commission SpeedLine (en %)
     */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("15.00");

    // ==================== DOCUMENTS ====================

    /**
     * URL du document KBIS
     */
    @Column(length = 500)
    private String kbisUrl;

    /**
     * URL de la carte d'identité du représentant légal
     */
    @Column(length = 500)
    private String idCardUrl;

    /**
     * URL de l'attestation d'assurance
     */
    @Column(length = 500)
    private String insuranceUrl;

    /**
     * URL du RIB (Relevé d'Identité Bancaire)
     */
    @Column(length = 500)
    private String ribUrl;

    /**
     * URLs des photos du partenaire (JSON array)
     */
    @Column(columnDefinition = "TEXT")
    private String photosJson;

    /**
     * Notes internes (admin/support uniquement)
     */
    @Column(length = 1000)
    private String internalNotes;

    // ==================== CATÉGORIES ====================

    /**
     * Catégories du partenaire (IDs séparés par virgule)
     * Ex: "1,5,12" pour Restaurant, Italien, Pizza
     */
    @Column(length = 500)
    private String categoryIds;

    /**
     * Tags pour la recherche (séparés par virgule)
     * Ex: "pizza,italien,livraison rapide"
     */
    @Column(length = 500)
    private String tags;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Dernière connexion du propriétaire
     */
    private LocalDateTime lastLoginAt;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Générer le slug à partir du nom commercial
     */
    @PrePersist
    public void generateSlug() {
        if (this.slug == null && this.businessName != null) {
            this.slug = this.businessName.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    + "-" + System.currentTimeMillis() % 10000;
        }
    }

    /**
     * Incrémenter le compteur de commandes
     */
    public void incrementOrderCount(BigDecimal orderAmount) {
        this.totalOrders++;
        this.totalRevenue = this.totalRevenue.add(orderAmount);
    }

    /**
     * Mettre à jour la note moyenne
     */
    public void updateRating(BigDecimal newRating) {
        BigDecimal totalScore = this.rating.multiply(BigDecimal.valueOf(this.totalRatings));
        this.totalRatings++;
        this.rating = totalScore.add(newRating).divide(BigDecimal.valueOf(this.totalRatings), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Vérifier si le partenaire peut recevoir des commandes
     */
    public boolean canAcceptOrders() {
        return isActive && acceptsOrders && status == PartnerStatus.ACTIVE;
    }

    /**
     * Calculer les frais de livraison pour une commande
     */
    public BigDecimal calculateDeliveryFee(BigDecimal orderAmount) {
        if (freeDeliveryThreshold != null && orderAmount.compareTo(freeDeliveryThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return deliveryFee;
    }

    /**
     * Vérifier si la commande atteint le minimum
     */
    public boolean meetsMinimumOrder(BigDecimal orderAmount) {
        return orderAmount.compareTo(minimumOrder) >= 0;
    }
}
