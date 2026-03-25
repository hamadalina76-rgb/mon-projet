package com.speedline.partner.controller;

import com.speedline.partner.dto.CompletePartnerProfileRequest;
import com.speedline.partner.dto.CreatePartnerRequest;
import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.dto.UpdatePartnerStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI/Swagger contract for Partner endpoints.
 * All documentation annotations live here; PartnerController
 * contains only the implementation logic.
 */
@RequestMapping(path = "/partners", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Partners", description = "Gestion des établissements partenaires (restaurants, magasins...)")
public interface PartnerApi {

    // ===================== INTERNAL =====================

    @PostMapping(path = "/internal", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Créer profil partner (interne)",
            description = "Endpoint appelé par auth-service via Feign lors de l'inscription d'un partenaire. " +
                          "Crée un profil minimal avec statut PENDING."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profil créé",
                    content = @Content(schema = @Schema(implementation = PartnerDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur serveur interne")
    })
    ResponseEntity<?> createPartnerInternal(
            @Parameter(description = "Données de base du partenaire", required = true)
            @RequestBody CreatePartnerRequest request
    );

    // ===================== LECTURE =====================

    @GetMapping("/by-user/{userId}")
    @Operation(
            summary = "Profil partner par userId",
            description = "Retourne le PartnerDTO correspondant à l'utilisateur connecté (après login)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partenaire trouvé",
                    content = @Content(schema = @Schema(implementation = PartnerDTO.class))),
            @ApiResponse(responseCode = "404", description = "Aucun partenaire pour ce userId")
    })
    ResponseEntity<?> getPartnerByUserId(
            @Parameter(description = "ID de l'utilisateur", required = true)
            @PathVariable Long userId
    );

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail partenaire",
            description = "Retourne le PartnerDTO complet (infos, horaires, photos…) par l'ID partenaire."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partenaire trouvé",
                    content = @Content(schema = @Schema(implementation = PartnerDTO.class))),
            @ApiResponse(responseCode = "404", description = "Partenaire introuvable")
    })
    ResponseEntity<?> getPartnerById(
            @Parameter(description = "ID du partenaire", required = true)
            @PathVariable Long id
    );

    // ===================== MODIFICATION =====================

    @PutMapping(path = "/{id}/complete-profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Compléter le profil (Phase 2)",
            description = "Permet au partenaire de renseigner toutes ses informations après le premier login : " +
                          "type, adresse, horaires, description, etc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour",
                    content = @Content(schema = @Schema(implementation = PartnerDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la mise à jour")
    })
    ResponseEntity<?> completeProfile(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "Données complètes du profil", required = true)
            @RequestBody CompletePartnerProfileRequest request
    );

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Mettre à jour le partenaire",
            description = "Met à jour les informations du partenaire (équivalent à complete-profile)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partenaire mis à jour",
                    content = @Content(schema = @Schema(implementation = PartnerDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la mise à jour")
    })
    ResponseEntity<?> updatePartner(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "Nouvelles données", required = true)
            @RequestBody CompletePartnerProfileRequest request
    );

    @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Ouvrir / fermer le partenaire",
            description = "Bascule acceptsOrders. Le changement est immédiatement visible dans l'app client. " +
                          "Réservé à l'OWNER ou à l'ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis à jour",
                    content = @Content(schema = @Schema(implementation = PartnerDTO.class))),
            @ApiResponse(responseCode = "404", description = "Partenaire introuvable")
    })
    ResponseEntity<?> updateStatus(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "{ \"isOpen\": true|false }", required = true)
            @RequestBody UpdatePartnerStatusRequest request
    );

    // ===================== HORAIRES =====================

    @GetMapping("/{id}/opening-hours")
    @Operation(
            summary = "Lire les horaires d'ouverture",
            description = "Retourne la liste des plages horaires (une entrée par jour de semaine)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horaires retournés"),
            @ApiResponse(responseCode = "404", description = "Partenaire introuvable")
    })
    ResponseEntity<?> getOpeningHours(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id
    );

    @PutMapping(path = "/{id}/opening-hours", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Modifier les horaires d'ouverture",
            description = "Remplace les horaires complets. Body = tableau JSON : " +
                          "[{\"day\":\"MONDAY\",\"isClosed\":false,\"slots\":[{\"open\":\"09:00\",\"close\":\"22:00\"}]}]"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horaires mis à jour"),
            @ApiResponse(responseCode = "400", description = "Format JSON invalide"),
            @ApiResponse(responseCode = "404", description = "Partenaire introuvable")
    })
    ResponseEntity<?> putOpeningHours(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @RequestBody List<Map<String, Object>> body
    );

    // ===================== STAFF =====================

    @GetMapping("/{id}/staff")
    @Operation(
            summary = "Liste du staff",
            description = "Retourne tous les membres du staff du partenaire (OWNER, MANAGER, STAFF)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff retourné"),
            @ApiResponse(responseCode = "404", description = "Partenaire introuvable")
    })
    ResponseEntity<?> getStaff(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id
    );

    // ===================== MÉDIAS =====================

    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Uploader les images",
            description = "Upload le logo, la photo de couverture et/ou des photos supplémentaires. " +
                          "Tous les champs sont optionnels (multipart/form-data)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images uploadées, URLs retournées"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de l'upload")
    })
    ResponseEntity<?> uploadImages(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "Logo (JPG/PNG, max 5 MB)")
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @Parameter(description = "Photo de couverture (JPG/PNG, max 5 MB)")
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @Parameter(description = "Photos additionnelles (multiple)")
            @RequestParam(value = "photos", required = false) MultipartFile[] photos
    );

    @PostMapping(path = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Uploader les documents légaux",
            description = "Upload les documents de vérification : Kbis, carte d'identité, assurance, RIB. " +
                          "Tous les champs sont optionnels."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents uploadés, URLs retournées"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de l'upload")
    })
    ResponseEntity<?> uploadDocuments(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "Extrait Kbis (PDF)") @RequestParam(value = "kbis", required = false) MultipartFile kbis,
            @Parameter(description = "Carte d'identité (PDF/JPG)") @RequestParam(value = "idCard", required = false) MultipartFile idCard,
            @Parameter(description = "Attestation d'assurance (PDF)") @RequestParam(value = "insurance", required = false) MultipartFile insurance,
            @Parameter(description = "RIB (PDF/JPG)") @RequestParam(value = "rib", required = false) MultipartFile rib
    );

    @PostMapping(path = "/{id}/upload/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload logo partenaire",
            description = "Upload du logo du partenaire (multipart/form-data, champ `file`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logo uploadé, URL retournée"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de l'upload")
    })
    ResponseEntity<?> uploadLogo(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "Fichier image logo (JPG/PNG/WEBP)", required = true)
            @RequestParam("file") MultipartFile file
    );

    @PostMapping(path = "/{id}/upload/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload cover partenaire",
            description = "Upload de la photo de couverture (multipart/form-data, champ `file`)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cover uploadée, URL retournée"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de l'upload")
    })
    ResponseEntity<?> uploadCover(
            @Parameter(description = "ID du partenaire", required = true) @PathVariable Long id,
            @Parameter(description = "Fichier image cover (JPG/PNG/WEBP)", required = true)
            @RequestParam("file") MultipartFile file
    );

    // ===================== GÉOLOCALISATION =====================

    @GetMapping("/nearby")
    @Operation(
            summary = "Partenaires à proximité",
            description = "Retourne les partenaires actifs dans le rayon configuré (défaut 5 km), " +
                          "triés par : ouverts en premier, puis distance croissante, puis note décroissante. " +
                          "Le rayon peut être surchargé via la clé Redis `config:nearby:radius_km`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de partenaires proches"),
            @ApiResponse(responseCode = "400", description = "Coordonnées invalides (hors bornes ou manquantes)")
    })
    ResponseEntity<Page<PartnerDTO>> getNearbyPartners(
            @Parameter(description = "Latitude du client (−90 à 90)", required = true, example = "36.8065")
            @RequestParam
            @NotNull
            @DecimalMin(value = "-90.0",  message = "Latitude invalide : doit être entre -90 et 90")
            @DecimalMax(value = "90.0",   message = "Latitude invalide : doit être entre -90 et 90")
            BigDecimal lat,

            @Parameter(description = "Longitude du client (−180 à 180)", required = true, example = "10.1815")
            @RequestParam
            @NotNull
            @DecimalMin(value = "-180.0", message = "Longitude invalide : doit être entre -180 et 180")
            @DecimalMax(value = "180.0",  message = "Longitude invalide : doit être entre -180 et 180")
            BigDecimal lng,

            @Parameter(description = "Numéro de page, 0-indexé (défaut 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Nombre de résultats par page, entre 1 et 100 (défaut 20)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,

            @Parameter(description = "Filtrer uniquement partenaires ouverts", example = "true")
            @RequestParam(required = false) Boolean isOpen,

            @Parameter(description = "Filtrer par catégorie (id)")
            @RequestParam(required = false) String categoryId,

            @Parameter(description = "Note minimale", example = "4.0")
            @RequestParam(required = false) Double minRating,

            @Parameter(description = "Temps de livraison maximum (minutes)", example = "30")
            @RequestParam(required = false) Integer maxDeliveryTime,

            @Parameter(description = "Livraison gratuite uniquement", example = "true")
            @RequestParam(required = false) Boolean freeDelivery,

            @Parameter(description = "Tri des résultats (distance, rating, deliveryTime)", example = "distance")
            @RequestParam(defaultValue = "distance") String sortBy
    );

    @GetMapping("/search")
    @Operation(
            summary = "Recherche partenaires",
            description = "Recherche textuelle des partenaires dans la zone à proximité du client."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de résultats de recherche"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides")
    })
    ResponseEntity<Page<PartnerDTO>> searchPartners(
            @Parameter(description = "Texte recherché", required = true, example = "pizza")
            @RequestParam @NotNull String query,

            @Parameter(description = "Latitude du client (−90 à 90)", required = true, example = "36.8065")
            @RequestParam
            @NotNull
            @DecimalMin(value = "-90.0", message = "Latitude invalide : doit être entre -90 et 90")
            @DecimalMax(value = "90.0", message = "Latitude invalide : doit être entre -90 et 90")
            BigDecimal lat,

            @Parameter(description = "Longitude du client (−180 à 180)", required = true, example = "10.1815")
            @RequestParam
            @NotNull
            @DecimalMin(value = "-180.0", message = "Longitude invalide : doit être entre -180 et 180")
            @DecimalMax(value = "180.0", message = "Longitude invalide : doit être entre -180 et 180")
            BigDecimal lng,

            @Parameter(description = "Numéro de page, 0-indexé", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Nombre de résultats par page", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @GetMapping("/search/trending")
    @Operation(
            summary = "Recherches tendances",
            description = "Retourne les termes de recherche tendances basés sur les partenaires proches."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des termes tendances"),
            @ApiResponse(responseCode = "400", description = "Coordonnées invalides")
    })
    ResponseEntity<List<String>> getTrendingSearches(
            @Parameter(description = "Latitude du client (−90 à 90)", required = true, example = "36.8065")
            @RequestParam
            @NotNull
            @DecimalMin(value = "-90.0", message = "Latitude invalide : doit être entre -90 et 90")
            @DecimalMax(value = "90.0", message = "Latitude invalide : doit être entre -90 et 90")
            BigDecimal lat,

            @Parameter(description = "Longitude du client (−180 à 180)", required = true, example = "10.1815")
            @RequestParam
            @NotNull
            @DecimalMin(value = "-180.0", message = "Longitude invalide : doit être entre -180 et 180")
            @DecimalMax(value = "180.0", message = "Longitude invalide : doit être entre -180 et 180")
            BigDecimal lng,

            @Parameter(description = "Nombre maximum de termes", example = "5")
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit
    );

    // ===================== SANTÉ =====================

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Vérifie que le service est actif.")
    ResponseEntity<Map<String, String>> health();
}
