package com.speedline.partner.service;

import com.speedline.partner.dto.MenuDTO;
import com.speedline.partner.dto.ProductDTO;

import java.util.List;

/**
 * Service pour la gestion des menus
 * 
 * Ce service gère l'affichage structuré des menus :
 * - Menu complet d'un partenaire
 * - Sections par catégorie
 * - Produits spéciaux (populaires, nouveaux, en promo)
 */
public interface MenuService {

    /**
     * Obtenir le menu complet d'un partenaire
     * Organisé par catégories avec tous les produits
     * 
     * @param partnerId ID du partenaire
     * @return MenuDTO complet avec:
     *         - partnerId, partnerName
     *         - sections (catégories avec leurs produits)
     *         - popularProducts (produits les plus commandés)
     *         - newProducts (nouveaux produits)
     *         - promotionalProducts (produits en promotion)
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    MenuDTO getFullMenu(Long partnerId);

    /**
     * Obtenir les produits d'une catégorie spécifique
     * 
     * @param partnerId ID du partenaire
     * @param categoryId ID de la catégorie
     * @return List<ProductDTO> produits de cette catégorie
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     * @throws CategoryNotFoundException si la catégorie n'existe pas
     */
    List<ProductDTO> getMenuByCategory(Long partnerId, Long categoryId);

    /**
     * Obtenir les produits populaires (best-sellers)
     * 
     * @param partnerId ID du partenaire
     * @param limit Nombre maximum de produits (défaut: 10)
     * @return List<ProductDTO> produits les plus commandés
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getPopularItems(Long partnerId, int limit);

    /**
     * Obtenir les nouveaux produits
     * Produits ajoutés dans les 30 derniers jours
     * 
     * @param partnerId ID du partenaire
     * @param limit Nombre maximum de produits (défaut: 10)
     * @return List<ProductDTO> nouveaux produits
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getNewItems(Long partnerId, int limit);

    /**
     * Obtenir les produits en promotion
     * 
     * @param partnerId ID du partenaire
     * @return List<ProductDTO> produits avec réduction active
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getPromotionalItems(Long partnerId);

    /**
     * Obtenir les produits recommandés
     * Basé sur les produits featured et populaires
     * 
     * @param partnerId ID du partenaire
     * @param limit Nombre maximum de produits (défaut: 6)
     * @return List<ProductDTO> produits recommandés
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getRecommendedItems(Long partnerId, int limit);

    /**
     * Obtenir les produits végétariens
     * 
     * @param partnerId ID du partenaire
     * @return List<ProductDTO> produits végétariens
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getVegetarianItems(Long partnerId);

    /**
     * Obtenir les produits halal
     * 
     * @param partnerId ID du partenaire
     * @return List<ProductDTO> produits halal
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getHalalItems(Long partnerId);

    /**
     * Rechercher dans le menu d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @param query Terme de recherche
     * @return List<ProductDTO> produits correspondants
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> searchMenu(Long partnerId, String query);

    /**
     * Vérifier si un produit est disponible pour commande
     * Vérifie: isAvailable, status, stock
     * 
     * @param productId ID du produit
     * @return boolean true si commandable
     * @throws ProductNotFoundException si le produit n'existe pas
     */
    boolean isProductAvailable(Long productId);

    /**
     * Calculer le prix total d'un produit avec options et suppléments
     * 
     * @param productId ID du produit
     * @param selectedOptionValueIds IDs des valeurs d'options sélectionnées
     * @param selectedAddonIds IDs des suppléments avec quantités (format: "addonId:quantity")
     * @return BigDecimal prix total calculé
     * @throws ProductNotFoundException si le produit n'existe pas
     * @throws InvalidSelectionException si une sélection est invalide
     */
    java.math.BigDecimal calculateProductPrice(Long productId, 
                                                List<Long> selectedOptionValueIds,
                                                List<String> selectedAddonIds);
}
