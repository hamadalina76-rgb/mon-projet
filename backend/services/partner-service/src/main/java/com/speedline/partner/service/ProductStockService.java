package com.speedline.partner.service;

import com.speedline.partner.dto.request.UpdateStockRequest;
import com.speedline.partner.dto.response.BulkStockUpdateResult;
import com.speedline.partner.dto.response.ProductStockDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service de gestion du stock des produits partenaire.
 */
public interface ProductStockService {

    /**
     * Liste tous les produits du partenaire avec leur stock (ou défaut si pas de ProductStock).
     */
    List<ProductStockDTO> getStockList(Long partnerId);

    /**
     * Liste filtrée : recherche textuelle + filtre de statut.
     */
    List<ProductStockDTO> getStockList(Long partnerId, String search, String status);

    /**
     * Met à jour le stock d'un produit et applique les règles (isAvailable, événements).
     */
    ProductStockDTO updateStock(Long partnerId, Long productId, UpdateStockRequest request);

    /**
     * Produits avec quantity <= lowStockThreshold et isTrackingEnabled = true.
     */
    List<ProductStockDTO> getLowStock(Long partnerId);

    /**
     * Produits avec quantity = 0 et isTrackingEnabled = true.
     */
    List<ProductStockDTO> getOutOfStock(Long partnerId);

    /**
     * Mise à jour en masse via CSV (colonnes: productId,quantity).
     */
    BulkStockUpdateResult bulkUpdate(Long partnerId, MultipartFile file);

    /**
     * Remet en stock tous les produits épuisés du partenaire (quantity = 1 ou lowStockThreshold si > 0).
     * @return nombre de produits restaurés
     */
    int restoreAllOutOfStock(Long partnerId);
}
