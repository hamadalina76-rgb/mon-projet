import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ApiService } from '@core/services/api.service';
import {
  Promotion,
  PromotionPageResponse,
  PromotionDetailResponse,
  PromotionStatistics,
  CreatePromotionRequest,
  UpdatePromotionRequest,
  ValidatePromotionRequest,
  ValidatePromotionResponse,
  ApplyPromotionRequest,
  RevokePromotionRequest,
  PromotionAnalytics,
} from '@core/models/promotion.model';

@Injectable({ providedIn: 'root' })
export class PromotionsService {
  private api = inject(ApiService);

  // ---------------------------------------------------------------- CRUD --

  getPromotions(params: {
    search?: string;
    status?: string;
    type?: string;
    startFrom?: string;
    startTo?: string;
    partnerId?: string;
    zoneId?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  } = {}): Observable<PromotionPageResponse> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));
    if (params.search)    httpParams = httpParams.set('search', params.search);
    if (params.status)    httpParams = httpParams.set('status', params.status);
    if (params.type)      httpParams = httpParams.set('type',   params.type);
    if (params.startFrom) httpParams = httpParams.set('startFrom', params.startFrom);
    if (params.startTo)   httpParams = httpParams.set('startTo', params.startTo);
    if (params.partnerId) httpParams = httpParams.set('partnerId', params.partnerId);
    if (params.zoneId)    httpParams = httpParams.set('zoneId', params.zoneId);
    if (params.sortBy)    httpParams = httpParams.set('sortBy', params.sortBy);
    if (params.sortDir)   httpParams = httpParams.set('sortDir', params.sortDir);
    return this.api.get<any>('promotions', httpParams).pipe(
      map((r) => r.data ?? r)
    );
  }

  getActivePromotions(): Observable<Promotion[]> {
    return this.api.get<any>('promotions/active').pipe(map((r) => r.data ?? r));
  }

  getById(id: number): Observable<PromotionDetailResponse> {
    return this.api.get<any>(`promotions/${id}`).pipe(map((r) => r.data ?? r));
  }

  /** Returns just the Promotion object (for form / edit). */
  getPromotionForEdit(id: number): Observable<Promotion> {
    return this.getById(id).pipe(map((r) => r.promotion));
  }

  getByCode(code: string): Observable<Promotion> {
    return this.api.get<any>(`promotions/code/${code}`).pipe(map((r) => r.data ?? r));
  }

  create(request: CreatePromotionRequest): Observable<Promotion> {
    return this.api.post<any>('promotions', request).pipe(map((r) => r.data ?? r));
  }

  update(id: number, request: UpdatePromotionRequest): Observable<Promotion> {
    return this.api.put<any>(`promotions/${id}`, request).pipe(map((r) => r.data ?? r));
  }

  delete(id: number): Observable<void> {
    return this.api.delete<any>(`promotions/${id}`);
  }

  toggle(id: number): Observable<void> {
    return this.api.patch<any>(`promotions/${id}/toggle`, {});
  }

  activate(id: number): Observable<void> {
    return this.api.patch<any>(`promotions/${id}/activate`, {});
  }

  deactivate(id: number): Observable<void> {
    return this.api.patch<any>(`promotions/${id}/deactivate`, {});
  }

  getStatistics(): Observable<PromotionStatistics> {
    return this.api.get<any>('promotions/statistics').pipe(map((r) => r.data ?? r));
  }

  // ------------------------------------------------ CODE CHECK ----

  /**
   * Returns true if the code is available, false if taken.
   * Uses a dedicated endpoint that always returns 200.
   */
  checkCodeAvailability(code: string): Observable<boolean> {
    return this.api.get<any>(`promotions/code/${encodeURIComponent(code)}/available`).pipe(
      map((r) => r.data ?? r),
      catchError(() => of(false)),
    );
  }

  // -------------------------------------------------- SIMULATE ----

  simulate(request: any): Observable<any> {
    return this.api.post<any>('promotions/simulate', request).pipe(map((r) => r.data ?? r));
  }

  // -------------------------------------------------- BUSINESS LOGIC ----

  validate(request: ValidatePromotionRequest): Observable<ValidatePromotionResponse> {
    return this.api.post<any>('promotions/validate', request).pipe(map((r) => r.data ?? r));
  }

  apply(code: string, request: ApplyPromotionRequest): Observable<ValidatePromotionResponse> {
    return this.api.post<any>(`promotions/${code}/apply`, request).pipe(map((r) => r.data ?? r));
  }

  revoke(code: string, request: RevokePromotionRequest): Observable<void> {
    return this.api.post<any>(`promotions/${code}/revoke`, request);
  }

  // ------------------------------------------------------------- STATS ---

  getAnalytics(id: number): Observable<PromotionAnalytics> {
    return this.api.get<any>(`promotions/${id}/analytics`).pipe(map((r) => r.data ?? r));
  }

  // ------------------------------------------------------------- CSV ---

  exportCsv(params: { search?: string; status?: string; type?: string } = {}): void {
    let httpParams = new HttpParams();
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.type)   httpParams = httpParams.set('type', params.type);

    this.api.get('promotions/export/csv', httpParams, { responseType: 'blob' } as any)
      .subscribe((blob: any) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'promotions.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      });
  }
}
