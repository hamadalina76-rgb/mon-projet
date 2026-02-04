package com.speedline.delivery.service.impl;

import com.speedline.delivery.dto.DeliveryDTO;
import com.speedline.delivery.repository.DeliveryRepository;
import com.speedline.delivery.service.CourierMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implémentation du service de matching livreur-commande.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierMatchingServiceImpl implements CourierMatchingService {

    private final DeliveryRepository deliveryRepository;
    // TODO: Injecter UserServiceClient / LocationServiceClient pour récupérer les livreurs et leurs positions

    @Override
    @Transactional(readOnly = true)
    public Long findBestCourier(Long deliveryId) {
        // TODO: Implémenter l'algorithme pour trouver le meilleur livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAvailableCouriers(BigDecimal pickupLatitude, BigDecimal pickupLongitude, double radiusKm) {
        // TODO: Implémenter la recherche de livreurs disponibles autour du pickup
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO autoAssignCourier(Long deliveryId) {
        // TODO: Implémenter l'assignation automatique d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO assignCourier(Long deliveryId, Long courierId) {
        // TODO: Implémenter l'assignation manuelle d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateCourierScore(Long courierId, Long deliveryId) {
        // TODO: Implémenter le calcul du score d'un livreur pour une livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional
    public DeliveryDTO reassignDelivery(Long deliveryId) {
        // TODO: Implémenter la réassignation de la livraison à un autre livreur
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCourierAcceptDelivery(Long courierId) {
        // TODO: Implémenter la vérification si un livreur peut accepter une nouvelle livraison
        throw new UnsupportedOperationException("À implémenter");
    }

    @Override
    @Transactional(readOnly = true)
    public int getCourierWorkload(Long courierId) {
        // TODO: Implémenter le calcul de la charge de travail actuelle d'un livreur
        throw new UnsupportedOperationException("À implémenter");
    }
}

