package com.speedline.partner.repository;

import com.speedline.partner.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    @Query(value = """
        SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END 
        FROM categories 
        WHERE (name_i18n ->> :locale) = :name 
        AND id != COALESCE(:excludeId, -1)
    """, nativeQuery = true)
    boolean existsByNameAndLocaleExcluding(
            @Param("name") String name,
            @Param("locale") String locale,
            @Param("excludeId") Long excludeId
    );

    /**
     * Recherche filtrée : search sur nameI18n (JSONB casté en text) et slug,
     * filtre sur category_business_type (string) et is_active (boolean).
     * Tous les paramètres sont optionnels (NULL = pas de filtre).
     */
    @Query(value = """
        SELECT * FROM categories
        WHERE
            (:search IS NULL OR :search = '' OR
                LOWER(CAST(name_i18n AS text)) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(slug)                    LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:businessType IS NULL OR category_business_type = :businessType)
        AND (:status IS NULL OR is_active = CAST(:status AS boolean))
        ORDER BY display_order ASC NULLS LAST
    """, nativeQuery = true)
    List<Category> findFiltered(
            @Param("search")       String search,
            @Param("businessType") String businessType,
            @Param("status")       String status
    );
}