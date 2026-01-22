// src/app/features/dashboard/services/dashboard.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private api = inject(ApiService);

  // TODO: Implement dashboard service methods
  
  getPlatformStats(): Observable<any> {
    return this.api.get('admin/dashboard/stats');
  }

  getRealtimeOrders(): Observable<any> {
    return this.api.get('admin/dashboard/realtime-orders');
  }

  getActiveUsers(): Observable<any> {
    return this.api.get('admin/dashboard/active-users');
  }

  getRevenueChart(period: string): Observable<any> {
    return this.api.get(`admin/dashboard/revenue?period=${period}`);
  }
}
