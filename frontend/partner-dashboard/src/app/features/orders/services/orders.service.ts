// src/app/features/orders/services/orders.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class OrdersService {
  private api = inject(ApiService);

  getOrders(params?: any): Observable<any> {
    return this.api.get('partner/orders', params);
  }

  getOrder(id: string): Observable<any> {
    return this.api.get(`partner/orders/${id}`);
  }

  confirmOrder(id: string): Observable<any> {
    return this.api.post(`partner/orders/${id}/confirm`, {});
  }

  startPreparing(id: string): Observable<any> {
    return this.api.post(`partner/orders/${id}/prepare`, {});
  }

  markReady(id: string): Observable<any> {
    return this.api.post(`partner/orders/${id}/ready`, {});
  }

  cancelOrder(id: string, reason: string): Observable<any> {
    return this.api.post(`partner/orders/${id}/cancel`, { reason });
  }

  updatePrepTime(id: string, prepTime: number): Observable<any> {
    return this.api.put(`partner/orders/${id}/prep-time`, { prepTime });
  }
}
