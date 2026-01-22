// src/app/features/partners/services/partners.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class PartnersService {
  private api = inject(ApiService);

  getPartners(page: number, pageSize: number): Observable<any> {
    return this.api.get(`admin/partners?page=${page}&size=${pageSize}`);
  }

  getPartner(id: string): Observable<any> {
    return this.api.get(`admin/partners/${id}`);
  }

  getPendingPartners(): Observable<any> {
    return this.api.get('admin/partners/pending');
  }

  approvePartner(id: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/approve`, {});
  }

  rejectPartner(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/reject`, { reason });
  }

  suspendPartner(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/suspend`, { reason });
  }

  verifyDocuments(id: string, documents: any): Observable<any> {
    return this.api.post(`admin/partners/${id}/verify-documents`, documents);
  }
}
