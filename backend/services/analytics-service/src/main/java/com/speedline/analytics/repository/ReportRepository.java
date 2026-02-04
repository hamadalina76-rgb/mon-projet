package com.speedline.analytics.repository;

import com.speedline.analytics.domain.Report;
import com.speedline.analytics.domain.Report.ReportStatus;
import com.speedline.analytics.domain.Report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Report
 * Note: Implémentation manuelle nécessaire car JPA est désactivé
 * TODO: Réactiver JPA ou implémenter avec JDBC pour PostgreSQL
 */
public interface ReportRepository {

    // Méthodes de base CrudRepository
    <S extends Report> S save(S entity);
    <S extends Report> Iterable<S> saveAll(Iterable<S> entities);
    Optional<Report> findById(Long id);
    boolean existsById(Long id);
    Iterable<Report> findAll();
    Iterable<Report> findAllById(Iterable<Long> ids);
    long count();
    void deleteById(Long id);
    void delete(Report entity);
    void deleteAllById(Iterable<? extends Long> ids);
    void deleteAll(Iterable<? extends Report> entities);
    void deleteAll();

    // Méthodes spécifiques

    /**
     * Trouver les rapports d'un utilisateur
     */
    Page<Report> findByRequestedBy(Long requestedBy, Pageable pageable);

    /**
     * Trouver les rapports par type
     */
    Page<Report> findByType(ReportType type, Pageable pageable);

    /**
     * Trouver les rapports par statut (avec pagination)
     */
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * Trouver les rapports expirés
     */
    List<Report> findExpiredReports(LocalDateTime now);

    /**
     * Trouver les rapports en attente de génération
     */
    List<Report> findByStatus(ReportStatus status);

    /**
     * Mettre à jour le statut
     */
    int updateStatus(Long reportId, ReportStatus status);

    /**
     * Marquer comme complété avec le chemin du fichier
     */
    int markAsCompleted(Long reportId, String filePath, String fileName, Long fileSize);

    /**
     * Marquer comme échoué
     */
    int markAsFailed(Long reportId, String errorMessage);

    /**
     * Supprimer les rapports expirés
     */
    int deleteExpiredReports(LocalDateTime now);
}
