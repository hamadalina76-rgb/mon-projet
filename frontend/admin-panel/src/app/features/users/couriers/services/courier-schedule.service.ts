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
  CourierExceptionalSchedule,
  ExceptionalScheduleCreateRequest,
  OverlapCheckResult,
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

  applyTemplate(courierId: string, templateId: number, effectiveFrom?: string): Observable<CourierScheduleResponse> {
    let url = `admin/couriers/${courierId}/schedule/apply-template/${templateId}`;
    if (effectiveFrom) {
      url += `?effectiveFrom=${encodeURIComponent(effectiveFrom)}`;
    }
    return this.api.post(url, {});
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

  // ── Exceptional Schedules ─────────────────────────────────────────────────

  getExceptionalSchedulesByCourier(
    courierId: string,
    from?: string,
    to?: string
  ): Observable<CourierExceptionalSchedule[]> {
    let url = `admin/exceptional-schedules/courier/${courierId}`;
    const params: string[] = [];
    if (from) params.push(`from=${from}`);
    if (to)   params.push(`to=${to}`);
    if (params.length) url += '?' + params.join('&');
    return this.api.get(url);
  }

  getAllExceptionalSchedules(
    page: number = 0,
    size: number = 20,
    q?: string,
    exceptionType?: string,
    from?: string,
    to?: string,
    courierId?: number,
    unavailabilityReason?: string,
    validationStatus?: string,
    courierType?: 'INTERNAL' | 'EXTERNAL'
  ): Observable<{ content: CourierExceptionalSchedule[]; totalElements: number }> {
    let url = `admin/exceptional-schedules?page=${page}&size=${size}`;
    if (q)             url += `&search=${encodeURIComponent(q)}`;
    if (exceptionType) url += `&exceptionType=${exceptionType}`;
    if (from)          url += `&dateFrom=${from}`;
    if (to)            url += `&dateTo=${to}`;
    if (courierId)     url += `&courierId=${courierId}`;
    if (unavailabilityReason) url += `&unavailabilityReason=${unavailabilityReason}`;
    if (validationStatus)     url += `&validationStatus=${validationStatus}`;
    if (courierType)          url += `&courierType=${courierType}`;
    return this.api.get(url);
  }

  checkExceptionalOverlap(
    courierId: number,
    startDate: string,
    endDate: string,
    excludeId?: number
  ): Observable<OverlapCheckResult> {
    let url = `admin/exceptional-schedules/check-overlap?courierId=${courierId}&startDate=${startDate}&endDate=${endDate}`;
    if (excludeId !== undefined) url += `&excludeId=${excludeId}`;
    return this.api.get(url);
  }

  createExceptionalSchedule(
    req: ExceptionalScheduleCreateRequest
  ): Observable<CourierExceptionalSchedule> {
    return this.api.post('admin/exceptional-schedules', req);
  }

  updateExceptionalSchedule(
    id: number,
    req: ExceptionalScheduleCreateRequest
  ): Observable<CourierExceptionalSchedule> {
    return this.api.put(`admin/exceptional-schedules/${id}`, req);
  }

  approveExceptionalSchedule(
    id: number,
    req: { comment?: string; exceptionType?: string; label?: string; startDate?: string; endDate?: string; startsAt?: string; endsAt?: string; reason?: string; isRestPeriod?: boolean }
  ): Observable<CourierExceptionalSchedule> {
    return this.api.post(`admin/exceptional-schedules/${id}/approve`, req);
  }

  rejectExceptionalSchedule(
    id: number,
    comment?: string
  ): Observable<CourierExceptionalSchedule> {
    return this.api.post(`admin/exceptional-schedules/${id}/reject`, { comment });
  }

  deleteExceptionalSchedule(id: number): Observable<void> {
    return this.api.delete(`admin/exceptional-schedules/${id}`);
  }
}
