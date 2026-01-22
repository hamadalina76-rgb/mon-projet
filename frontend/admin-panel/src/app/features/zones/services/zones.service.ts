// src/app/features/zones/services/zones.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class ZonesService {
  private api = inject(ApiService);

  getZones(): Observable<any> {
    return this.api.get('admin/zones');
  }

  getZone(id: string): Observable<any> {
    return this.api.get(`admin/zones/${id}`);
  }

  createZone(data: any): Observable<any> {
    return this.api.post('admin/zones', data);
  }

  updateZone(id: string, data: any): Observable<any> {
    return this.api.put(`admin/zones/${id}`, data);
  }

  deleteZone(id: string): Observable<any> {
    return this.api.delete(`admin/zones/${id}`);
  }

  getPricing(zoneId: string): Observable<any> {
    return this.api.get(`admin/zones/${zoneId}/pricing`);
  }

  updatePricing(zoneId: string, data: any): Observable<any> {
    return this.api.put(`admin/zones/${zoneId}/pricing`, data);
  }
}
