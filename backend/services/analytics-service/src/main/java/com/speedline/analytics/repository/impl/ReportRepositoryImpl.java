package com.speedline.analytics.repository.impl;

import com.speedline.analytics.domain.Report;
import com.speedline.analytics.domain.Report.ReportStatus;
import com.speedline.analytics.domain.Report.ReportType;
import com.speedline.analytics.repository.ReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation basique du repository Report
 * TODO: Implémenter avec JDBC pour PostgreSQL une fois la configuration prête
 */
@Repository
public class ReportRepositoryImpl implements ReportRepository {

    // Note: Cette implémentation est temporaire pour permettre le démarrage du service
    // Les méthodes devront être implémentées avec JDBC pour PostgreSQL plus tard

    @Override
    public <S extends Report> S save(S entity) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public <S extends Report> Iterable<S> saveAll(Iterable<S> entities) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public Optional<Report> findById(Long id) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long id) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return false;
    }

    @Override
    public Iterable<Report> findAll() {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new ArrayList<>();
    }

    @Override
    public Iterable<Report> findAllById(Iterable<Long> ids) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new ArrayList<>();
    }

    @Override
    public long count() {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return 0;
    }

    @Override
    public void deleteById(Long id) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public void delete(Report entity) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public void deleteAll(Iterable<? extends Report> entities) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public void deleteAll() {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public Page<Report> findByRequestedBy(Long requestedBy, Pageable pageable) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new PageImpl<>(new ArrayList<>());
    }

    @Override
    public Page<Report> findByType(ReportType type, Pageable pageable) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new PageImpl<>(new ArrayList<>());
    }

    @Override
    public Page<Report> findByStatus(ReportStatus status, Pageable pageable) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new PageImpl<>(new ArrayList<>());
    }

    @Override
    public List<Report> findExpiredReports(LocalDateTime now) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new ArrayList<>();
    }

    @Override
    public List<Report> findByStatus(ReportStatus status) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        return new ArrayList<>();
    }

    @Override
    public int updateStatus(Long reportId, ReportStatus status) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public int markAsCompleted(Long reportId, String filePath, String fileName, Long fileSize) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public int markAsFailed(Long reportId, String errorMessage) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }

    @Override
    public int deleteExpiredReports(LocalDateTime now) {
        // TODO: Implémenter avec JDBC pour PostgreSQL
        throw new UnsupportedOperationException("À implémenter avec JDBC pour PostgreSQL");
    }
}
