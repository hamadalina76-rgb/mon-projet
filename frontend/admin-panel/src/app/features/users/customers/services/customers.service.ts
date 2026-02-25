import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { environment } from '@environments/environment';

export interface CustomerListParams {
  page?: number;
  size?: number;
  sort?: string;
  sortDir?: string;
  status?: string;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface CustomerListResponse {
  content: Customer[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Customer {
  id: number;
  userId: number;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  profilePicture?: string;
  status?: string;
  totalOrders?: number;
  totalSpent?: number;
  lastOrderDate?: string;
  createdAt?: string;
  updatedAt?: string;
  addresses?: unknown[];
  defaultAddress?: { city?: string; formattedAddress?: string };
}

export interface NewCustomersByMonth {
  [monthKey: string]: number;
}

@Injectable({
  providedIn: 'root',
})
export class CustomersService {
  private api = inject(ApiService);
  private http = inject(HttpClient);
  private baseUrl = environment.apiBaseUrl;

  getCustomers(
    page: number,
    pageSize: number,
    params?: Partial<CustomerListParams>
  ): Observable<CustomerListResponse> {
    let p = new HttpParams()
      .set('page', String(page))
      .set('size', String(pageSize));
    if (params?.sort) p = p.set('sort', params.sort);
    if (params?.sortDir) p = p.set('sortDir', params.sortDir);
    if (params?.status) p = p.set('status', params.status);
    if (params?.search) p = p.set('search', params.search);
    if (params?.dateFrom) p = p.set('dateFrom', params.dateFrom);
    if (params?.dateTo) p = p.set('dateTo', params.dateTo);
    return this.api.get<CustomerListResponse>(`admin/customers`, p);
  }

  getCustomer(id: string): Observable<Customer> {
    return this.api.get<Customer>(`admin/customers/${id}`);
  }

  blockCustomer(id: string): Observable<void> {
    return this.api.post<void>(`admin/customers/${id}/block`, {});
  }

  unblockCustomer(id: string): Observable<void> {
    return this.api.post<void>(`admin/customers/${id}/unblock`, {});
  }

  deleteCustomer(id: string): Observable<void> {
    return this.api.delete<void>(`admin/customers/${id}`);
  }

  resetPassword(id: string): Observable<void> {
    return this.api.post<void>(`admin/customers/${id}/reset-password`, {});
  }

  sendNotification(id: string, subject: string, body: string): Observable<void> {
    return this.api.post<void>(`admin/customers/${id}/send-notification`, { subject, body });
  }

  exportCustomers(format: 'csv' | 'xlsx', params?: Partial<CustomerListParams>): Observable<Blob> {
    let p = new HttpParams().set('format', format);
    if (params?.status) p = p.set('status', params.status);
    if (params?.search) p = p.set('search', params.search);
    if (params?.dateFrom) p = p.set('dateFrom', params.dateFrom);
    if (params?.dateTo) p = p.set('dateTo', params.dateTo);
    return this.api.get<Blob>('admin/customers/export', p, { responseType: 'blob' });
  }

  getNewCustomersByMonth(year?: number): Observable<NewCustomersByMonth> {
    const y = year ?? new Date().getFullYear();
    return this.api.get<NewCustomersByMonth>(`admin/customers/stats/new-by-month?year=${y}`);
  }

  /** Orders du client (order-service: GET /api/orders/customers/{id}/orders) */
  getCustomerOrders(customerId: number, page = 0, size = 10): Observable<CustomerOrdersResponse> {
    return this.http.get<CustomerOrdersResponse>(
      `${this.baseUrl}/orders/customers/${customerId}/orders?page=${page}&size=${size}`
    );
  }

  /** Avis du client (review-service: GET /api/reviews/customer/{id}) */
  getCustomerReviews(customerId: number, page = 0, size = 10): Observable<CustomerReviewsResponse> {
    return this.http.get<CustomerReviewsResponse>(
      `${this.baseUrl}/reviews/customer/${customerId}?page=${page}&size=${size}`
    );
  }
}

// Orders by customer (order-service path is /api/orders/...)
export interface CustomerOrdersResponse {
  content: unknown[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Reviews by customer (review-service path is /api/reviews/...)
export interface CustomerReviewsResponse {
  content: unknown[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
