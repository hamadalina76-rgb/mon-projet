// src/app/features/support/services/support.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class SupportService {
  private api = inject(ApiService);

  getTickets(page: number, pageSize: number, status?: string): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    if (status) {
      params = params.set('status', status);
    }
    return this.api.get('admin/support/tickets', params);
  }

  getTicket(id: string): Observable<any> {
    return this.api.get(`admin/support/tickets/${id}`);
  }

  assignTicket(id: string, agentId: string): Observable<any> {
    return this.api.post(`admin/support/tickets/${id}/assign`, { agentId });
  }

  resolveTicket(id: string, resolution: string): Observable<any> {
    return this.api.post(`admin/support/tickets/${id}/resolve`, { resolution });
  }

  addResponse(id: string, message: string): Observable<any> {
    return this.api.post(`admin/support/tickets/${id}/responses`, { message });
  }

  getFAQs(): Observable<any> {
    return this.api.get('admin/support/faqs');
  }

  createFAQ(data: any): Observable<any> {
    return this.api.post('admin/support/faqs', data);
  }

  updateFAQ(id: string, data: any): Observable<any> {
    return this.api.put(`admin/support/faqs/${id}`, data);
  }
}
