package com.speedline.notification.repository;

import com.speedline.notification.domain.PushToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour PushToken (MongoDB)
 */
@Repository
public interface PushTokenRepository extends MongoRepository<PushToken, String> {

    /**
     * Trouver les tokens d'un utilisateur
     */
    List<PushToken> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Trouver un token par valeur
     */
    Optional<PushToken> findByToken(String token);

    /**
     * Trouver les tokens par type de device
     */
    List<PushToken> findByUserIdAndDeviceTypeAndIsActiveTrue(Long userId, PushToken.DeviceType deviceType);

    /**
     * Désactiver tous les tokens d'un utilisateur
     */
    void deleteByUserId(Long userId);

    /**
     * Désactiver un token spécifique
     */
    void deleteByToken(String token);

    /**
     * Compter les tokens actifs d'un utilisateur
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * Trouver les tokens par deviceId
     */
    Optional<PushToken> findByDeviceId(String deviceId);
}
