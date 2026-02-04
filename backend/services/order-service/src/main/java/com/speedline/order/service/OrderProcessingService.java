package com.speedline.order.service;

import com.speedline.order.dto.CreateOrderRequest;
import com.speedline.order.dto.OrderResponse;

/**
 * Service pour le traitement et la validation des commandes
 * 
 * Ce service gère la logique métier complexe :
 * - Validation des commandes
 * - Orchestration du workflow
 * - Vérification de la disponibilité
 */
public interface OrderProcessingService {

    // ==================== VALIDATION ====================

    /**
     * Valider une demande de commande avant création
     * Vérifie tous les prérequis
     * 
     * @param request CreateOrderRequest à valider
     * @throws CustomerNotFoundException si le client n'existe pas
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     * @throws PartnerNotAvailableException si le partenaire n'accepte pas de commandes
     * @throws PartnerClosedException si le partenaire est fermé
     * @throws ProductNotFoundException si un produit n'existe pas
     * @throws ProductNotAvailableException si un produit n'est pas disponible
     * @throws InsufficientStockException si le stock est insuffisant
     * @throws InvalidOptionSelectionException si une sélection d'option est invalide
     * @throws AddressNotFoundException si l'adresse n'existe pas
     * @throws AddressNotInDeliveryZoneException si l'adresse n'est pas dans la zone
     * @throws MinimumOrderNotMetException si le minimum n'est pas atteint
     */
    void validateOrder(CreateOrderRequest request);

    /**
     * Vérifier si le partenaire peut accepter une nouvelle commande
     * 
     * @param partnerId ID du partenaire
     * @return boolean true si le partenaire peut accepter
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    boolean canPartnerAcceptOrder(Long partnerId);

    /**
     * Vérifier si un produit est disponible
     * 
     * @param productId ID du produit
     * @param quantity Quantité demandée
     * @return boolean true si disponible
     * @throws ProductNotFoundException si le produit n'existe pas
     */
    boolean isProductAvailable(Long productId, int quantity);

    /**
     * Vérifier si une adresse est dans la zone de livraison du partenaire
     * 
     * @param partnerId ID du partenaire
     * @param addressId ID de l'adresse
     * @return boolean true si dans la zone
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     * @throws AddressNotFoundException si l'adresse n'existe pas
     */
    boolean isAddressInDeliveryZone(Long partnerId, Long addressId);

    // ==================== TRAITEMENT ====================

    /**
     * Traiter une commande validée
     * - Décrémenter les stocks
     * - Envoyer les notifications
     * - Publier les événements Kafka
     * 
     * @param orderId ID de la commande créée
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    void processOrder(Long orderId);

    /**
     * Traiter le paiement d'une commande
     * 
     * @param orderId ID de la commande
     * @param paymentMethodId ID de la méthode de paiement (pour CARD)
     * @return boolean true si le paiement est réussi
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws PaymentFailedException si le paiement échoue
     */
    boolean processPayment(Long orderId, Long paymentMethodId);

    /**
     * Traiter l'annulation d'une commande
     * - Rembourser si nécessaire
     * - Restaurer les stocks
     * - Notifier les parties
     * 
     * @param orderId ID de la commande
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    void processCancellation(Long orderId);

    /**
     * Traiter la livraison d'une commande
     * - Mettre à jour les statistiques
     * - Ajouter les points de fidélité
     * - Notifier les parties
     * 
     * @param orderId ID de la commande
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    void processDelivery(Long orderId);

    // ==================== WORKFLOW ====================

    /**
     * Vérifier si une transition de statut est valide
     * 
     * @param currentStatus Statut actuel
     * @param newStatus Nouveau statut souhaité
     * @return boolean true si la transition est valide
     */
    boolean isValidStatusTransition(String currentStatus, String newStatus);

    /**
     * Obtenir les prochains statuts possibles
     * 
     * @param currentStatus Statut actuel
     * @return List<String> statuts possibles
     */
    java.util.List<String> getNextPossibleStatuses(String currentStatus);

    // ==================== ESTIMATION ====================

    /**
     * Estimer le temps de livraison
     * 
     * @param partnerId ID du partenaire
     * @param deliveryLatitude Latitude de livraison
     * @param deliveryLongitude Longitude de livraison
     * @return Integer temps estimé en minutes
     */
    Integer estimateDeliveryTime(Long partnerId, java.math.BigDecimal deliveryLatitude, 
                                  java.math.BigDecimal deliveryLongitude);

    /**
     * Calculer l'heure de livraison estimée
     * 
     * @param orderId ID de la commande
     * @return LocalDateTime heure estimée
     * @throws OrderNotFoundException si la commande n'existe pas
     */
    java.time.LocalDateTime calculateEstimatedDeliveryTime(Long orderId);

    // ==================== RETRY ====================

    /**
     * Réessayer le traitement d'une commande échouée
     * 
     * @param orderId ID de la commande
     * @return OrderResponse mis à jour
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws OrderNotRetryableException si la commande ne peut pas être réessayée
     */
    OrderResponse retryOrder(Long orderId);
}
