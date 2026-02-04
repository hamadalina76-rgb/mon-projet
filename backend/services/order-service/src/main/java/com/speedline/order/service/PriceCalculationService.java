package com.speedline.order.service;

import com.speedline.order.dto.CreateOrderRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour le calcul des prix des commandes
 * 
 * Ce service gère tous les calculs financiers :
 * - Prix des articles avec options/suppléments
 * - Sous-total, frais, taxes
 * - Promotions et réductions
 * - Total final
 */
public interface PriceCalculationService {

    // ==================== CALCUL ARTICLE ====================

    /**
     * Calculer le prix d'un article avec ses options et suppléments
     * 
     * @param productId ID du produit
     * @param quantity Quantité
     * @param selectedOptionValueIds IDs des valeurs d'options sélectionnées
     * @param selectedAddons Suppléments sélectionnés (addonId:quantity)
     * @return BigDecimal prix total de l'article
     * @throws ProductNotFoundException si le produit n'existe pas
     * @throws InvalidOptionSelectionException si une sélection est invalide
     */
    BigDecimal calculateItemPrice(Long productId, int quantity, 
                                   List<Long> selectedOptionValueIds,
                                   List<CreateOrderRequest.AddonSelection> selectedAddons);

    /**
     * Calculer le prix unitaire d'un article (sans quantité)
     * 
     * @param productId ID du produit
     * @param selectedOptionValueIds IDs des valeurs d'options sélectionnées
     * @param selectedAddons Suppléments sélectionnés
     * @return BigDecimal prix unitaire avec modificateurs
     */
    BigDecimal calculateUnitPrice(Long productId, 
                                   List<Long> selectedOptionValueIds,
                                   List<CreateOrderRequest.AddonSelection> selectedAddons);

    /**
     * Calculer le total des modificateurs de prix (options + addons)
     * 
     * @param selectedOptionValueIds IDs des valeurs d'options
     * @param selectedAddons Suppléments sélectionnés
     * @return BigDecimal total des modificateurs
     */
    BigDecimal calculateModifiersTotal(List<Long> selectedOptionValueIds,
                                        List<CreateOrderRequest.AddonSelection> selectedAddons);

    // ==================== CALCUL COMMANDE ====================

    /**
     * Calculer le sous-total d'une commande (somme des articles)
     * 
     * @param request CreateOrderRequest avec les articles
     * @return BigDecimal sous-total
     */
    BigDecimal calculateSubtotal(CreateOrderRequest request);

    /**
     * Calculer les frais de livraison
     * 
     * @param partnerId ID du partenaire
     * @param deliveryAddressId ID de l'adresse de livraison
     * @param subtotal Sous-total de la commande
     * @return BigDecimal frais de livraison (0 si gratuit)
     */
    BigDecimal calculateDeliveryFee(Long partnerId, Long deliveryAddressId, BigDecimal subtotal);

    /**
     * Calculer les frais de service (commission SpeedLine)
     * 
     * @param subtotal Sous-total de la commande
     * @return BigDecimal frais de service
     */
    BigDecimal calculateServiceFee(BigDecimal subtotal);

    /**
     * Calculer la TVA
     * 
     * @param subtotal Sous-total de la commande
     * @param deliveryFee Frais de livraison
     * @param serviceFee Frais de service
     * @return BigDecimal montant de la TVA
     */
    BigDecimal calculateTax(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal serviceFee);

    // ==================== CALCUL PROMOTION ====================

    /**
     * Valider un code promo
     * 
     * @param promoCode Code promo
     * @param customerId ID du client
     * @param partnerId ID du partenaire
     * @param subtotal Sous-total de la commande
     * @return boolean true si le code est valide
     */
    boolean validatePromoCode(String promoCode, Long customerId, Long partnerId, BigDecimal subtotal);

    /**
     * Calculer la réduction pour un code promo
     * 
     * @param promoCode Code promo
     * @param subtotal Sous-total de la commande
     * @return BigDecimal montant de la réduction
     * @throws InvalidPromoCodeException si le code n'existe pas
     */
    BigDecimal calculateDiscount(String promoCode, BigDecimal subtotal);

    /**
     * Calculer la réduction avec les points de fidélité
     * 
     * @param loyaltyPoints Nombre de points à utiliser
     * @return BigDecimal valeur en TND (100 points = 1 TND)
     */
    BigDecimal calculateLoyaltyDiscount(int loyaltyPoints);

    // ==================== CALCUL TOTAL ====================

    /**
     * Calculer le total final d'une commande
     * 
     * @param subtotal Sous-total
     * @param deliveryFee Frais de livraison
     * @param serviceFee Frais de service
     * @param tax TVA
     * @param discount Réduction
     * @param tip Pourboire
     * @return BigDecimal total final
     */
    BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal deliveryFee, 
                               BigDecimal serviceFee, BigDecimal tax,
                               BigDecimal discount, BigDecimal tip);

    /**
     * Calculer tous les montants d'une commande
     * 
     * @param request CreateOrderRequest
     * @return OrderPriceBreakdown avec tous les montants détaillés
     */
    OrderPriceBreakdown calculateOrderPrice(CreateOrderRequest request);

    // ==================== COMMISSION ====================

    /**
     * Calculer la commission SpeedLine sur une commande
     * 
     * @param partnerId ID du partenaire
     * @param orderTotal Total de la commande
     * @return BigDecimal montant de la commission
     */
    BigDecimal calculateCommission(Long partnerId, BigDecimal orderTotal);

    /**
     * Calculer le montant à reverser au partenaire
     * 
     * @param partnerId ID du partenaire
     * @param orderTotal Total de la commande
     * @return BigDecimal montant net pour le partenaire
     */
    BigDecimal calculatePartnerPayout(Long partnerId, BigDecimal orderTotal);

    /**
     * Calculer les gains du livreur
     * 
     * @param deliveryFee Frais de livraison
     * @param tip Pourboire
     * @return BigDecimal gains du livreur
     */
    BigDecimal calculateCourierEarnings(BigDecimal deliveryFee, BigDecimal tip);

    // ==================== DTO INTERNE ====================

    /**
     * Structure contenant le détail des prix d'une commande
     */
    record OrderPriceBreakdown(
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal serviceFee,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal tip,
            BigDecimal total,
            String promoCode,
            BigDecimal promoDiscount,
            Integer loyaltyPointsUsed,
            BigDecimal loyaltyDiscount
    ) {}
}
