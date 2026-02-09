package com.speedline.user.controller;

import com.speedline.user.dto.AddressDTO;
import com.speedline.user.dto.AddressUpdateRequest;
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
 * Interface pour l'API de gestion des adresses de livraison
 * Définit le contrat et la documentation Swagger pour les opérations sur les adresses
 */
@Tag(name = "Addresses", description = "API de gestion des adresses de livraison")
public interface IAddressController {

    /**
     * Récupère une adresse par son ID
     */
    @Operation(
            summary = "Récupérer une adresse par ID",
            description = "Retourne les informations complètes d'une adresse de livraison par son ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Adresse trouvée",
                    content = @Content(schema = @Schema(implementation = AddressDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Adresse non trouvée"
            )
    })
    ResponseEntity<AddressDTO> getAddressById(
            @Parameter(description = "ID de l'adresse", required = true, example = "1")
            @PathVariable Long id
    );

    /**
     * Met à jour une adresse existante
     */
    @Operation(
            summary = "Mettre à jour une adresse",
            description = "Met à jour les informations d'une adresse existante. " +
                    "Tous les champs sont optionnels, seuls les champs fournis seront mis à jour. " +
                    "Les coordonnées GPS sont validées si fournies (latitude: -90 à 90, longitude: -180 à 180)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Adresse mise à jour avec succès",
                    content = @Content(schema = @Schema(implementation = AddressDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides (coordonnées GPS incorrectes, champs requis manquants)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé - l'adresse n'appartient pas à l'utilisateur authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Adresse non trouvée"
            )
    })
    ResponseEntity<AddressDTO> updateAddress(
            @Parameter(description = "ID de l'adresse", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody AddressUpdateRequest request
    );

    /**
     * Supprime une adresse (soft delete)
     */
    @Operation(
            summary = "Supprimer une adresse (soft delete)",
            description = "Supprime une adresse de livraison. " +
                    "L'adresse n'est pas physiquement supprimée mais marquée comme inactive. " +
                    "Impossible de supprimer l'adresse par défaut si d'autres adresses existent - " +
                    "il faut d'abord définir une autre adresse comme adresse par défaut."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Adresse supprimée avec succès"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Impossible de supprimer l'adresse par défaut (définir d'abord une autre adresse par défaut)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé - l'adresse n'appartient pas à l'utilisateur authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Adresse non trouvée"
            )
    })
    ResponseEntity<Void> deleteAddress(
            @Parameter(description = "ID de l'adresse", required = true, example = "1")
            @PathVariable Long id
    );
}
