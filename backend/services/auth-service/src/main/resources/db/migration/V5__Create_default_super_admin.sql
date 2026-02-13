-- ============================================
-- Auth Service - Create Default SUPER_ADMIN
-- Version: V4
-- Description: Crée automatiquement le compte SUPER_ADMIN
--              si aucun n'existe déjà
-- ============================================

-- Fonction pour créer le SUPER_ADMIN seulement si aucun n'existe
DO $$
DECLARE
    super_admin_count INTEGER;
    hashed_password VARCHAR(255);
BEGIN
    -- Compter le nombre de SUPER_ADMIN existants
    SELECT COUNT(*) INTO super_admin_count 
    FROM users 
    WHERE role = 'SUPER_ADMIN';
    
    -- Si aucun SUPER_ADMIN n'existe, en créer un
    IF super_admin_count = 0 THEN
        -- Hash du mot de passe "SuperAdmin@2024" avec BCrypt (force 10)
        -- Ce hash a été généré avec BCrypt et correspond au mot de passe "SuperAdmin@2024"
        hashed_password := '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
        
        INSERT INTO users (
            email,
            password,
            first_name,
            last_name,
            phone_number,
            role,
            status,
            is_email_verified,
            is_phone_verified,
            auth_provider,
            created_at,
            updated_at
        ) VALUES (
            'superadmin@speedline.com',
            hashed_password,
            'Super',
            'Admin',
            '+21600000000',
            'SUPER_ADMIN',
            'ACTIVE',
            true,
            false,
            'LOCAL',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
        RAISE NOTICE '✓ SUPER_ADMIN créé avec succès!';
        RAISE NOTICE '  Email: superadmin@speedline.com';
        RAISE NOTICE '  Password: SuperAdmin@2024';
        RAISE NOTICE '  ⚠️  IMPORTANT: Changez ce mot de passe après la première connexion!';
    ELSE
        RAISE NOTICE '✓ SUPER_ADMIN déjà existant(s): % compte(s)', super_admin_count;
    END IF;
END $$;

-- Créer un index sur le rôle si pas déjà existant
CREATE INDEX IF NOT EXISTS idx_users_role_super_admin ON users(role) WHERE role = 'SUPER_ADMIN';

-- Commentaires pour documentation
COMMENT ON TABLE users IS 'Table des utilisateurs avec support du rôle SUPER_ADMIN';
COMMENT ON COLUMN users.role IS 'Rôle de l''utilisateur: CUSTOMER, COURIER, PARTNER, ADMIN, SUPER_ADMIN';
