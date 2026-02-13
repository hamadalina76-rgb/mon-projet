package com.speedline.auth.domain;

public enum UserStatus {
    PENDING,
    ACTIVE,
    INACTIVE,    // Compte désactivé par un admin
    SUSPENDED,   // Compte suspendu (sanction temporaire)
    DELETED
}
