// src/app/features/users/couriers/services/couriers.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class CouriersService {
  private api = inject(ApiService);

  getCouriers(
    page: number,
    pageSize: number,
    status?: string,
    search?: string,
    courierType?: string
  ): Observable<any> {
    let url = `admin/couriers?page=${page}&size=${pageSize}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (courierType) url += `&courierType=${encodeURIComponent(courierType)}`;
    if (search?.trim()) url += `&search=${encodeURIComponent(search.trim())}`;
    return this.api.get(url);
  }

  getCourier(id: string): Observable<any> {
    return this.api.get(`admin/couriers/${id}`);
  }

  getPendingApprovals(page: number = 0, pageSize: number = 20): Observable<any> {
    return this.api.get(`admin/couriers/pending?page=${page}&size=${pageSize}`);
  }

  approveCourier(id: string, courierType: 'INTERNAL' | 'EXTERNAL', zoneIds: number[] = []): Observable<any> {
    return this.api.post(`admin/couriers/${id}/approve`, { courierType, zoneIds });
  }

  assignZones(id: string, zoneIds: number[]): Observable<any> {
    return this.api.put(`admin/couriers/${id}/zones`, { zoneIds });
  }

  rejectCourier(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/reject`, { reason });
  }

  requestMoreInfo(id: string, message: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/request-more-info`, { message: message ?? '' });
  }

  deactivateCourier(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/deactivate`, { reason: reason ?? '' });
  }

  suspendCourier(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/suspend`, { reason: reason ?? '' });
  }

  activateCourier(id: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/activate`, {});
  }

  updateCourier(id: string, request: any): Observable<any> {
    return this.api.put(`admin/couriers/${id}`, request);
  }

  getChangeLogs(id: string, page: number = 0, size: number = 20): Observable<any> {
    return this.api.get(`admin/couriers/${id}/change-logs?page=${page}&size=${size}`);
  }
}
