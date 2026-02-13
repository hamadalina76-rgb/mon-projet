// src/app/core/models/role.model.ts

// ============================================================================
// RBAC - Role-Based Access Control
// ============================================================================

export type AdminRole =
  | 'SUPER_ADMIN'
  | 'ADMIN'
  | 'FINANCE_ADMIN'
  | 'SUPPORT_ADMIN'
  | 'CONTENT_MODERATOR';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

// All granular permissions
export const PERMISSIONS = {
  // Dashboard
  DASHBOARD_VIEW: 'dashboard:view',

  // Delivery
  DELIVERY_VIEW: 'delivery:view',
  DELIVERY_MANAGE: 'delivery:manage',

  // Users
  USERS_VIEW: 'users:view',
  USERS_CREATE: 'users:create',
  USERS_EDIT: 'users:edit',
  USERS_DELETE: 'users:delete',
  USERS_BLOCK: 'users:block',

  // Partners
  PARTNERS_VIEW: 'partners:view',
  PARTNERS_APPROVE: 'partners:approve',
  PARTNERS_EDIT: 'partners:edit',
  PARTNERS_DELETE: 'partners:delete',

  // Orders
  ORDERS_VIEW: 'orders:view',
  ORDERS_MANAGE: 'orders:manage',
  ORDERS_REFUND: 'orders:refund',

  // Payments
  PAYMENTS_VIEW: 'payments:view',
  PAYMENTS_MANAGE: 'payments:manage',

  // Promotions
  PROMOTIONS_VIEW: 'promotions:view',
  PROMOTIONS_CREATE: 'promotions:create',
  PROMOTIONS_EDIT: 'promotions:edit',
  PROMOTIONS_DELETE: 'promotions:delete',

  // Reviews
  REVIEWS_VIEW: 'reviews:view',
  REVIEWS_MODERATE: 'reviews:moderate',

  // Support
  SUPPORT_VIEW: 'support:view',
  SUPPORT_MANAGE: 'support:manage',

  // Zones
  ZONES_VIEW: 'zones:view',
  ZONES_MANAGE: 'zones:manage',

  // Settings
  SETTINGS_VIEW: 'settings:view',
  SETTINGS_EDIT: 'settings:edit',

  // Analytics
  ANALYTICS_VIEW: 'analytics:view',
  ANALYTICS_EXPORT: 'analytics:export',

  // Monitoring
  MONITORING_VIEW: 'monitoring:view',

  // Notifications
  NOTIFICATIONS_VIEW: 'notifications:view',
  NOTIFICATIONS_SEND: 'notifications:send',

  // Admin management
  ADMINS_VIEW: 'admins:view',
  ADMINS_CREATE: 'admins:create',
  ADMINS_EDIT: 'admins:edit',
  ADMINS_DELETE: 'admins:delete',
} as const;

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];

// Role → Permissions mapping
export const ROLE_PERMISSIONS: Record<AdminRole, Permission[]> = {
  // Super Admin: accès total
  SUPER_ADMIN: Object.values(PERMISSIONS),

  // Admin: dashboard by default, will be overridden by customPermissions at login
  ADMIN: [
    PERMISSIONS.DASHBOARD_VIEW,
  ],


  // Finance Admin: paiements uniquement
  FINANCE_ADMIN: [
    PERMISSIONS.PAYMENTS_VIEW,
    PERMISSIONS.PAYMENTS_MANAGE,
    PERMISSIONS.ORDERS_VIEW,
    PERMISSIONS.ORDERS_REFUND,
    PERMISSIONS.ANALYTICS_VIEW,
    PERMISSIONS.ANALYTICS_EXPORT,
  ],

  // Support Admin: tickets uniquement
  SUPPORT_ADMIN: [
    PERMISSIONS.SUPPORT_VIEW,
    PERMISSIONS.SUPPORT_MANAGE,
    PERMISSIONS.ORDERS_VIEW,
    PERMISSIONS.USERS_VIEW,
    PERMISSIONS.NOTIFICATIONS_VIEW,
    PERMISSIONS.NOTIFICATIONS_SEND,
  ],

  // Content Moderator: avis, partenaires
  CONTENT_MODERATOR: [
    PERMISSIONS.REVIEWS_VIEW,
    PERMISSIONS.REVIEWS_MODERATE,
    PERMISSIONS.PARTNERS_VIEW,
    PERMISSIONS.PARTNERS_APPROVE,
    PERMISSIONS.PARTNERS_EDIT,
    PERMISSIONS.PROMOTIONS_VIEW,
    PERMISSIONS.PROMOTIONS_EDIT,
    PERMISSIONS.NOTIFICATIONS_VIEW,
  ],
};

// Human-readable role labels
export const ROLE_LABELS: Record<AdminRole, { en: string; fr: string; ar: string }> = {
  SUPER_ADMIN:      { en: 'Super Admin',       fr: 'Super Administrateur', ar: 'المسؤول الأعلى' },
  ADMIN:            { en: 'Admin',             fr: 'Administrateur',       ar: 'المسؤول' },
  FINANCE_ADMIN:    { en: 'Finance Admin',      fr: 'Admin Finance',       ar: 'مسؤول المالية' },
  SUPPORT_ADMIN:    { en: 'Support Admin',      fr: 'Admin Support',       ar: 'مسؤول الدعم' },
  CONTENT_MODERATOR:{ en: 'Content Moderator',  fr: 'Modérateur Contenu',  ar: 'مشرف المحتوى' },
};
