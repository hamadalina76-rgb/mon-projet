package com.speedline.location.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Adresse client confirmée depuis l'app mobile (GPS ou saisie manuelle).
 * Table: customer_locations
 */
@Entity
@Table(name = "customer_locations", indexes = {
    @Index(name = "idx_customer_locations_user_id",     columnList = "user_id"),
    @Index(name = "idx_customer_locations_user_default", columnList = "user_id, is_default")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur (JWT subject ou "anonymous") */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /** Adresse complète formatée */
    @Column(name = "formatted_address", nullable = false, length = 500)
    private String formattedAddress;

    @Column(length = 255)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    /** HOME / WORK / OTHER */
    @Column(name = "address_type", length = 50)
    private String addressType;

    @Column(name = "custom_label", length = 100)
    private String customLabel;

    @Column(name = "is_default")
    private Boolean isDefault;

    /** Quand l'utilisateur a confirmé l'adresse */
    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
