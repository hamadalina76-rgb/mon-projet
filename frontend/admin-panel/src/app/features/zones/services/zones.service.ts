// src/app/features/zones/services/zones.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@environments/environment';
import { Zone, ZoneCreateRequest, ZoneUpdateRequest, PartnerInZone, ZoneType } from '../models/zone.model';
import { ZoneGeometry } from '../models/zone-geometry.model';

@Injectable({
  providedIn: 'root',
})
export class ZonesService {
  private http = inject(HttpClient);
  // Utilise directement /api/zones car l'API Gateway route /api/zones vers location-service
  private baseUrl = `${environment.apiUrl.replace('/v1', '')}/zones`;

  // CRUD Operations
  getZones(
    page: number = 0,
    size: number = 20,
    search?: string,
    isActive?: boolean | null
  ): Observable<{ content: Zone[]; totalElements: number }> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search != null && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (isActive != null) {
      params = params.set('isActive', String(isActive));
    }
    return this.http.get<{ content: Zone[]; totalElements: number }>(this.baseUrl, { params });
  }

  getActiveZones(): Observable<Zone[]> {
    return this.http.get<Zone[]>(`${this.baseUrl}/active`);
  }

  getZonesByType(type: ZoneType): Observable<Zone[]> {
    return this.http.get<Zone[]>(`${this.baseUrl}/type/${type}`);
  }

  getZone(id: number): Observable<Zone> {
    return this.http.get<Zone>(`${this.baseUrl}/${id}`);
  }

  createZone(data: ZoneCreateRequest): Observable<Zone> {
    return this.http.post<Zone>(this.baseUrl, data);
  }

  updateZone(id: number, data: ZoneUpdateRequest): Observable<Zone> {
    return this.http.put<Zone>(`${this.baseUrl}/${id}`, data);
  }

  deleteZone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  setActiveStatus(id: number, isActive: boolean): Observable<Zone> {
    return this.http.patch<Zone>(`${this.baseUrl}/${id}/status`, { isActive });
  }

  // Geometry Operations
  isPointInZone(zoneId: number, latitude: number, longitude: number): Observable<boolean> {
    const params = new HttpParams().set('latitude', latitude.toString()).set('longitude', longitude.toString());
    return this.http.get<{ contains: boolean }>(`${this.baseUrl}/${zoneId}/contains`, { params })
      .pipe(map((response) => response.contains));
  }

  findZoneForPoint(latitude: number, longitude: number): Observable<Zone | null> {
    const params = new HttpParams().set('latitude', latitude.toString()).set('longitude', longitude.toString());
    return this.http.get<Zone>(`${this.baseUrl}/find`, { params });
  }

  getDeliveryFeeForPoint(latitude: number, longitude: number): Observable<number | null> {
    const params = new HttpParams().set('latitude', latitude.toString()).set('longitude', longitude.toString());
    return this.http.get<{ deliveryFee: number }>(`${this.baseUrl}/delivery-fee`, { params })
      .pipe(map((response) => response.deliveryFee || null));
  }

  // Partners in Zone
  getPartnersInZone(zoneId: number): Observable<PartnerInZone[]> {
    return this.http.get<PartnerInZone[]>(`${this.baseUrl}/${zoneId}/partners`);
  }

  // Export/Import (backend gère la pagination)
  exportZones(): Observable<string> {
    return this.http.get(`${this.baseUrl}/export`, { responseType: 'text' });
  }

  importZones(geojson: string): Observable<{ created: number; failed: number }> {
    return this.http.post<{ created: number; failed: number }>(
      `${this.baseUrl}/import`,
      geojson,
      { headers: { 'Content-Type': 'application/json' } }
    );
  }

  // Validation
  validateBoundary(boundaryJson: string, excludeZoneId?: number): Observable<{ valid: boolean; overlaps?: any[] }> {
    return this.http.post<{ valid: boolean; overlaps?: any[] }>(`${this.baseUrl}/validate`, { boundaryJson, excludeZoneId });
  }
}
