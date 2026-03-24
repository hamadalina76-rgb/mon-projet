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
