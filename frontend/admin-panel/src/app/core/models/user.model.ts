// src/app/core/models/user.model.ts

import { AdminRole, UserStatus } from './role.model';

export { AdminRole, UserStatus } from './role.model';

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: AdminRole;
  permissions: string[];
  avatar?: string;
  phone?: string;
  status: UserStatus;
  createdAt: string;
  lastLoginAt?: string;
  twoFactorEnabled?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
  twoFactorCode?: string;
}

// Maps to AuthResponse from backend (snake_case via @JsonProperty)
export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expiresIn: number;
  user: BackendUserInfo;
  isNewUser?: boolean;
}

// Raw user info from backend AuthResponse.UserInfo
export interface BackendUserInfo {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  profilePicture?: string;
}

/** Response of GET /api/v1/auth/profile */
export interface ProfileResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  profilePicture?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otpCode: string;
  newPassword: string;
}

export interface AdminActivityLog {
  id: string;
  adminId: string;
  adminName: string;
  action: string;
  resource: string;
  resourceId?: string;
  details?: string;
  ipAddress?: string;
  userAgent?: string;
  timestamp: string;
}
