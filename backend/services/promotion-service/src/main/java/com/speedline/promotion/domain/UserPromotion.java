package com.speedline.promotion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entité UserPromotion - Utilisation d'une promotion par un utilisateur
 */
@Entity
@Table(name = "user_promotions", indexes = {
    @Index(name = "idx_user_promo_user", columnList = "userId"),
    @Index(name = "idx_user_promo_promo", columnList = "promotionId"),
    @Index(name = "idx_user_promo_unique", columnList = "userId, promotionId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence vers l'utilisateur (auth-service)
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * Référence vers la promotion
     */
    @Column(nullable = false)
    private Long promotionId;

    /**
     * Nombre de fois utilisée par cet utilisateur
     */
    @Builder.Default
    private Integer usageCount = 0;

    /**
     * Date de première utilisation
     */
    private LocalDateTime firstUsedAt;

    /**
     * Date de dernière utilisation
     */
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Incrémenter l'utilisation
     */
    public void incrementUsage() {
        this.usageCount++;
        LocalDateTime now = LocalDateTime.now();
        if (this.firstUsedAt == null) {
            this.firstUsedAt = now;
        }
        this.lastUsedAt = now;
    }

    /**
     * Vérifier si l'utilisateur peut encore utiliser cette promotion
     * (basé sur usageLimitPerUser de la promotion)
     */
    public boolean canUse(int maxUsagePerUser) {
        return maxUsagePerUser == 0 || this.usageCount < maxUsagePerUser;
    }
}
