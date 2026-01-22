// src/app/features/users/customers/services/customers.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class CustomersService {
  private api = inject(ApiService);

  getCustomers(page: number, pageSize: number): Observable<any> {
    return this.api.get(`admin/customers?page=${page}&size=${pageSize}`);
  }

  getCustomer(id: string): Observable<any> {
    return this.api.get(`admin/customers/${id}`);
  }

  blockCustomer(id: string): Observable<any> {
    return this.api.post(`admin/customers/${id}/block`, {});
  }

  unblockCustomer(id: string): Observable<any> {
    return this.api.post(`admin/customers/${id}/unblock`, {});
  }

  exportCustomers(): Observable<Blob> {
    return this.api.get('admin/customers/export');
  }
}
