package com.speedline.analytics.repository.impl;

import com.speedline.analytics.domain.Analytics;
import com.speedline.analytics.repository.AnalyticsRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation basique du repository Analytics pour ClickHouse
 * TODO: Implémenter avec ClickHouse JDBC une fois la configuration prête
 */
@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    // Note: Cette implémentation est temporaire pour permettre le démarrage du service
    // Les méthodes devront être implémentées avec ClickHouse JDBC plus tard

    @Override
    public <S extends Analytics> S save(S entity) {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public <S extends Analytics> Iterable<S> saveAll(Iterable<S> entities) {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public Optional<Analytics> findById(Long aLong) {
        // TODO: Implémenter avec ClickHouse JDBC
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long aLong) {
        // TODO: Implémenter avec ClickHouse JDBC
        return false;
    }

    @Override
    public Iterable<Analytics> findAll() {
        // TODO: Implémenter avec ClickHouse JDBC
        return new ArrayList<>();
    }

    @Override
    public Iterable<Analytics> findAllById(Iterable<Long> longs) {
        // TODO: Implémenter avec ClickHouse JDBC
        return new ArrayList<>();
    }

    @Override
    public long count() {
        // TODO: Implémenter avec ClickHouse JDBC
        return 0;
    }

    @Override
    public void deleteById(Long aLong) {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public void delete(Analytics entity) {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public void deleteAll(Iterable<? extends Analytics> entities) {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public void deleteAll() {
        // TODO: Implémenter avec ClickHouse JDBC
        throw new UnsupportedOperationException("À implémenter avec ClickHouse JDBC");
    }

    @Override
    public List<Analytics> findByDate(LocalDate date) {
        // TODO: Implémenter avec ClickHouse JDBC
        return new ArrayList<>();
    }

    @Override
    public List<Analytics> findByPartnerIdAndDateBetween(Long partnerId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implémenter avec ClickHouse JDBC
        return new ArrayList<>();
    }

    @Override
    public List<Analytics> findByOrderId(Long orderId) {
        // TODO: Implémenter avec ClickHouse JDBC
        return new ArrayList<>();
    }

    @Override
    public long countByStatus(String status) {
        // TODO: Implémenter avec ClickHouse JDBC
        return 0;
    }
}
