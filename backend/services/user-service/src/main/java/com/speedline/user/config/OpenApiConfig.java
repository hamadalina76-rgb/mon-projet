package com.speedline.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI/Swagger pour le User Service
 * 
 * Accessible via: /swagger-ui.html ou /swagger-ui/index.html
 * Documentation JSON: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpeedLine User Service API")
                        .description("""
                                ## API de gestion des utilisateurs pour la plateforme SpeedLine
                                
                                Ce service gère :
                                - **Customers** : Profils clients, préférences, favoris
                                - **Addresses** : Adresses de livraison avec support GPS (PostGIS)
                                - **Couriers** : Profils livreurs, disponibilité, statistiques, documents
                                - **Wallets** : Soldes et transactions
                                - **Loyalty** : Points de fidélité
                                
                                ### Endpoints principaux
                                
                                | Ressource | Endpoints |
                                |-----------|-----------|
                                | Customers | `GET/PUT/DELETE /customers/{id}` |
                                | Addresses | `GET/PUT/DELETE /addresses/{id}` |
                                | Customer Addresses | `GET/POST /customers/{id}/addresses` |
                                | Favorites | `GET/POST/DELETE /customers/{id}/favorites/{partnerId}` |
                                | Couriers | `GET/PUT /couriers/{id}` |
                                | Courier Availability | `PUT /couriers/{id}/availability` |
                                | Courier Statistics | `GET /couriers/{id}/statistics` |
                                | Courier Documents | `POST /couriers/{id}/documents` |
                                
                                ### Validation des coordonnées GPS
                                - Latitude : entre -90 et 90
                                - Longitude : entre -180 et 180
                                
                                ### Règles métier Adresses
                                - Maximum 10 adresses par client
                                - Une seule adresse par défaut par client
                                - Impossible de supprimer l'adresse par défaut si d'autres existent
                                
                                ### Règles métier Livreurs
                                - Le livreur doit être approuvé pour être disponible
                                - Statuts: PENDING_APPROVAL, ACTIVE, AVAILABLE, BUSY, OFFLINE, SUSPENDED
                                - Types de véhicule: BICYCLE, ELECTRIC_BICYCLE, MOTORCYCLE, CAR, WALKING
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SpeedLine Team")
                                .email("support@speedline.tn")
                                .url("https://speedline.tn"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://speedline.tn/terms")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentation complète SpeedLine")
                        .url("https://docs.speedline.tn"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Serveur de développement local"),
                        new Server()
                                .url("http://api-gateway:8080/user-service")
                                .description("Via API Gateway")))
                .tags(List.of(
                        new Tag()
                                .name("Customers")
                                .description("Gestion des profils clients : consultation, mise à jour, suppression, " +
                                        "gestion des adresses et des partenaires favoris"),
                        new Tag()
                                .name("Addresses")
                                .description("Gestion des adresses de livraison : CRUD, validation GPS, " +
                                        "gestion de l'adresse par défaut. Supporte les coordonnées PostGIS."),
                        new Tag()
                                .name("Couriers")
                                .description("Gestion des profils livreurs : consultation, mise à jour, " +
                                        "gestion de la disponibilité, statistiques de performance et upload de documents. " +
                                        "Types de véhicule: BICYCLE, ELECTRIC_BICYCLE, MOTORCYCLE, CAR, WALKING")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenu depuis auth-service. " +
                                        "Format: `Bearer <token>`")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}



