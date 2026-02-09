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

import java.util.List;

/**
 * Interface pour l'API de gestion des clients (Customers)
 * Définit le contrat et la documentation Swagger pour les opérations sur les clients
 */
@Tag(name = "Customers", description = "API de gestion des profils clients")
public interface ICustomerController {

    /**
     * Récupère un client par son ID
     */
    @Operation(
            summary = "Récupérer un client par ID",
            description = "Retourne les informations complètes d'un client par son ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client trouvé",
                    content = @Content(schema = @Schema(implementation = CustomerDTO.class))),
            @ApiResponse(responseCode = "404", description = "Client non trouvé")
    })
    ResponseEntity<CustomerDTO> getCustomerById(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id
    );

    /**
     * Met à jour un profil client
     */
    @Operation(
            summary = "Mettre à jour un profil client",
            description = "Met à jour les préférences et favoris d'un client"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    ResponseEntity<CustomerDTO> updateCustomer(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CustomerUpdateRequest request
    );

    /**
     * Supprime un profil client (soft delete)
     */
    @Operation(
            summary = "Supprimer un profil client (soft delete)",
            description = "Marque le profil client comme supprimé sans le supprimer physiquement"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé")
    })
    ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id
    );

    /**
     * Récupère les adresses d'un client
     */
    @Operation(
            summary = "Obtenir les adresses d'un client",
            description = "Retourne la liste des adresses actives d'un client"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des adresses récupérée"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé")
    })
    ResponseEntity<List<AddressDTO>> getCustomerAddresses(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id
    );

    /**
     * Crée une nouvelle adresse pour un client
     */
    @Operation(
            summary = "Créer une nouvelle adresse",
            description = "Ajoute une nouvelle adresse au profil client"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adresse créée avec succès",
                    content = @Content(schema = @Schema(implementation = AddressDTO.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé")
    })
    ResponseEntity<AddressDTO> createAddress(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AddressCreateRequest request
    );

    /**
     * Récupère les partenaires favoris d'un client
     */
    @Operation(
            summary = "Obtenir les partenaires favoris",
            description = "Retourne la liste des IDs des partenaires favoris d'un client"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des favoris récupérée"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé")
    })
    ResponseEntity<List<Long>> getFavoritePartners(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id
    );

    /**
     * Ajoute un partenaire aux favoris d'un client
     */
    @Operation(
            summary = "Ajouter un partenaire aux favoris",
            description = "Ajoute un partenaire à la liste des favoris d'un client"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partenaire ajouté aux favoris"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé")
    })
    ResponseEntity<Void> addFavoritePartner(
            @Parameter(description = "ID du client", required = true)
            @PathVariable Long id,
            @Parameter(description = "ID du partenaire à ajouter", required = true)
            @PathVariable Long partnerId
    );
}
