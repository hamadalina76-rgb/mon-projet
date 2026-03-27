export type DayOfWeek =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY'
  | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DAYS_OF_WEEK: DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
];

export interface ShiftDTO {
  id?: number;
  shiftOrder?: number;
  startTime: string;  // 'HH:mm'
  endTime: string;    // 'HH:mm'
  breakStart?: string; // 'HH:mm'
  breakEnd?: string;   // 'HH:mm'
}

export interface ScheduleTemplateResponse {
  id: number;
  name: string;
  description?: string;
  isActive: boolean;
  days: Record<DayOfWeek, ShiftDTO[]>;
  createdAt?: string;
}

export interface ScheduleTemplateCreateRequest {
  name: string;
  description?: string;
  days: Partial<Record<DayOfWeek, Array<{ startTime: string; endTime: string; breakStart?: string; breakEnd?: string }>>>;
}

export interface CourierScheduleResponse {
  id: number;
  courierId: number;
  templateId?: number;
  templateName?: string;
  weekStartDate?: string;
  isPermanent: boolean;
  isActive: boolean;
  days: Record<DayOfWeek, ShiftDTO[]>;
  createdAt?: string;
}

export interface CourierScheduleSaveRequest {
  templateId?: number;
  weekStartDate?: string;
  isPermanent?: boolean;
  days: Partial<Record<DayOfWeek, Array<{ startTime: string; endTime: string; breakStart?: string; breakEnd?: string }>>>;
}

export interface CopyDayRequest {
  sourceDay: DayOfWeek;
  targetDays: DayOfWeek[];
}

export interface CourierScheduleAuditLog {
  id: number;
  courierId: number;
  scheduleId?: number;
  action: string;
  templateId?: number;
  templateName?: string;
  details?: string;
  adminId?: number;
  adminName?: string;
  createdAt?: string;
}

export interface AuditLogPageResponse {
  content: CourierScheduleAuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Exceptional Schedules ───────────────────────────────────────────────────

export type ExceptionType = 'JOUR_FERIE' | 'EVENEMENT_SPECIAL' | 'CONGE' | 'FERMETURE' | 'FORMATION';

export const EXCEPTION_TYPES: ExceptionType[] = [
  'JOUR_FERIE', 'EVENEMENT_SPECIAL', 'CONGE', 'FERMETURE', 'FORMATION'
];

export interface CourierExceptionalSchedule {
  id: number;
  courierId: number;
  courierName?: string;
  exceptionType: ExceptionType;
  label: string;
  startDate: string;
  endDate: string;
  reason?: string;
  isRestPeriod: boolean;
  adminId?: number;
  adminName?: string;
  createdAt?: string;
}

export interface ExceptionalScheduleCreateRequest {
  courierId: number;
  exceptionType: ExceptionType;
  label: string;
  startDate: string;
  endDate: string;
  reason?: string;
  isRestPeriod: boolean;
}

export interface OverlapCheckResult {
  hasOverlap: boolean;
  overlapping: Array<{ id: number; label: string; startDate: string; endDate: string }>;
}
