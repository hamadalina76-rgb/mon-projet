-- ============================================
-- User Service - Admin Module Initial Data
-- Version: 4
-- Description: Insertion des rôles et permissions par défaut
-- ============================================

-- Insertion des rôles par défaut
INSERT INTO admin_roles (code, label, description, color, trust_level, max_refund_amount, requires_approval, active) VALUES
('SUPER_ADMIN', 'Super Admin', 'Accès complet à toutes les fonctionnalités du système', '#E31E24', 100, 10000.00, false, true),
('ADMIN', 'Admin', 'Gestion des utilisateurs, commandes et partenaires', '#FF6B35', 80, 5000.00, false, true),
('MODERATOR', 'Modérateur', 'Modération du contenu et support client avancé', '#4ECDC4', 60, 1000.00, true, true),
('SUPPORT', 'Support', 'Support client et consultation des données', '#95E1D3', 40, 500.00, true, true),
('VIEWER', 'Visualiseur', 'Consultation en lecture seule', '#F0F0F0', 20, 0.00, true, true);

-- Permissions pour SUPER_ADMIN (accès complet)
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord principal', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'users', 'Utilisateurs', 'people', 'Gestion complète des clients et livreurs', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'admins', 'Administrateurs', 'admin_panel_settings', 'Gestion des administrateurs et rôles', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'orders', 'Commandes', 'shopping_cart', 'Gestion complète des commandes', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'partners', 'Partenaires', 'store', 'Gestion des restaurants et partenaires', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'delivery', 'Livraisons', 'local_shipping', 'Gestion et suivi des livraisons', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'payments', 'Paiements', 'payment', 'Gestion des paiements et transactions', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'promotions', 'Promotions', 'local_offer', 'Gestion des promotions et codes promo', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'reviews', 'Avis', 'star', 'Modération des avis et notes', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'analytics', 'Analytiques', 'analytics', 'Accès aux statistiques et rapports', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'notifications', 'Notifications', 'notifications', 'Envoi et gestion des notifications', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'settings', 'Paramètres', 'settings', 'Configuration système complète', true);

-- Permissions pour ADMIN
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'users', 'Utilisateurs', 'people', 'Gestion des utilisateurs', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'orders', 'Commandes', 'shopping_cart', 'Gestion des commandes', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'partners', 'Partenaires', 'store', 'Gestion des partenaires', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'delivery', 'Livraisons', 'local_shipping', 'Gestion des livraisons', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'payments', 'Paiements', 'payment', 'Gestion des paiements', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'promotions', 'Promotions', 'local_offer', 'Gestion des promotions', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'reviews', 'Avis', 'star', 'Modération des avis', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'analytics', 'Analytiques', 'analytics', 'Accès aux statistiques', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'notifications', 'Notifications', 'notifications', 'Envoi de notifications', true);

-- Permissions pour MODERATOR
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'MODERATOR'), 'dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true),
((SELECT id FROM admin_roles WHERE code = 'MODERATOR'), 'users', 'Utilisateurs', 'people', 'Consultation des utilisateurs', true),
((SELECT id FROM admin_roles WHERE code = 'MODERATOR'), 'orders', 'Commandes', 'shopping_cart', 'Consultation des commandes', true),
((SELECT id FROM admin_roles WHERE code = 'MODERATOR'), 'reviews', 'Avis', 'star', 'Modération des avis et commentaires', true),
((SELECT id FROM admin_roles WHERE code = 'MODERATOR'), 'notifications', 'Notifications', 'notifications', 'Envoi de notifications aux utilisateurs', true);

-- Permissions pour SUPPORT
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'SUPPORT'), 'dashboard', 'Tableau de bord', 'dashboard', 'Accès au tableau de bord', true),
((SELECT id FROM admin_roles WHERE code = 'SUPPORT'), 'users', 'Utilisateurs', 'people', 'Consultation des utilisateurs', true),
((SELECT id FROM admin_roles WHERE code = 'SUPPORT'), 'orders', 'Commandes', 'shopping_cart', 'Consultation et assistance commandes', true),
((SELECT id FROM admin_roles WHERE code = 'SUPPORT'), 'notifications', 'Notifications', 'notifications', 'Envoi de notifications support', true);

-- Permissions pour VIEWER
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'VIEWER'), 'dashboard', 'Tableau de bord', 'dashboard', 'Consultation du tableau de bord', true),
((SELECT id FROM admin_roles WHERE code = 'VIEWER'), 'analytics', 'Analytiques', 'analytics', 'Consultation des statistiques', true);
