package com.speedline.auth.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour l'authentification
 * 
 * Endpoints:
 * POST /auth/register - Inscription
 * POST /auth/login - Connexion
 * POST /auth/refresh - Refresh token
 * POST /auth/logout - Déconnexion
 * POST /auth/verify-email - Vérifier email
 * POST /auth/forgot-password - Mot de passe oublié
 * POST /auth/reset-password - Réinitialiser mot de passe
 * GET  /auth/validate-token - Valider JWT (interne)
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    // TODO: Implémenter les endpoints
}
