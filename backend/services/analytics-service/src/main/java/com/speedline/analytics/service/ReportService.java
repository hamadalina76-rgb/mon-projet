package com.speedline.analytics.service;

import com.speedline.analytics.domain.Report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service pour la génération de rapports
 */
public interface ReportService {

    /**
     * Générer un rapport
     * 
     * @param type Type de rapport (REVENUE, ORDERS, PARTNERS, etc.)
     * @param parameters Paramètres de génération (dates, filtres, etc.)
     * @param requestedBy ID de l'utilisateur demandeur
     * @param format Format du fichier (PDF, CSV, XLSX)
     * @return ReportDTO avec id, status=PENDING
     * @throws InvalidReportParametersException si les paramètres sont invalides
     */
    ReportDTO generateReport(ReportType type, ReportParameters parameters, 
                              Long requestedBy, String format);

    /**
     * Récupérer un rapport par ID
     * 
     * @param reportId ID du rapport
     * @return ReportDTO complet
     * @throws ReportNotFoundException si le rapport n'existe pas
     */
    ReportDTO getReportById(Long reportId);

    /**
     * Télécharger un rapport
     * 
     * @param reportId ID du rapport
     * @return byte[] contenu du fichier
     * @throws ReportNotFoundException si le rapport n'existe pas
     * @throws ReportNotReadyException si le rapport n'est pas encore généré
     */
    byte[] downloadReport(Long reportId);

    /**
     * Obtenir l'historique des rapports d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Pagination
     * @return Page<ReportDTO>
     */
    Page<ReportDTO> getUserReports(Long userId, Pageable pageable);

    /**
     * Obtenir tous les rapports
     * 
     * @param pageable Pagination
     * @return Page<ReportDTO>
     */
    Page<ReportDTO> getAllReports(Pageable pageable);

    /**
     * Supprimer un rapport
     * 
     * @param reportId ID du rapport
     * @throws ReportNotFoundException si le rapport n'existe pas
     */
    void deleteReport(Long reportId);

    /**
     * Nettoyer les rapports expirés
     * Appelé par un scheduler
     */
    void cleanupExpiredReports();

    /**
     * DTO pour les rapports
     */
    record ReportDTO(
            Long id,
            ReportType type,
            String fileName,
            String filePath,
            Long fileSize,
            String fileFormat,
            com.speedline.analytics.domain.Report.ReportStatus status,
            Long requestedBy,
            java.time.LocalDateTime generatedAt,
            java.time.LocalDateTime expiresAt
    ) {}

    /**
     * DTO pour les paramètres de rapport
     */
    record ReportParameters(
            LocalDate startDate,
            LocalDate endDate,
            Long partnerId,
            Long courierId,
            List<String> filters,
            String groupBy
    ) {}
}
