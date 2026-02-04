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
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Customer - Profil client de l'application
 * Lié à User dans auth-service via userId
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_user_id", columnList = "userId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur dans auth-service
     * PAS de FK - communication inter-service
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * Solde du portefeuille client (pour paiements rapides)
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    /**
     * Points de fidélité accumulés
     */
    @Builder.Default
    private Integer loyaltyPoints = 0;

    /**
     * Nombre total de commandes passées
     */
    @Builder.Default
    private Integer totalOrders = 0;

    /**
     * Montant total dépensé
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    /**
     * Note moyenne donnée par le client
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRatingGiven;

    /**
     * Date de la dernière commande
     */
    private LocalDateTime lastOrderDate;

    /**
     * Préférences du client (embedded)
     */
    @Embedded
    @Builder.Default
    private CustomerPreferences preferences = new CustomerPreferences();

    /**
     * Partenaires favoris (IDs séparés par virgule)
     * Format: "1,5,12,45"
     */
    @Column(length = 1000)
    private String favoritePartnerIds;

    /**
     * Produits favoris (IDs séparés par virgule)
     */
    @Column(length = 1000)
    private String favoriteProductIds;

    /**
     * Statut du compte client
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * Client VIP (avantages spéciaux)
     */
    @Builder.Default
    private Boolean isVip = false;

    /**
     * Niveau VIP (BRONZE, SILVER, GOLD, PLATINUM)
     */
    @Enumerated(EnumType.STRING)
    private VipLevel vipLevel;

    /**
     * Code de parrainage unique du client
     */
    @Column(length = 20, unique = true)
    private String referralCode;

    /**
     * ID du client qui a parrainé ce client
     */
    private Long referredByCustomerId;

    /**
     * Nombre de parrainages réussis
     */
    @Builder.Default
    private Integer successfulReferrals = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Adresses du client (OneToMany)
     */
    @OneToMany(mappedBy = "customerId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    // ==================== ENUMS ====================

    public enum CustomerStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        DELETED
    }

    public enum VipLevel {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Ajouter des points de fidélité
     */
    public void addLoyaltyPoints(int points) {
        this.loyaltyPoints += points;
        updateVipLevel();
    }

    /**
     * Utiliser des points de fidélité
     */
    public boolean useLoyaltyPoints(int points) {
        if (this.loyaltyPoints >= points) {
            this.loyaltyPoints -= points;
            return true;
        }
        return false;
    }

    /**
     * Mettre à jour le niveau VIP basé sur les points
     */
    private void updateVipLevel() {
        if (loyaltyPoints >= 10000) {
            this.vipLevel = VipLevel.PLATINUM;
            this.isVip = true;
        } else if (loyaltyPoints >= 5000) {
            this.vipLevel = VipLevel.GOLD;
            this.isVip = true;
        } else if (loyaltyPoints >= 2000) {
            this.vipLevel = VipLevel.SILVER;
            this.isVip = true;
        } else if (loyaltyPoints >= 500) {
            this.vipLevel = VipLevel.BRONZE;
            this.isVip = true;
        }
    }

    /**
     * Incrémenter le compteur de commandes
     */
    public void incrementOrderCount(BigDecimal orderAmount) {
        this.totalOrders++;
        this.totalSpent = this.totalSpent.add(orderAmount);
        this.lastOrderDate = LocalDateTime.now();
    }
}
