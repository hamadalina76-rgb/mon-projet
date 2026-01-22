// src/app/core/models/api-response.model.ts

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: FieldError[];
  timestamp: string;
  path: string;
}

export interface FieldError {
  field: string;
  message: string;
}
