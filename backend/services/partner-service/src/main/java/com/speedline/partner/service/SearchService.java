package com.speedline.partner.service;

import com.speedline.partner.domain.PartnerType;
import com.speedline.partner.dto.CategoryDTO;
import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la recherche de partenaires et produits
 * 
 * Ce service gère toutes les fonctionnalités de recherche :
 * - Recherche textuelle
 * - Filtrage par catégorie, type, localisation
 * - Suggestions et autocomplétion
 */
public interface SearchService {

    // ==================== RECHERCHE DE PARTENAIRES ====================

    /**
     * Rechercher des partenaires par texte
     * Recherche dans: businessName, description, tags
     * 
     * @param query Terme de recherche
     * @param pageable Pagination
     * @return Page<PartnerDTO> partenaires correspondants
     */
    Page<PartnerDTO> searchPartners(String query, Pageable pageable);

    /**
     * Rechercher des partenaires avec filtres
     * 
     * @param query Terme de recherche (peut être null)
     * @param type Type de partenaire (peut être null)
     * @param categoryId ID de catégorie (peut être null)
     * @param city Ville (peut être null)
     * @param latitude Latitude du client (pour tri par distance, peut être null)
     * @param longitude Longitude du client
     * @param maxDistance Distance maximum en km (peut être null)
     * @param minRating Note minimum (peut être null)
     * @param isOpenNow Uniquement les partenaires ouverts (peut être null)
     * @param pageable Pagination
     * @return Page<PartnerDTO> partenaires correspondants
     */
    Page<PartnerDTO> searchPartnersWithFilters(
            String query,
            PartnerType type,
            Long categoryId,
            String city,
            BigDecimal latitude,
            BigDecimal longitude,
            Double maxDistance,
            BigDecimal minRating,
            Boolean isOpenNow,
            Pageable pageable);

    /**
     * Obtenir les partenaires proches
     * 
     * @param latitude Latitude du client
     * @param longitude Longitude du client
     * @param radiusKm Rayon de recherche en km
     * @param limit Nombre maximum de résultats
     * @return List<PartnerDTO> partenaires triés par distance
     */
    List<PartnerDTO> getNearbyPartners(BigDecimal latitude, BigDecimal longitude, 
                                        double radiusKm, int limit);

    /**
     * Obtenir les partenaires par catégorie
     * 
     * @param categoryId ID de la catégorie
     * @param pageable Pagination
     * @return Page<PartnerDTO> partenaires de cette catégorie
     */
    Page<PartnerDTO> getPartnersByCategory(Long categoryId, Pageable pageable);

    // ==================== RECHERCHE DE PRODUITS ====================

    /**
     * Rechercher des produits globalement (tous partenaires)
     * 
     * @param query Terme de recherche
     * @param pageable Pagination
     * @return Page<ProductDTO> produits correspondants
     */
    Page<ProductDTO> searchProducts(String query, Pageable pageable);

    /**
     * Rechercher des produits avec filtres
     * 
     * @param query Terme de recherche (peut être null)
     * @param partnerId ID du partenaire (peut être null)
     * @param categoryId ID de catégorie (peut être null)
     * @param minPrice Prix minimum (peut être null)
     * @param maxPrice Prix maximum (peut être null)
     * @param isVegetarian Uniquement végétarien (peut être null)
     * @param isVegan Uniquement vegan (peut être null)
     * @param isHalal Uniquement halal (peut être null)
     * @param isGlutenFree Uniquement sans gluten (peut être null)
     * @param isOnSale Uniquement en promotion (peut être null)
     * @param pageable Pagination
     * @return Page<ProductDTO> produits correspondants
     */
    Page<ProductDTO> searchProductsWithFilters(
            String query,
            Long partnerId,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isVegetarian,
            Boolean isVegan,
            Boolean isHalal,
            Boolean isGlutenFree,
            Boolean isOnSale,
            Pageable pageable);

    // ==================== CATÉGORIES ====================

    /**
     * Obtenir toutes les catégories de partenaires
     * 
     * @return List<CategoryDTO> catégories triées par displayOrder
     */
    List<CategoryDTO> getPartnerCategories();

    /**
     * Obtenir les catégories en vedette
     * 
     * @return List<CategoryDTO> catégories featured
     */
    List<CategoryDTO> getFeaturedCategories();

    /**
     * Obtenir les sous-catégories d'une catégorie
     * 
     * @param parentCategoryId ID de la catégorie parente
     * @return List<CategoryDTO> sous-catégories
     */
    List<CategoryDTO> getSubcategories(Long parentCategoryId);

    // ==================== SUGGESTIONS ====================

    /**
     * Obtenir des suggestions de recherche
     * Basé sur les recherches populaires et les noms de partenaires/produits
     * 
     * @param query Début de la requête (min 2 caractères)
     * @param limit Nombre maximum de suggestions (défaut: 10)
     * @return List<String> suggestions
     */
    List<String> getSuggestions(String query, int limit);

    /**
     * Obtenir les recherches populaires
     * 
     * @param limit Nombre maximum (défaut: 10)
     * @return List<String> recherches populaires
     */
    List<String> getPopularSearches(int limit);

    /**
     * Enregistrer une recherche (pour les statistiques)
     * 
     * @param query Terme recherché
     * @param userId ID de l'utilisateur (peut être null)
     */
    void recordSearch(String query, Long userId);

    // ==================== DÉCOUVERTE ====================

    /**
     * Obtenir les partenaires recommandés pour un utilisateur
     * Basé sur l'historique et les préférences
     * 
     * @param userId ID de l'utilisateur
     * @param limit Nombre maximum de résultats
     * @return List<PartnerDTO> partenaires recommandés
     */
    List<PartnerDTO> getRecommendedPartners(Long userId, int limit);

    /**
     * Obtenir les produits recommandés pour un utilisateur
     * Basé sur l'historique et les préférences
     * 
     * @param userId ID de l'utilisateur
     * @param limit Nombre maximum de résultats
     * @return List<ProductDTO> produits recommandés
     */
    List<ProductDTO> getRecommendedProducts(Long userId, int limit);

    /**
     * Obtenir les nouveaux partenaires
     * Partenaires inscrits dans les 30 derniers jours
     * 
     * @param limit Nombre maximum de résultats
     * @return List<PartnerDTO> nouveaux partenaires
     */
    List<PartnerDTO> getNewPartners(int limit);
}
