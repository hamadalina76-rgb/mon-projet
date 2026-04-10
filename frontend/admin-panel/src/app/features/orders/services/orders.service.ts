// src/app/features/orders/services/orders.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';
import { AdminOrder, OrderFilters, OrderPageResponse } from '../models/admin-order.model';

@Injectable({
  providedIn: 'root',
})
export class OrdersService {
  private api = inject(ApiService);

  getOrders(page: number, pageSize: number, filters?: OrderFilters, sort?: string): Observable<OrderPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    if (sort) params = params.set('sort', sort);
    if (filters) {
      Object.entries(filters).forEach(([key, val]) => {
        if (val !== undefined && val !== null && val !== '') {
          params = params.set(key, val.toString());
        }
      });
    }
    return this.api.get<OrderPageResponse>('orders/admin', params);
  }

  getOrder(id: number): Observable<AdminOrder> {
    return this.api.get<AdminOrder>(`orders/${id}`);
  }

  getDisputes(): Observable<any> {
    return this.api.get('orders/disputes');
  }

  resolveDispute(orderId: string, resolution: any): Observable<any> {
    return this.api.post(`orders/${orderId}/resolve`, resolution);
  }

  cancelOrder(id: string, reason: string): Observable<any> {
    return this.api.post(`orders/${id}/cancel`, { reason });
  }

  refundOrder(id: string, amount: number): Observable<any> {
    return this.api.post(`orders/${id}/refund`, { amount });
  }
}
