-- ============================================
-- Corrige le nom de la table pour Hibernate (PostgreSQL lowercase)
-- V6 a créé "ADM_ZONES" (guillemets) ; Hibernate valide "adm_zones".
-- ============================================

ALTER TABLE IF EXISTS "ADM_ZONES" RENAME TO adm_zones;
