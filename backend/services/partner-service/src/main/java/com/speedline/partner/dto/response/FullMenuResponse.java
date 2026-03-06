package com.speedline.partner.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Réponse du menu complet d'un partenaire, structure imbriquée consommée par l'app client.
 * TC-16 : GET /api/partners/{id}/menu → structure imbriquée complète.
 *
 * Structure :
 * {
 *   "partnerId": 1,
 *   "partnerName": "Pizza Palace",
 *   "categories": [
 *     {
 *       "category": { id, name, imageUrl, position, isVisible, … },
 *       "products": [
 *         {
 *           id, name, price, isAvailable, tags, …,
 *           "optionGroups": [ { id, name, type, options: [...] } ]
 *         }
 *       ]
 *     }
 *   ]
 * }
 */
@Data
@Builder
public class FullMenuResponse {

    private Long partnerId;
    private String partnerName;

    /** Catégories visibles avec leurs produits actifs. TC-16. */
    private List<CategorySection> categories;

    // ==================== INNER CLASS ====================

    /**
     * Section du menu = une catégorie visible + ses produits actifs.
     */
    @Data
    @Builder
    public static class CategorySection {

        /** Métadonnées de la catégorie. */
        private MenuCategoryResponse category;

        /** Produits actifs de cette catégorie, triés par position. */
        private List<ProductResponse> products;
    }
}
