-- ============================================
-- User Service - Create SUPER_ADMIN Profile
-- Version: V5
-- Description: Crée automatiquement le profil admin
--              pour le SUPER_ADMIN de auth-service
-- ============================================

-- Fonction pour créer le profil SUPER_ADMIN
DO $$
DECLARE
    admin_profile_count INTEGER;
    super_admin_role_id BIGINT;
BEGIN
    -- Vérifier si le profil existe déjà
    SELECT COUNT(*) INTO admin_profile_count 
    FROM admins 
    WHERE email = 'superadmin@speedline.com';
    
    -- Si le profil n'existe pas, le créer
    IF admin_profile_count = 0 THEN
        -- Récupérer l'ID du rôle SUPER_ADMIN
        SELECT id INTO super_admin_role_id 
        FROM admin_roles 
        WHERE code = 'SUPER_ADMIN' 
        LIMIT 1;
        
        IF super_admin_role_id IS NULL THEN
            RAISE EXCEPTION 'Rôle SUPER_ADMIN non trouvé dans admin_roles. Exécutez d''abord la migration V4.';
        END IF;
        
        -- Créer le profil admin
        -- user_id = 1 car c'est le premier utilisateur créé dans auth-service
        INSERT INTO admins (
            user_id,
            full_name,
            email,
            role_id,
            status,
            notes,
            created_at,
            updated_at
        ) VALUES (
            1, -- userId du SUPER_ADMIN dans auth-service
            'Super Admin',
            'superadmin@speedline.com',
            super_admin_role_id,
            'ACTIVE',
            'Compte SUPER_ADMIN créé automatiquement au démarrage',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );
        
        RAISE NOTICE '✓ Profil SUPER_ADMIN créé avec succès!';
        RAISE NOTICE '  Email: superadmin@speedline.com';
        RAISE NOTICE '  UserId: 1';
        RAISE NOTICE '  Rôle: SUPER_ADMIN';
        RAISE NOTICE '  Le système est prêt à être utilisé!';
    ELSE
        RAISE NOTICE '✓ Profil SUPER_ADMIN déjà existant';
    END IF;
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE '⚠️  Impossible de créer le profil SUPER_ADMIN: %', SQLERRM;
        RAISE NOTICE 'ℹ️  Le profil sera créé automatiquement lors de la première connexion';
END $$;

-- Créer un index sur user_id si pas déjà existant
CREATE INDEX IF NOT EXISTS idx_admins_user_id_unique ON admins(user_id);

-- Commentaires pour documentation
COMMENT ON TABLE admins IS 'Table des profils administrateurs liés aux comptes auth-service';
COMMENT ON COLUMN admins.user_id IS 'Référence logique au users.id dans auth-service (pas de FK car bases séparées)';
