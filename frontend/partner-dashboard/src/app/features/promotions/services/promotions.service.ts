// src/app/features/promotions/services/promotions.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class PromotionsService {
  private api = inject(ApiService);

  getPromotions(): Observable<any> {
    return this.api.get('partner/promotions');
  }

  getPromotion(id: string): Observable<any> {
    return this.api.get(`partner/promotions/${id}`);
  }

  createPromotion(data: any): Observable<any> {
    return this.api.post('partner/promotions', data);
  }

  updatePromotion(id: string, data: any): Observable<any> {
    return this.api.put(`partner/promotions/${id}`, data);
  }

  deletePromotion(id: string): Observable<any> {
    return this.api.delete(`partner/promotions/${id}`);
  }

  togglePromotion(id: string): Observable<any> {
    return this.api.post(`partner/promotions/${id}/toggle`, {});
  }
}
