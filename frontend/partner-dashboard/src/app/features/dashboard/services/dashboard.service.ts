// src/app/features/dashboard/services/dashboard.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private api = inject(ApiService);

  getDashboardStats(): Observable<any> {
    return this.api.get('partner/dashboard/stats');
  }

  getOrdersChart(period: string): Observable<any> {
    return this.api.get('partner/dashboard/orders-chart', { period });
  }

  getRecentOrders(): Observable<any> {
    return this.api.get('partner/dashboard/recent-orders');
  }

  getPopularProducts(): Observable<any> {
    return this.api.get('partner/dashboard/popular-products');
  }
}
