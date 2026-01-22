// src/app/features/promotions/services/promotions.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class PromotionsService {
  private api = inject(ApiService);

  getPromoCodes(page: number, pageSize: number): Observable<any> {
    return this.api.get('admin/promotions/codes', { page, size: pageSize });
  }

  createPromoCode(data: any): Observable<any> {
    return this.api.post('admin/promotions/codes', data);
  }

  updatePromoCode(id: string, data: any): Observable<any> {
    return this.api.put(`admin/promotions/codes/${id}`, data);
  }

  deletePromoCode(id: string): Observable<any> {
    return this.api.delete(`admin/promotions/codes/${id}`);
  }

  getCampaigns(): Observable<any> {
    return this.api.get('admin/promotions/campaigns');
  }

  createCampaign(data: any): Observable<any> {
    return this.api.post('admin/promotions/campaigns', data);
  }
}
