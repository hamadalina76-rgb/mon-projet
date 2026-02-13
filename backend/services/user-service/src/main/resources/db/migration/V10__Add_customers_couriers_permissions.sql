-- ============================================
-- User Service - Add Customers and Couriers Permissions
-- Version: 10
-- Description: Ajouter les modules customers et couriers
--              pour compléter les permissions utilisateurs
-- ============================================

-- Ajouter customers et couriers pour SUPER_ADMIN
INSERT INTO permissions (role_id, module, module_label, icon, description, description_en, description_ar, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'customers', 'Clients', 'person', 'Gestion complète des clients', 'Complete customer management', 'إدارة كاملة للعملاء', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'couriers', 'Livreurs', 'directions_bike', 'Gestion complète des livreurs', 'Complete courier management', 'إدارة كاملة للسعاة', true);

-- Ajouter customers et couriers pour ADMIN
INSERT INTO permissions (role_id, module, module_label, icon, description, description_en, description_ar, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'customers', 'Clients', 'person', 'Gestion des clients', 'Customer management', 'إدارة العملاء', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'couriers', 'Livreurs', 'directions_bike', 'Gestion des livreurs', 'Courier management', 'إدارة السعاة', true);
