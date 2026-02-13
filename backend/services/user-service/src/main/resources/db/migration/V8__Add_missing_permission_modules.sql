-- ============================================
-- User Service - Add Missing Permission Modules
-- Version: 8
-- Description: Ajouter les modules support, zones et monitoring
--              qui existent dans le frontend mais pas dans le backend
-- ============================================

-- Ajouter support, zones, monitoring pour SUPER_ADMIN
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'support', 'Support', 'support_agent', 'Gestion des tickets de support client', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'zones', 'Zones', 'map', 'Gestion des zones de livraison', true),
((SELECT id FROM admin_roles WHERE code = 'SUPER_ADMIN'), 'monitoring', 'Monitoring', 'monitor_heart', 'Surveillance et état du système', true);

-- Ajouter support, zones, monitoring pour ADMIN
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'support', 'Support', 'support_agent', 'Gestion du support client', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'zones', 'Zones', 'map', 'Gestion des zones de livraison', true),
((SELECT id FROM admin_roles WHERE code = 'ADMIN'), 'monitoring', 'Monitoring', 'monitor_heart', 'Surveillance du système', true);

-- Ajouter support pour SUPPORT role
INSERT INTO permissions (role_id, module, module_label, icon, description, enabled) VALUES
((SELECT id FROM admin_roles WHERE code = 'SUPPORT'), 'support', 'Support', 'support_agent', 'Accès aux tickets de support', true);
