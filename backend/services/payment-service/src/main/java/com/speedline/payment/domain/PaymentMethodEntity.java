package com.speedline.payment.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entité PaymentMethod - Moyens de paiement enregistrés
 */
@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_method_user", columnList = "user_id"),
    @Index(name = "idx_payment_method_default", columnList = "user_id, is_default")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur (auth-service)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ==================== TYPE ====================

    /**
     * Type de moyen de paiement
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType type;

    // ==================== INFORMATIONS CARTE ====================

    /**
     * 4 derniers chiffres de la carte
     */
    @Column(name = "last4_digits", length = 4)
    private String last4Digits;

    /**
     * Marque de la carte (VISA, MASTERCARD, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand")
    private CardBrand cardBrand;

    /**
     * Date d'expiration (MM/YY)
     */
    @Column(name = "expiry_date", length = 7)
    private String expiryDate;

    /**
     * Mois d'expiration
     */
    @Column(name = "expiry_month")
    private Integer expiryMonth;

    /**
     * Année d'expiration
     */
    @Column(name = "expiry_year")
    private Integer expiryYear;

    /**
     * Nom du titulaire de la carte
     */
    @Column(name = "cardholder_name", length = 255)
    private String cardholderName;

    // ==================== TOKEN ====================

    /**
     * Token Stripe pour cette carte
     */
    @Column(name = "stripe_payment_method_id", length = 500)
    private String stripePaymentMethodId;

    /**
     * Empreinte de la carte (pour détecter les doublons)
     */
    @Column(length = 255)
    private String fingerprint;

    // ==================== FLAGS ====================

    /**
     * Moyen de paiement par défaut
     */
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Moyen de paiement actif
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Carte vérifiée (3D Secure passé)
     */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    // ==================== BILLING ADDRESS ====================

    /**
     * Adresse de facturation (JSON)
     */
    @Column(name = "billing_address_json", columnDefinition = "TEXT")
    private String billingAddressJson;

    // ==================== TIMESTAMPS ====================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Dernière utilisation
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // ==================== ENUMS ====================

    public enum PaymentMethodType {
        CARD,
        BANK_ACCOUNT
    }

    public enum CardBrand {
        VISA,
        MASTERCARD,
        AMERICAN_EXPRESS,
        DISCOVER,
        UNKNOWN
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifier si la carte est expirée
     */
    public boolean isExpired() {
        if (expiryMonth == null || expiryYear == null) return false;
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear() % 100; // 2 derniers chiffres
        int currentMonth = now.getMonthValue();
        return expiryYear < currentYear || (expiryYear == currentYear && expiryMonth < currentMonth);
    }

    /**
     * Obtenir un affichage masqué de la carte
     */
    public String getMaskedCardNumber() {
        return "**** **** **** " + last4Digits;
    }

    /**
     * Marquer comme utilisée
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
