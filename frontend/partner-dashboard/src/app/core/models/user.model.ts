// src/app/core/models/user.model.ts

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  role: UserRole;
  partnerId?: string;
  partnerName?: string;
  avatar?: string;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export type UserRole = 'PARTNER_OWNER' | 'PARTNER_MANAGER' | 'PARTNER_STAFF';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
  expiresIn: number;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string;
  businessName: string;
  businessType: string;
  address: string;
  city: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirm {
  token: string;
  newPassword: string;
}
