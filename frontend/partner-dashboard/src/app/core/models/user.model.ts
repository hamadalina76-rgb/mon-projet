// src/app/core/models/user.model.ts

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  phoneNumber?: string;
  role: UserRole;
  partnerId?: number;
  partnerName?: string;
  profilePicture?: string;
  avatar?: string;
  status: UserStatus;
  isProfileComplete?: boolean;
  createdAt: string;
  updatedAt: string;
}

export type UserRole = 'CUSTOMER' | 'COURIER' | 'PARTNER' | 'ADMIN' | 'SUPER_ADMIN' | 'PARTNER_OWNER' | 'PARTNER_MANAGER' | 'PARTNER_STAFF';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';

// ==================== AUTH REQUEST/RESPONSE (matching backend) ====================

/** POST /api/v1/auth/register */
export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: 'PARTNER';
}

/** Response from POST /api/v1/auth/register */
export interface RegisterResponse {
  message: string;
}

/** POST /api/v1/auth/login */
export interface LoginRequest {
  email: string;
  password: string;
  deviceInfo?: string;
}

/** Response from POST /api/v1/auth/login → OTP sent */
export interface OtpResponse {
  message: string;
  email: string;
  otpSent: boolean;
  expirationMinutes: number;
}

/** POST /api/v1/auth/verify-otp */
export interface VerifyOtpRequest {
  email: string;
  otpCode: string;
  type: 'LOGIN' | 'FORGOT_PASSWORD';
}

/** Response from POST /api/v1/auth/verify-otp → tokens */
export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expiresIn: number;
  isNewUser: boolean;
  user: User;
}

// Kept for backward compatibility
export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
  expiresIn: number;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirm {
  token: string;
  newPassword: string;
}
