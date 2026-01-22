// src/app/features/analytics/services/analytics.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private api = inject(ApiService);

  getOverview(period: string): Observable<any> {
    return this.api.get('partner/analytics/overview', { period });
  }

  getRevenueData(period: string): Observable<any> {
    return this.api.get('partner/analytics/revenue', { period });
  }

  getOrdersData(period: string): Observable<any> {
    return this.api.get('partner/analytics/orders', { period });
  }

  getPopularProducts(period: string): Observable<any> {
    return this.api.get('partner/analytics/popular-products', { period });
  }

  getPerformanceMetrics(period: string): Observable<any> {
    return this.api.get('partner/analytics/performance', { period });
  }

  getSalesReport(startDate: string, endDate: string): Observable<any> {
    return this.api.get('partner/analytics/sales-report', { startDate, endDate });
  }

  exportReport(type: string, format: string): Observable<Blob> {
    return this.api.get(`partner/analytics/export/${type}`, { format });
  }
}
