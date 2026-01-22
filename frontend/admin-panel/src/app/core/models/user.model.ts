// src/app/core/models/user.model.ts

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: AdminRole;
  permissions: string[];
  avatar?: string;
  status: UserStatus;
  createdAt: string;
  lastLoginAt?: string;
}

export type AdminRole = 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR' | 'SUPPORT';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface LoginRequest {
  email: string;
  password: string;
  twoFactorCode?: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: AdminUser;
  expiresIn: number;
}

// Permissions
export const PERMISSIONS = {
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

  // Settings
  SETTINGS_VIEW: 'settings:view',
  SETTINGS_EDIT: 'settings:edit',

  // Analytics
  ANALYTICS_VIEW: 'analytics:view',
  ANALYTICS_EXPORT: 'analytics:export',

  // Monitoring
  MONITORING_VIEW: 'monitoring:view',
} as const;
