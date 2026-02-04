package com.speedline.analytics.repository;

import com.speedline.analytics.domain.Analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour Analytics (ClickHouse)
 * Note: ClickHouse utilise JDBC, pas JPA standard
 * Cette interface sera implémentée avec ClickHouse JDBC
 */
public interface AnalyticsRepository {

    /**
     * Sauvegarder une entité Analytics
     */
    <S extends Analytics> S save(S entity);

    /**
     * Sauvegarder plusieurs entités
     */
    <S extends Analytics> Iterable<S> saveAll(Iterable<S> entities);

    /**
     * Trouver par ID
     */
    Optional<Analytics> findById(Long id);

    /**
     * Vérifier si existe par ID
     */
    boolean existsById(Long id);

    /**
     * Trouver toutes les entités
     */
    Iterable<Analytics> findAll();

    /**
     * Trouver toutes les entités par IDs
     */
    Iterable<Analytics> findAllById(Iterable<Long> ids);

    /**
     * Compter toutes les entités
     */
    long count();

    /**
     * Supprimer par ID
     */
    void deleteById(Long id);

    /**
     * Supprimer une entité
     */
    void delete(Analytics entity);

    /**
     * Supprimer toutes les entités par IDs
     */
    void deleteAllById(Iterable<? extends Long> ids);

    /**
     * Supprimer toutes les entités
     */
    void deleteAll(Iterable<? extends Analytics> entities);

    /**
     * Supprimer toutes les entités
     */
    void deleteAll();

    /**
     * Trouver les analytics par date
     */
    List<Analytics> findByDate(LocalDate date);

    /**
     * Trouver les analytics d'un partenaire
     */
    List<Analytics> findByPartnerIdAndDateBetween(Long partnerId, LocalDate startDate, LocalDate endDate);

    /**
     * Trouver les analytics d'une commande
     */
    List<Analytics> findByOrderId(Long orderId);

    /**
     * Compter les commandes par statut
     */
    long countByStatus(String status);
}
