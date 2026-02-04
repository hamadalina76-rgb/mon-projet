package com.speedline.partner.service;

import com.speedline.partner.domain.ProductStatus;
import com.speedline.partner.dto.ProductDTO;
import com.speedline.partner.dto.ProductOptionDTO;
import com.speedline.partner.dto.ProductAddonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service pour la gestion des produits/articles
 * 
 * Ce service gère toutes les opérations liées aux produits :
 * - Création, modification, suppression de produits
 * - Gestion des options et suppléments
 * - Gestion de la disponibilité et du stock
 * - Recherche et filtrage
 */
public interface ProductService {

    // ==================== OPÉRATIONS CRUD ====================

    /**
     * Créer un nouveau produit
     * 
     * @param partnerId ID du partenaire propriétaire
     * @param categoryId ID de la catégorie (peut être null)
     * @param name Nom du produit
     * @param description Description
     * @param price Prix de base
     * @param image URL de l'image principale
     * @return ProductDTO avec id généré, status=ACTIVE
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     * @throws CategoryNotFoundException si la catégorie n'existe pas
     */
    ProductDTO createProduct(Long partnerId, Long categoryId, String name,
                             String description, BigDecimal price, String image);

    /**
     * Récupérer un produit par son ID
     * 
     * @param productId ID du produit
     * @return ProductDTO complet avec options et addons
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO getProductById(Long productId);

    /**
     * Mettre à jour les informations d'un produit
     * 
     * @param productId ID du produit
     * @param name Nouveau nom (null = pas de changement)
     * @param description Nouvelle description
     * @param shortDescription Description courte
     * @param price Nouveau prix
     * @param categoryId Nouvelle catégorie
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateProduct(Long productId, String name, String description,
                             String shortDescription, BigDecimal price, Long categoryId);

    /**
     * Mettre à jour les images d'un produit
     * 
     * @param productId ID du produit
     * @param image URL de l'image principale
     * @param additionalImages Liste des URLs des images supplémentaires
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateImages(Long productId, String image, List<String> additionalImages);

    /**
     * Supprimer un produit (soft delete)
     * 
     * @param productId ID du produit
     * @throws ProductNotFoundException si non trouvé
     */
    void deleteProduct(Long productId);

    // ==================== GESTION DE LA DISPONIBILITÉ ====================

    /**
     * Mettre à jour la disponibilité d'un produit
     * 
     * @param productId ID du produit
     * @param isAvailable true = disponible
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO setAvailability(Long productId, boolean isAvailable);

    /**
     * Mettre à jour le statut d'un produit
     * 
     * @param productId ID du produit
     * @param status Nouveau statut
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateStatus(Long productId, ProductStatus status);

    /**
     * Mettre à jour le stock d'un produit
     * 
     * @param productId ID du produit
     * @param stockQuantity Nouvelle quantité en stock (null = illimité)
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateStock(Long productId, Integer stockQuantity);

    /**
     * Décrémenter le stock après une commande
     * 
     * @param productId ID du produit
     * @param quantity Quantité à déduire
     * @return boolean true si succès, false si stock insuffisant
     * @throws ProductNotFoundException si non trouvé
     */
    boolean decrementStock(Long productId, int quantity);

    // ==================== GESTION DES PROMOTIONS ====================

    /**
     * Appliquer une promotion sur un produit
     * 
     * @param productId ID du produit
     * @param discountPercentage Pourcentage de réduction
     * @return ProductDTO mis à jour avec originalPrice et discountPercentage
     * @throws ProductNotFoundException si non trouvé
     * @throws InvalidDiscountException si le pourcentage est invalide (0-100)
     */
    ProductDTO applyDiscount(Long productId, BigDecimal discountPercentage);

    /**
     * Supprimer la promotion d'un produit
     * 
     * @param productId ID du produit
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO removeDiscount(Long productId);

    // ==================== GESTION DES CARACTÉRISTIQUES ====================

    /**
     * Mettre à jour les caractéristiques alimentaires
     * 
     * @param productId ID du produit
     * @param isVegetarian Végétarien
     * @param isVegan Vegan
     * @param isHalal Halal
     * @param isGlutenFree Sans gluten
     * @param spicyLevel Niveau d'épice (0-3)
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateDietaryInfo(Long productId, Boolean isVegetarian, Boolean isVegan,
                                  Boolean isHalal, Boolean isGlutenFree, Integer spicyLevel);

    /**
     * Mettre à jour les informations nutritionnelles
     * 
     * @param productId ID du produit
     * @param nutritionalInfoJson JSON des infos nutritionnelles
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateNutritionalInfo(Long productId, String nutritionalInfoJson);

    /**
     * Mettre à jour les allergènes
     * 
     * @param productId ID du produit
     * @param allergens Liste des allergènes
     * @return ProductDTO mis à jour
     * @throws ProductNotFoundException si non trouvé
     */
    ProductDTO updateAllergens(Long productId, List<String> allergens);

    // ==================== GESTION DES OPTIONS ====================

