package com.speedline.user.dto;

import com.speedline.user.domain.CourierStatus;
import com.speedline.user.domain.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour Courier - Utilisé pour les réponses API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informations complètes d'un livreur")
public class CourierDTO {

    @Schema(description = "ID unique du livreur", example = "1")
    private Long id;

    @Schema(description = "ID de l'utilisateur associé", example = "42")
    private Long userId;
    
    // Informations de base (depuis auth-service)
    @Schema(description = "Email du livreur", example = "livreur@speedline.ma")
    private String email;

    @Schema(description = "Prénom du livreur", example = "Ahmed")
    private String firstName;

    @Schema(description = "Nom du livreur", example = "Benali")
    private String lastName;

    @Schema(description = "Numéro de téléphone", example = "+212612345678")
    private String phoneNumber;

    @Schema(description = "URL de la photo de profil")
    private String profilePhoto;
    
    // Informations véhicule
    @Schema(description = "Type de véhicule", example = "MOTORCYCLE")
    private VehicleType vehicleType;

    @Schema(description = "Numéro d'immatriculation", example = "12345-A-67")
    private String vehicleNumber;

    @Schema(description = "Modèle du véhicule", example = "Honda PCX 125")
    private String vehicleModel;

    @Schema(description = "Couleur du véhicule", example = "Noir")
    private String vehicleColor;
    
    // Informations de documentation
    @Schema(description = "Numéro de carte d'identité", example = "AB123456")
    private String identityNumber;

    @Schema(description = "URL de la photo recto de la CIN")
    private String identityDocumentFrontImage;

    @Schema(description = "URL de la photo verso de la CIN")
    private String identityDocumentBackImage;

    @Schema(description = "Numéro du permis de conduire", example = "XYZ-12345-6789")
    private String drivingLicenseNumber;

    @Schema(description = "URL de l'image du permis de conduire")
    private String drivingLicenseImage;

    @Schema(description = "Date d'expiration du permis")
    private LocalDateTime drivingLicenseExpiry;

    @Schema(description = "Nom du titulaire du compte bancaire", example = "Ahmed Benali")
    private String bankAccountHolder;

    @Schema(description = "IBAN du compte bancaire", example = "FR76 1234 5678 9012 3456 7890 123")
    private String bankIban;
    
    // Statut et disponibilité
    @Schema(description = "Statut du livreur", example = "AVAILABLE")
    private CourierStatus status;

    @Schema(description = "Livreur disponible pour des livraisons", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Livreur connecté à l'application", example = "true")
    private Boolean isOnline;

    @Schema(description = "Documents vérifiés par l'admin", example = "true")
    private Boolean documentsVerified;
    
    // Position actuelle
    @Schema(description = "Latitude actuelle", example = "33.5731")
    private BigDecimal currentLatitude;

    @Schema(description = "Longitude actuelle", example = "-7.5898")
    private BigDecimal currentLongitude;

    @Schema(description = "Dernière mise à jour de la position")
    private LocalDateTime lastLocationUpdate;
    
    // Statistiques
    @Schema(description = "Note moyenne (1-5)", example = "4.8")
    private BigDecimal rating;

    @Schema(description = "Nombre total d'évaluations", example = "156")
    private Integer totalRatings;

    @Schema(description = "Nombre total de livraisons", example = "523")
    private Integer totalDeliveries;

    @Schema(description = "Nombre de livraisons réussies", example = "512")
    private Integer successfulDeliveries;

    @Schema(description = "Temps moyen de livraison en minutes", example = "28")
    private Integer averageDeliveryTime;

    @Schema(description = "Taux de réussite (0-100)", example = "97.9")
    private Double successRate;
    
    // Finances (visible uniquement par le livreur)
    @Schema(description = "Gains totaux en MAD", example = "15230.50")
    private BigDecimal totalEarnings;

    @Schema(description = "Gains de la semaine en MAD", example = "1250.00")
    private BigDecimal weeklyEarnings;

    @Schema(description = "Solde disponible pour retrait en MAD", example = "850.00")
    private BigDecimal availableBalance;
    
    // Zone de livraison
    @Schema(description = "Zone de livraison préférée", example = "Casablanca Centre")
    private String preferredDeliveryZone;

    @Schema(description = "Rayon maximum de livraison en km", example = "15")
    private Integer maxDeliveryRadius;
    
    // Timestamps
    @Schema(description = "Date de création du profil")
    private LocalDateTime createdAt;

    @Schema(description = "Dernière connexion")
    private LocalDateTime lastLoginAt;

    /**
     * Nom complet du livreur
     */
    @Schema(description = "Nom complet (prénom + nom)", example = "Ahmed Benali")
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
