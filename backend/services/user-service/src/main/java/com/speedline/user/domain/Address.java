package com.speedline.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entité Address - Adresses de livraison des clients
 */
@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_address_customer", columnList = "customerId"),
    @Index(name = "idx_address_user", columnList = "userId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers le client (dans ce service)
     */
    @Column(nullable = false)
    private Long customerId;

    /**
     * Référence vers l'utilisateur (auth-service)
     * Utile pour les requêtes directes
     */
    @Column(nullable = false)
    private Long userId;

    // ==================== TYPE ET LABEL ====================

    /**
     * Type d'adresse (HOME, WORK, OTHER)
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AddressType type = AddressType.HOME;

    /**
     * Label personnalisé (ex: "Chez maman", "Bureau centre-ville")
     */
    @Column(length = 100)
    private String label;

    // ==================== ADRESSE COMPLÈTE ====================

    /**
     * Numéro et nom de rue
     */
    @Column(nullable = false, length = 255)
    private String street;

    /**
     * Nom ou numéro du bâtiment
     */
    @Column(length = 100)
    private String building;

    /**
     * Étage
     */
    @Column(length = 50)
    private String floor;

    /**
     * Numéro d'appartement
     */
    @Column(length = 50)
    private String apartment;

    /**
     * Code d'accès à l'immeuble
     */
    @Column(length = 50)
    private String accessCode;

    /**
     * Ville
     */
    @Column(nullable = false, length = 100)
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

    // ==================== COORDONNÉES GPS ====================

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
     * Adresse formatée complète (générée)
     */
    @Column(length = 500)
    private String formattedAddress;

    /**
     * ID de lieu Google/Mapbox (pour précision)
     */
    @Column(length = 255)
    private String placeId;

    // ==================== INSTRUCTIONS ====================

    /**
     * Instructions de livraison
     */
    @Column(length = 500)
    private String deliveryInstructions;

    /**
     * Point de repère pour trouver l'adresse
     */
    @Column(length = 255)
    private String landmark;

    /**
     * Numéro de téléphone pour cette adresse (si différent)
     */
    @Column(length = 20)
    private String contactPhone;

    /**
     * Nom du contact (si différent du client)
     */
    @Column(length = 100)
    private String contactName;

    // ==================== FLAGS ====================

    /**
     * Adresse par défaut
     */
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Adresse vérifiée (GPS confirmé)
     */
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Adresse active (non supprimée)
     */
    @Builder.Default
    private Boolean isActive = true;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Dernière utilisation de cette adresse
     */
    private LocalDateTime lastUsedAt;

    /**
     * Nombre de fois utilisée
     */
    @Builder.Default
    private Integer usageCount = 0;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Générer l'adresse formatée si elle n'est pas déjà définie.
     * Le service peut fournir une adresse GPS/Mapbox ; dans ce cas on la conserve.
     */
    @PrePersist
    @PreUpdate
    public void generateFormattedAddress() {
        if (formattedAddress != null && !formattedAddress.isBlank()) {
            return; // déjà définie par le service — ne pas écraser
        }
        // Combine city + postal code as a single space-separated token
        String cityPostal = Stream.of(city, postalCode)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "));
        this.formattedAddress = Stream.of(
                        street,
                        building,
                        floor    != null && !floor.isBlank()    ? "Étage " + floor    : null,
                        apartment != null && !apartment.isBlank() ? "Apt "   + apartment : null,
                        cityPostal.isBlank() ? null : cityPostal,
                        country
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    /**
     * Marquer comme utilisée
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.usageCount++;
    }

    /**
     * Vérifier si les coordonnées GPS sont définies
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Obtenir les coordonnées sous forme de chaîne
     */
    public String getCoordinatesString() {
        if (!hasCoordinates()) return null;
        return latitude + "," + longitude;
    }
}
