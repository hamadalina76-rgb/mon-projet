import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import {
  ScheduleTemplateResponse,
  ScheduleTemplateCreateRequest,
  CourierScheduleResponse,
  CourierScheduleSaveRequest,
  CopyDayRequest,
  CourierScheduleAuditLog,
  AuditLogPageResponse,
} from '../models/courier-schedule.model';

@Injectable({ providedIn: 'root' })
export class CourierScheduleService {
  private api = inject(ApiService);

  // ── Schedule Templates ──────────────────────────────────────────────────

  getTemplates(page: number = 0, size: number = 10, search?: string, isActive?: boolean): Observable<any> {
    let url = `admin/schedule-templates?page=${page}&size=${size}`;
    if (search?.trim()) url += `&search=${encodeURIComponent(search.trim())}`;
    if (isActive !== undefined && isActive !== null) url += `&isActive=${isActive}`;
    return this.api.get(url);
  }

  getActiveTemplates(): Observable<ScheduleTemplateResponse[]> {
    return this.api.get('admin/schedule-templates/active');
  }

  getTemplate(id: number): Observable<ScheduleTemplateResponse> {
    return this.api.get(`admin/schedule-templates/${id}`);
  }

  createTemplate(req: ScheduleTemplateCreateRequest): Observable<ScheduleTemplateResponse> {
    return this.api.post('admin/schedule-templates', req);
  }

  updateTemplate(id: number, req: ScheduleTemplateCreateRequest): Observable<ScheduleTemplateResponse> {
    return this.api.put(`admin/schedule-templates/${id}`, req);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.api.delete(`admin/schedule-templates/${id}`);
  }

  toggleTemplate(id: number): Observable<void> {
    return this.api.patch(`admin/schedule-templates/${id}/toggle`, {});
  }

  // ── Courier Schedules ───────────────────────────────────────────────────

  getAllSchedules(courierId: string): Observable<CourierScheduleResponse[]> {
    return this.api.get(`admin/couriers/${courierId}/schedule/all`);
  }

  getCourierSchedule(courierId: string): Observable<CourierScheduleResponse> {
    return this.api.get(`admin/couriers/${courierId}/schedule`);
  }

  saveCourierSchedule(courierId: string, req: CourierScheduleSaveRequest): Observable<CourierScheduleResponse> {
    return this.api.post(`admin/couriers/${courierId}/schedule`, req);
  }

  applyTemplate(courierId: string, templateId: number): Observable<CourierScheduleResponse> {
    return this.api.post(`admin/couriers/${courierId}/schedule/apply-template/${templateId}`, {});
  }

  copyDay(courierId: string, req: CopyDayRequest): Observable<CourierScheduleResponse> {
    return this.api.post(`admin/couriers/${courierId}/schedule/copy-day`, req);
  }

  getAuditLogs(
    courierId: string,
    page: number = 0,
    size: number = 10,
    action?: string,
    dateFrom?: string,
    dateTo?: string
  ): Observable<AuditLogPageResponse> {
    let url = `admin/couriers/${courierId}/schedule/audit-logs?page=${page}&size=${size}`;
    if (action)   url += `&action=${encodeURIComponent(action)}`;
    if (dateFrom) url += `&dateFrom=${encodeURIComponent(dateFrom)}`;
    if (dateTo)   url += `&dateTo=${encodeURIComponent(dateTo)}`;
    return this.api.get(url);
  }
}
