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
    search?: string
  ): Observable<any> {
    let url = `admin/couriers?page=${page}&size=${pageSize}`;
    if (status) url += `&status=${encodeURIComponent(status)}`;
    if (search?.trim()) url += `&search=${encodeURIComponent(search.trim())}`;
    return this.api.get(url);
  }

  getCourier(id: string): Observable<any> {
    return this.api.get(`admin/couriers/${id}`);
  }

  getPendingApprovals(page: number = 0, pageSize: number = 20): Observable<any> {
    return this.api.get(`admin/couriers/pending?page=${page}&size=${pageSize}`);
  }

  approveCourier(id: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/approve`, {});
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
}
