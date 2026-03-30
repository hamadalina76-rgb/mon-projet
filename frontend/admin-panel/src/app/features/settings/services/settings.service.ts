// src/app/features/settings/services/settings.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface GeneralSettingsResponse {
  platformName: string;
  contactEmail: string;
  contactPhone: string;
  defaultLanguage: string;
  defaultCurrency: string;
  maintenanceMode: boolean;
  appEnabled: boolean;
}

export interface AppStatusResponse {
  appEnabled: boolean;
  maintenanceMode: boolean;
  currentlyOpen: boolean;
  currentDay: string;
  todayOpenTime: string | null;
  todayCloseTime: string | null;
  todayIsOpen: boolean;
}

export interface DaySchedule {
  dayOfWeek: string;
  isOpen: boolean;
  openTime: string | null;
  closeTime: string | null;
}

export interface WeeklyScheduleResponse {
  days: DaySchedule[];
}

export interface AuditLogEntry {
  id: number;
  action: string;
  adminId: number | null;
  adminName: string;
  details: string;
  createdAt: string;
}

export interface AuditLogListResponse {
  logs: AuditLogEntry[];
}

export interface AuditLogFilter {
  action?: string;
  adminName?: string;
  date?: string;  // YYYY-MM-DD
  page?: number;
  size?: number;
}

export interface PagedAuditLogResponse {
  logs: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private api = inject(ApiService);

  // ── General Settings ────────────────────────────────────────────────

  getGeneralSettings(): Observable<GeneralSettingsResponse> {
    return this.api.get('admin/settings/general');
  }

  updateGeneralSettings(data: Partial<GeneralSettingsResponse>): Observable<GeneralSettingsResponse> {
    return this.api.put('admin/settings/general', data);
  }

  // ── App Status (public) ─────────────────────────────────────────────

  getAppStatus(): Observable<AppStatusResponse> {
    return this.api.get('admin/settings/app-status');
  }

  // ── Other settings ──────────────────────────────────────────────────

  getPaymentSettings(): Observable<any> {
    return this.api.get('admin/settings/payment');
  }

  updatePaymentSettings(data: any): Observable<any> {
    return this.api.put('admin/settings/payment', data);
  }

  getNotificationSettings(): Observable<any> {
    return this.api.get('admin/settings/notifications');
  }

  updateNotificationSettings(data: any): Observable<any> {
    return this.api.put('admin/settings/notifications', data);
  }

  getSecuritySettings(): Observable<any> {
    return this.api.get('admin/settings/security');
  }

  updateSecuritySettings(data: any): Observable<any> {
    return this.api.put('admin/settings/security', data);
  }

  // ── Working Hours ────────────────────────────────────────────────────

  getWorkingHours(): Observable<WeeklyScheduleResponse> {
    return this.api.get('admin/settings/working-hours');
  }

  updateWorkingHours(data: WeeklyScheduleResponse): Observable<WeeklyScheduleResponse> {
    return this.api.put('admin/settings/working-hours', data);
  }

  // ── Audit Logs ───────────────────────────────────────────────────────

  getAuditLogs(filter?: AuditLogFilter): Observable<PagedAuditLogResponse> {
    const params: string[] = [];
    if (filter?.action)    params.push(`action=${encodeURIComponent(filter.action)}`);
    if (filter?.adminName) params.push(`adminName=${encodeURIComponent(filter.adminName)}`);
    if (filter?.date)      params.push(`date=${encodeURIComponent(filter.date)}`);
    params.push(`page=${filter?.page ?? 0}`);
    params.push(`size=${filter?.size ?? 10}`);
    return this.api.get(`admin/settings/audit-logs?${params.join('&')}`);
  }
}
