-- ============================================
-- User Service - Admin Permissions Junction Table
-- Version: 6
-- Description: Création de la table pour les permissions personnalisées des admins
-- ============================================

-- Table: admin_permissions
-- Table de jonction pour stocker les permissions personnalisées des admins
-- (au-delà de celles héritées de leur rôle)
CREATE TABLE IF NOT EXISTS admin_permissions (
    admin_id BIGINT NOT NULL,
    permission VARCHAR(255) NOT NULL,
    CONSTRAINT fk_admin_permissions_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE
);

-- Index pour améliorer les performances de recherche
CREATE INDEX IF NOT EXISTS idx_admin_permissions_admin_id ON admin_permissions(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_permissions_permission ON admin_permissions(permission);

-- Contrainte d'unicité pour éviter les doublons
CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_permissions_unique ON admin_permissions(admin_id, permission);