    /**
     * Ajouter une option à un produit
     * 
     * @param productId ID du produit
     * @param name Nom de l'option (ex: "Taille")
     * @param type Type (SINGLE ou MULTIPLE)
     * @param isRequired Obligatoire
     * @param minSelection Sélection minimum
     * @param maxSelection Sélection maximum
     * @return ProductOptionDTO créée
     * @throws ProductNotFoundException si le produit n'existe pas
     */
    ProductOptionDTO addOption(Long productId, String name, String type,
                               boolean isRequired, int minSelection, int maxSelection);

    /**
     * Mettre à jour une option
     * 
     * @param optionId ID de l'option
     * @param name Nouveau nom
     * @param isRequired Obligatoire
     * @param minSelection Sélection minimum
     * @param maxSelection Sélection maximum
     * @return ProductOptionDTO mise à jour
     * @throws OptionNotFoundException si l'option n'existe pas
     */
    ProductOptionDTO updateOption(Long optionId, String name, boolean isRequired,
                                   int minSelection, int maxSelection);

    /**
     * Supprimer une option
     * 
     * @param optionId ID de l'option
     * @throws OptionNotFoundException si l'option n'existe pas
     */
    void deleteOption(Long optionId);

    /**
     * Ajouter une valeur à une option
     * 
     * @param optionId ID de l'option
     * @param name Nom de la valeur (ex: "Medium")
     * @param priceModifier Modificateur de prix (+/- montant)
     * @param isDefault Valeur par défaut
     * @throws OptionNotFoundException si l'option n'existe pas
     */
    void addOptionValue(Long optionId, String name, BigDecimal priceModifier, boolean isDefault);

    /**
     * Supprimer une valeur d'option
     * 
     * @param valueId ID de la valeur
     * @throws OptionValueNotFoundException si la valeur n'existe pas
     */
    void deleteOptionValue(Long valueId);

    // ==================== GESTION DES SUPPLÉMENTS ====================

    /**
     * Ajouter un supplément à un produit
     * 
     * @param productId ID du produit
     * @param name Nom du supplément
     * @param price Prix du supplément
     * @param category Catégorie du supplément (ex: "Sauces")
     * @param image URL de l'image
     * @return ProductAddonDTO créé
     * @throws ProductNotFoundException si le produit n'existe pas
     */
    ProductAddonDTO addAddon(Long productId, String name, BigDecimal price,
                              String category, String image);

    /**
     * Mettre à jour un supplément
     * 
     * @param addonId ID du supplément
     * @param name Nouveau nom
     * @param price Nouveau prix
     * @param isAvailable Disponibilité
     * @return ProductAddonDTO mis à jour
     * @throws AddonNotFoundException si le supplément n'existe pas
     */
    ProductAddonDTO updateAddon(Long addonId, String name, BigDecimal price, boolean isAvailable);

    /**
     * Supprimer un supplément
     * 
     * @param addonId ID du supplément
     * @throws AddonNotFoundException si le supplément n'existe pas
     */
    void deleteAddon(Long addonId);

    // ==================== STATISTIQUES ====================

    /**
     * Incrémenter le compteur de commandes d'un produit
     * 
     * @param productId ID du produit
     * @throws ProductNotFoundException si non trouvé
     */
    void incrementOrderCount(Long productId);

    /**
     * Mettre à jour la note d'un produit
     * 
     * @param productId ID du produit
     * @param rating Nouvelle note (1-5)
     * @throws ProductNotFoundException si non trouvé
     */
    void updateRating(Long productId, BigDecimal rating);

    // ==================== RECHERCHE ET LISTE ====================

    /**
     * Obtenir tous les produits d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @return List<ProductDTO>
     * @throws PartnerNotFoundException si le partenaire n'existe pas
     */
    List<ProductDTO> getProductsByPartner(Long partnerId);

    /**
     * Obtenir les produits d'un partenaire avec pagination
     * 
     * @param partnerId ID du partenaire
     * @param pageable Pagination
     * @return Page<ProductDTO>
     */
    Page<ProductDTO> getProductsByPartnerPaginated(Long partnerId, Pageable pageable);

    /**
     * Obtenir les produits d'une catégorie
     * 
     * @param categoryId ID de la catégorie
     * @return List<ProductDTO>
     */
    List<ProductDTO> getProductsByCategory(Long categoryId);

    /**
     * Obtenir les produits populaires d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @return List<ProductDTO>
     */
    List<ProductDTO> getPopularProducts(Long partnerId);

    /**
     * Obtenir les produits en promotion d'un partenaire
     * 
     * @param partnerId ID du partenaire
     * @return List<ProductDTO>
     */
    List<ProductDTO> getPromotionalProducts(Long partnerId);

    /**
     * Rechercher des produits
     * 
     * @param partnerId ID du partenaire (null = tous les partenaires)
     * @param query Terme de recherche
     * @return List<ProductDTO>
     */
    List<ProductDTO> searchProducts(Long partnerId, String query);
}
