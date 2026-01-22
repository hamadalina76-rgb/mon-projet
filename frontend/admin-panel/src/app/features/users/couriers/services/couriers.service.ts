// src/app/features/users/couriers/services/couriers.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class CouriersService {
  private api = inject(ApiService);

  getCouriers(page: number, pageSize: number): Observable<any> {
    return this.api.get(`admin/couriers?page=${page}&size=${pageSize}`);
  }

  getCourier(id: string): Observable<any> {
    return this.api.get(`admin/couriers/${id}`);
  }

  getPendingApprovals(): Observable<any> {
    return this.api.get('admin/couriers/pending');
  }

  approveCourier(id: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/approve`, {});
  }

  rejectCourier(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/reject`, { reason });
  }

  suspendCourier(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/suspend`, { reason });
  }

  activateCourier(id: string): Observable<any> {
    return this.api.post(`admin/couriers/${id}/activate`, {});
  }
}
