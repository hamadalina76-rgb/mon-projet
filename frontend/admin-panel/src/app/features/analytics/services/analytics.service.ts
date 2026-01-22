// src/app/features/analytics/services/analytics.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private api = inject(ApiService);

  getRevenueStats(period: string): Observable<any> {
    const params = new HttpParams().set('period', period);
    return this.api.get('admin/analytics/revenue', params);
  }

  getOrderStats(period: string): Observable<any> {
    const params = new HttpParams().set('period', period);
    return this.api.get('admin/analytics/orders', params);
  }

  getUserStats(period: string): Observable<any> {
    const params = new HttpParams().set('period', period);
    return this.api.get('admin/analytics/users', params);
  }

  getPerformanceStats(): Observable<any> {
    return this.api.get('admin/analytics/performance');
  }

  exportReport(type: string, period: string): Observable<Blob> {
    const params = new HttpParams().set('period', period);
    return this.api.get(`admin/analytics/export/${type}`, params);
  }
}
