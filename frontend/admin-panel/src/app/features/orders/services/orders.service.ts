// src/app/features/orders/services/orders.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';
import { environment } from '@environments/environment';
import { AdminOrder, CourierPosition, InternalNote, OrderFilters, OrderPageResponse, OrderStatus } from '../models/admin-order.model';

@Injectable({
  providedIn: 'root',
})
export class OrdersService {
  private api = inject(ApiService);
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

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

  getCourierPosition(courierId: number): Observable<CourierPosition> {
    return this.api.get<CourierPosition>(`tracking/couriers/${courierId}`);
  }

  getDisputes(): Observable<any> {
    return this.api.get('orders/disputes');
  }

  resolveDispute(orderId: string, resolution: any): Observable<any> {
    return this.api.post(`orders/${orderId}/resolve`, resolution);
  }

  cancelOrder(id: string, reason: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/orders/${id}`, {
      body: { cancelledBy: 'ADMIN', reason },
    });
  }

  refundOrder(id: string, amount: number): Observable<any> {
    return this.api.post(`orders/${id}/refund`, { amount });
  }

  forceStatus(id: string, status: OrderStatus, notes?: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/orders/${id}/status`, {
      status,
      actorType: 'ADMIN',
      actorId: 1,
      notes: notes || '',
    });
  }

  assignCourier(id: string, courierId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/orders/${id}/courier/assign`, { courierId });
  }

  sendNotification(userId: number, title: string, message: string, data?: Record<string, any>): Observable<any> {
    return this.http.post(`${this.apiUrl}/notifications/send`, {
      userId,
      type: 'ORDER',
      title,
      message,
      data: data || {},
      channel: 'IN_APP',
    });
  }

  // ── Internal Notes ────────────────────────────────
  getNotes(orderId: number): Observable<InternalNote[]> {
    return this.http.get<InternalNote[]>(`${this.apiUrl}/orders/${orderId}/notes`);
  }

  addNote(orderId: number, content: string, visibility: string = 'ADMIN_ONLY'): Observable<InternalNote> {
    return this.http.post<InternalNote>(`${this.apiUrl}/orders/${orderId}/notes`, { content, visibility });
  }

  deleteNote(orderId: number, noteId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/orders/${orderId}/notes/${noteId}`);
  }
}
