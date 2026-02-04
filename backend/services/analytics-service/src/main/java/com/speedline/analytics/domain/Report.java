package com.speedline.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entité Report - Rapports générés
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_type", columnList = "type"),
    @Index(name = "idx_report_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type de rapport
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    /**
     * Paramètres de génération (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String parametersJson;

    /**
     * Chemin du fichier généré
     */
    @Column(length = 500)
    private String filePath;

    /**
     * Nom du fichier
     */
    @Column(length = 255)
    private String fileName;

    /**
     * Taille du fichier (en bytes)
     */
    private Long fileSize;

    /**
     * Format du fichier (PDF, CSV, XLSX)
     */
    @Column(length = 10)
    private String fileFormat;

    /**
     * Statut de génération
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    /**
     * ID de l'utilisateur qui a demandé le rapport
     */
    private Long requestedBy;

    /**
     * Date de génération
     */
    private LocalDateTime generatedAt;

    /**
     * Date d'expiration (pour nettoyage automatique)
     */
    private LocalDateTime expiresAt;

    /**
     * Message d'erreur (si échec)
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ReportType {
        REVENUE,
        ORDERS,
        PARTNERS,
        COURIERS,
        CUSTOMERS,
        DELIVERY_PERFORMANCE,
        PRODUCT_SALES
    }

    public enum ReportStatus {
        PENDING,
        GENERATING,
        COMPLETED,
        FAILED,
        EXPIRED
    }
}
