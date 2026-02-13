-- Script d'initialisation des rôles et permissions pour le module Admin
-- À exécuter après la création des tables

-- Insérer les rôles admin
INSERT INTO admin_roles (code, label, description, color, trust_level, max_refund_amount, requires_approval, active, created_at) VALUES
('SUPER_ADMIN', 'Super Admin', 'Accès complet à toutes les fonctionnalités', '#E31E24', 100, 10000.00, false, true, CURRENT_TIMESTAMP),
('ADMIN', 'Admin', 'Gestion des utilisateurs et commandes', '#FF6B35', 80, 5000.00, false, true, CURRENT_TIMESTAMP),
('MODERATOR', 'Modérateur', 'Modération du contenu et support client', '#4ECDC4', 60, 1000.00, true, true, CURRENT_TIMESTAMP),
('SUPPORT', 'Support', 'Support client uniquement', '#95E1D3', 40, 500.00, true, true, CURRENT_TIMESTAMP),
('VIEWER', 'Visualiseur', 'Lecture seule', '#F0F0F0', 20, 0.00, true, true, CURRENT_TIMESTAMP);

-- Récupérer les IDs des rôles (pour référence)
-- Note: Les IDs seront auto-générés, ajuster selon votre séquence

-- Insérer les permissions pour SUPER_ADMIN
INSERT INTO permissions (module, module_label, icon, description, enabled, role_id) VALUES
('dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('users', 'Utilisateurs', 'people', 'Gestion des utilisateurs (clients, livreurs)', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('admins', 'Administrateurs', 'admin_panel_settings', 'Gestion des administrateurs', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('orders', 'Commandes', 'shopping_cart', 'Gestion des commandes', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('partners', 'Partenaires', 'store', 'Gestion des restaurants/partenaires', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('delivery', 'Livraisons', 'local_shipping', 'Gestion des livraisons', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('payments', 'Paiements', 'payment', 'Gestion des paiements et transactions', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('promotions', 'Promotions', 'local_offer', 'Gestion des promotions et codes promo', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('reviews', 'Avis', 'star', 'Modération des avis et notes', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('analytics', 'Analytiques', 'analytics', 'Accès aux statistiques et rapports', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('notifications', 'Notifications', 'notifications', 'Envoi de notifications', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN')),
('settings', 'Paramètres', 'settings', 'Configuration système', true, (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'));

-- Insérer les permissions pour ADMIN
INSERT INTO permissions (module, module_label, icon, description, enabled, role_id) VALUES
('dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('users', 'Utilisateurs', 'people', 'Gestion des utilisateurs (clients, livreurs)', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('orders', 'Commandes', 'shopping_cart', 'Gestion des commandes', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('partners', 'Partenaires', 'store', 'Gestion des restaurants/partenaires', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('delivery', 'Livraisons', 'local_shipping', 'Gestion des livraisons', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('payments', 'Paiements', 'payment', 'Gestion des paiements', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('promotions', 'Promotions', 'local_offer', 'Gestion des promotions', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('reviews', 'Avis', 'star', 'Modération des avis', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('analytics', 'Analytiques', 'analytics', 'Accès aux statistiques', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN')),
('notifications', 'Notifications', 'notifications', 'Envoi de notifications', true, (SELECT id FROM admin_roles WHERE code = 'ADMIN'));

-- Insérer les permissions pour MODERATOR
INSERT INTO permissions (module, module_label, icon, description, enabled, role_id) VALUES
('dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true, (SELECT id FROM admin_roles WHERE code = 'MODERATOR')),
('users', 'Utilisateurs', 'people', 'Consultation des utilisateurs', true, (SELECT id FROM admin_roles WHERE code = 'MODERATOR')),
('orders', 'Commandes', 'shopping_cart', 'Consultation des commandes', true, (SELECT id FROM admin_roles WHERE code = 'MODERATOR')),
('reviews', 'Avis', 'star', 'Modération des avis', true, (SELECT id FROM admin_roles WHERE code = 'MODERATOR')),
('notifications', 'Notifications', 'notifications', 'Envoi de notifications', true, (SELECT id FROM admin_roles WHERE code = 'MODERATOR'));

-- Insérer les permissions pour SUPPORT
INSERT INTO permissions (module, module_label, icon, description, enabled, role_id) VALUES
('dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true, (SELECT id FROM admin_roles WHERE code = 'SUPPORT')),
('users', 'Utilisateurs', 'people', 'Consultation des utilisateurs', true, (SELECT id FROM admin_roles WHERE code = 'SUPPORT')),
('orders', 'Commandes', 'shopping_cart', 'Consultation des commandes', true, (SELECT id FROM admin_roles WHERE code = 'SUPPORT')),
('notifications', 'Notifications', 'notifications', 'Envoi de notifications', true, (SELECT id FROM admin_roles WHERE code = 'SUPPORT'));

-- Insérer les permissions pour VIEWER
INSERT INTO permissions (module, module_label, icon, description, enabled, role_id) VALUES
('dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true, (SELECT id FROM admin_roles WHERE code = 'VIEWER')),
('analytics', 'Analytiques', 'analytics', 'Consultation des statistiques', true, (SELECT id FROM admin_roles WHERE code = 'VIEWER'));

-- Créer un admin par défaut (Super Admin)
-- Note: Ajuster le userId selon votre système d'authentification
INSERT INTO admins (user_id, full_name, email, role_id, status, created_by, created_at, updated_at) VALUES
(1, 'Admin Système', 'admin@speedline.com', (SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Afficher un résumé
SELECT 'Rôles créés:' as info, COUNT(*) as count FROM admin_roles
UNION ALL
SELECT 'Permissions créées:' as info, COUNT(*) as count FROM permissions
UNION ALL
SELECT 'Admins créés:' as info, COUNT(*) as count FROM admins;
