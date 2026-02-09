package com.speedline.user.controller;

import com.speedline.user.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Interface pour l'API de gestion des livreurs
 * Définit le contrat et la documentation Swagger pour les opérations sur les livreurs
 */
@Tag(name = "Couriers", description = "API de gestion des livreurs")
public interface ICourierController {

    /**
     * Récupère un livreur par son ID
     */
    @Operation(
            summary = "Récupérer un livreur par ID",
            description = "Retourne les informations complètes d'un livreur incluant son statut, véhicule, position et statistiques"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Livreur trouvé",
                    content = @Content(schema = @Schema(implementation = CourierDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Livreur non trouvé"
            )
    })
    ResponseEntity<CourierDTO> getCourierById(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id
    );

    /**
     * Met à jour le profil d'un livreur
     */
    @Operation(
            summary = "Mettre à jour le profil du livreur",
            description = "Met à jour les informations du livreur : véhicule, zone de livraison, coordonnées bancaires. " +
                    "Seuls les champs fournis sont mis à jour (PATCH-like behavior)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil mis à jour avec succès",
                    content = @Content(schema = @Schema(implementation = CourierDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Livreur non trouvé"
            )
    })
    ResponseEntity<CourierDTO> updateCourier(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CourierUpdateRequest request
    );

    /**
     * Met à jour la disponibilité d'un livreur
     */
    @Operation(
            summary = "Changer la disponibilité du livreur",
            description = "Permet au livreur de passer en ligne/hors ligne et de se rendre disponible ou indisponible. " +
                    "Le livreur doit être approuvé et non suspendu pour pouvoir être disponible."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Disponibilité mise à jour",
                    content = @Content(schema = @Schema(implementation = CourierDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Le livreur ne peut pas être mis en disponibilité (non approuvé, suspendu, etc.)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Livreur non trouvé"
            )
    })
    ResponseEntity<CourierDTO> updateAvailability(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CourierAvailabilityRequest request
    );

    /**
     * Récupère les statistiques d'un livreur
     */
    @Operation(
            summary = "Récupérer les statistiques du livreur",
            description = "Retourne les statistiques détaillées : nombre de livraisons, taux de succès, note moyenne, " +
                    "gains totaux, distance parcourue, temps moyen de livraison"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistiques récupérées",
                    content = @Content(schema = @Schema(implementation = CourierStatisticsDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Livreur non trouvé"
            )
    })
    ResponseEntity<CourierStatisticsDTO> getCourierStatistics(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id
    );

    /**
     * Upload un document pour un livreur
     */
    @Operation(
            summary = "Uploader un document",
            description = "Permet au livreur de soumettre un document (CIN, permis de conduire, photo de profil, " +
                    "assurance véhicule, etc.). Le document est stocké via son URL (pré-uploadé sur un service de stockage)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Document uploadé avec succès",
                    content = @Content(schema = @Schema(implementation = CourierDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Type de document invalide ou URL manquante"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Livreur non trouvé"
            )
    })
    ResponseEntity<CourierDTO> uploadDocument(
            @Parameter(description = "ID du livreur", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CourierDocumentUploadRequest request
    );
}
