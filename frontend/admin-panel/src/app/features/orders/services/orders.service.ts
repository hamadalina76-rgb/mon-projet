// src/app/features/orders/services/orders.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class OrdersService {
  private api = inject(ApiService);

  getOrders(page: number, pageSize: number, filters?: any): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    if (filters) {
      Object.keys(filters).forEach(key => {
        if (filters[key] !== undefined && filters[key] !== null) {
          params = params.set(key, filters[key].toString());
        }
      });
    }
    return this.api.get('admin/orders', params);
  }

  getOrder(id: string): Observable<any> {
    return this.api.get(`admin/orders/${id}`);
  }

  getLiveOrders(): Observable<any> {
    return this.api.get('admin/orders/live');
  }

  getDisputes(): Observable<any> {
    return this.api.get('admin/orders/disputes');
  }

  resolveDispute(orderId: string, resolution: any): Observable<any> {
    return this.api.post(`admin/orders/${orderId}/resolve`, resolution);
  }

  cancelOrder(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/orders/${id}/cancel`, { reason });
  }

  refundOrder(id: string, amount: number): Observable<any> {
    return this.api.post(`admin/orders/${id}/refund`, { amount });
  }
}
