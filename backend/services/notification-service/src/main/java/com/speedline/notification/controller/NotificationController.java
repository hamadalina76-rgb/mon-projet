package com.speedline.notification.controller;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller pour Notification
 * 
 * Endpoints:
 * POST   /notifications/send - Envoyer notification
 * GET    /notifications/{userId} - Historique notifications
 * PUT    /notifications/{id}/read - Marquer comme lu
 * POST   /push-tokens - Enregistrer token FCM
 * DELETE /push-tokens/{id} - Supprimer token
 * GET    /notifications/unread-count - Nombre non lus
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    // TODO: Implémenter
}
