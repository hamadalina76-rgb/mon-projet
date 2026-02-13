-- Script pour créer le premier compte SUPER_ADMIN
-- À exécuter MANUELLEMENT après avoir démarré les services
-- Ce script crée:
-- 1. Un compte utilisateur dans auth-service (auth_db.users)
-- 2. Un profil admin dans user-service (user_db.admins)

-- ============================================
-- PARTIE 1: Créer le compte dans auth-service
-- ============================================
-- Base de données: auth_db
-- Password: "SuperAdmin@2024" (hashé avec BCrypt)

\c auth_db

INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    role,
    status,
    is_email_verified,
    is_phone_verified,
    auth_provider,
    created_at,
    updated_at
) VALUES (
    'superadmin@speedline.com',
    '$2a$10$YourBCryptHashedPasswordHere', -- Vous devez générer le hash BCrypt
    'Super',
    'Admin',
    'SUPER_ADMIN',
    'ACTIVE',
    true,
    false,
    'LOCAL',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING
RETURNING id;

-- Notez l'ID retourné, vous en aurez besoin pour la partie 2
-- Exemple: Si l'ID retourné est 1, utilisez-le ci-dessous


-- ============================================
-- PARTIE 2: Créer le profil admin dans user-service
-- ============================================
-- Base de données: user_db
-- Remplacez {{USER_ID}} par l'ID retourné ci-dessus

\c user_db

INSERT INTO admins (
    user_id,
    full_name,
    email,
    role_id,
    status,
    created_at,
    updated_at
) VALUES (
    1, -- REMPLACEZ PAR L'ID RETOURNÉ DE LA PARTIE 1
    'Super Admin',
    'superadmin@speedline.com',
    1, -- ID du rôle SUPER_ADMIN dans admin_roles
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;


-- ============================================
-- ALTERNATIVE: Utiliser l'API (Recommandé)
-- ============================================
-- Au lieu d'exécuter ce script SQL, vous pouvez créer le premier admin via l'API:
-- 
-- POST http://localhost:8083/api/admins/with-auth
-- Content-Type: application/json
-- 
-- {
--   "fullName": "Super Admin",
--   "email": "superadmin@speedline.com",
--   "password": "SuperAdmin@2024",
--   "roleId": 1,
--   "status": "ACTIVE"
-- }
--
-- Cette méthode est RECOMMANDÉE car elle:
-- - Hash automatiquement le mot de passe
-- - Crée les deux entrées (auth + profil) en une seule opération
-- - Gère les transactions correctement


-- ============================================
-- COMMENT GÉNÉRER LE HASH BCRYPT DU MOT DE PASSE
-- ============================================
-- Option 1: Utiliser un outil en ligne (pour dev seulement!)
-- https://bcrypt-generator.com/
-- Entrez "SuperAdmin@2024" et copiez le hash généré
--
-- Option 2: Utiliser Spring Boot (recommandé pour production)
-- Créez un simple test:
-- 
-- @Test
-- void generatePassword() {
--     BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
--     System.out.println(encoder.encode("SuperAdmin@2024"));
-- }


-- ============================================
-- VÉRIFICATION
-- ============================================
-- Pour vérifier que tout est bien créé:

-- Dans auth_db:
SELECT id, email, role, status FROM users WHERE email = 'superadmin@speedline.com';

-- Dans user_db:
SELECT a.id, a.user_id, a.full_name, a.email, r.label as role_label 
FROM admins a 
JOIN admin_roles r ON a.role_id = r.id 
WHERE a.email = 'superadmin@speedline.com';


-- ============================================
-- TEST DE CONNEXION
-- ============================================
-- Après création, testez la connexion:
--
-- POST http://localhost:8081/api/v1/auth/admin/login
-- Content-Type: application/json
--
-- {
--   "email": "superadmin@speedline.com",
--   "password": "SuperAdmin@2024"
-- }
--
-- Vous devriez recevoir un token JWT et les informations de l'utilisateur
