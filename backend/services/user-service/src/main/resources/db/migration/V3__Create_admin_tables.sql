-- ============================================
-- User Service - Admin Module Tables
-- Version: 3
-- Description: Création des tables pour le module administrateur
-- ============================================

-- Table: admin_roles
-- Définit les rôles des administrateurs avec leurs permissions
CREATE TABLE IF NOT EXISTS admin_roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL, -- Ex: SUPER_ADMIN, ADMIN, MODERATOR
    label VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(20), -- Couleur hex pour l'affichage
    trust_level INTEGER NOT NULL CHECK (trust_level >= 0 AND trust_level <= 100),
    max_refund_amount DECIMAL(10,2) DEFAULT 0,
    requires_approval BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index sur le code pour recherche rapide
CREATE INDEX IF NOT EXISTS idx_admin_roles_code ON admin_roles(code);
CREATE INDEX IF NOT EXISTS idx_admin_roles_active ON admin_roles(active);

-- Table: admins
-- Stocke les informations spécifiques aux administrateurs
CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL, -- Référence logique vers auth-service users table
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role_id BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- ACTIVE, INACTIVE, PENDING, SUSPENDED
    avatar TEXT,
    last_login TIMESTAMP,
    custom_permissions TEXT[], -- Permissions additionnelles au-delà du rôle
    created_by BIGINT, -- ID de l'admin qui a créé ce compte
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_role FOREIGN KEY (role_id) REFERENCES admin_roles(id) ON DELETE RESTRICT
);

-- Index pour performances
CREATE INDEX IF NOT EXISTS idx_admins_user_id ON admins(user_id);
CREATE INDEX IF NOT EXISTS idx_admins_email ON admins(email);
CREATE INDEX IF NOT EXISTS idx_admins_status ON admins(status);
CREATE INDEX IF NOT EXISTS idx_admins_role_id ON admins(role_id);

-- Table: permissions
-- Définit les permissions par module pour chaque rôle
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    module VARCHAR(100) NOT NULL, -- Ex: dashboard, users, orders, partners
    module_label VARCHAR(255) NOT NULL,
    icon VARCHAR(100), -- Nom de l'icône Material Icons
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_permission_role FOREIGN KEY (role_id) REFERENCES admin_roles(id) ON DELETE CASCADE
);

-- Index pour recherche rapide des permissions par rôle et module
CREATE INDEX IF NOT EXISTS idx_permissions_role_id ON permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module);
CREATE INDEX IF NOT EXISTS idx_permissions_enabled ON permissions(enabled);

-- Trigger pour mise à jour automatique de updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_admin_roles_updated_at BEFORE UPDATE ON admin_roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_admins_updated_at BEFORE UPDATE ON admins
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
