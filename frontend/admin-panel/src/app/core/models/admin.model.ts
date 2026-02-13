// src/app/core/models/admin.model.ts

export interface Admin {
  id: string;
  fullName: string;
  email: string;
  role: AdminRole;
  lastLogin: Date | string;
  status: AdminStatus;
  avatar?: string;
  permissions?: string[];
  createdAt?: Date | string;
}

export interface AdminRole {
  id: string;
  name: string;
  label: string;
  color: string;
  permissions: Permission[];
  trustLevel?: number;
  maxRefundAmount?: number;
  requiresApproval?: boolean;
}

export interface Permission {
  module: string;
  moduleLabel: string;
  icon: string;
  description: string;
  descriptionEn?: string;
  descriptionAr?: string;
  enabled: boolean;
  actions?: string[];
}

export enum AdminStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  PENDING = 'PENDING',
  SUSPENDED = 'SUSPENDED'
}

export interface RoleDefinition {
  id: string;
  name: string;
  label: string;
  description: string;
  trustLevel: number;
  maxRefundAmount: number;
  permissions: Permission[];
}
