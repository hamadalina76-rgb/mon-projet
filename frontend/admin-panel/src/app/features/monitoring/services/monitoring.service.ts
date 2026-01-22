// src/app/features/monitoring/services/monitoring.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class MonitoringService {
  private api = inject(ApiService);

  getSystemHealth(): Observable<any> {
    return this.api.get('admin/monitoring/health');
  }

  getServiceStatus(): Observable<any> {
    return this.api.get('admin/monitoring/services');
  }

  getErrorLogs(page: number, pageSize: number): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.api.get('admin/monitoring/logs/errors', params);
  }

  getPerformanceMetrics(): Observable<any> {
    return this.api.get('admin/monitoring/performance');
  }

  getAuditLogs(page: number, pageSize: number): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.api.get('admin/monitoring/audit', params);
  }
}
