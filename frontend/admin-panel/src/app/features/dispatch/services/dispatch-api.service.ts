import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import {
  DispatchCourierPosition,
  DispatchDashboardKpis,
  DispatchPendingOrder,
  DispatchZoneMetrics,
  ManualAssignPayload,
  ManualBundlePayload,
} from '../models/dispatch-dashboard.model';

@Injectable({ providedIn: 'root' })
export class DispatchApiService {
  private api = inject(ApiService);

  getKpis(): Observable<DispatchDashboardKpis> {
    return this.api.get<DispatchDashboardKpis>('dispatch/dashboard/kpis');
  }

  getZoneMetrics(zoneId: number): Observable<DispatchZoneMetrics> {
    return this.api.get<DispatchZoneMetrics>(`dispatch/zones/${zoneId}/metrics`);
  }

  getCourierPositions(): Observable<DispatchCourierPosition[]> {
    return this.api.get<DispatchCourierPosition[]>('dispatch/couriers/positions');
  }

  getPendingOrders(): Observable<DispatchPendingOrder[]> {
    return this.api.get<DispatchPendingOrder[]>('dispatch/orders/pending');
  }

  manualAssign(payload: ManualAssignPayload): Observable<unknown> {
    return this.api.post('dispatch/manual-assign', payload);
  }

  manualBundle(payload: ManualBundlePayload): Observable<unknown> {
    return this.api.post('dispatch/manual-bundle', payload);
  }

  updateZoneStatus(zoneId: number, active: boolean): Observable<unknown> {
    return this.api.put(`dispatch/zones/${zoneId}/status`, { active });
  }
}
