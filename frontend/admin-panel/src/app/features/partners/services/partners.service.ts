// src/app/features/partners/services/partners.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class PartnersService {
  private api = inject(ApiService);

  getPartners(page: number = 0, pageSize: number = 20, status?: string, search?: string): Observable<any> {
    let url = `admin/partners?page=${page}&size=${pageSize}`;
    if (status) {
      url += `&status=${status}`;
    }
    if (search?.trim()) {
      url += `&search=${encodeURIComponent(search.trim())}`;
    }
    return this.api.get(url);
  }

  getPartner(id: string): Observable<any> {
    return this.api.get(`admin/partners/${id}`);
  }

  getPendingPartners(page: number = 0, pageSize: number = 20): Observable<any> {
    return this.api.get(`admin/partners/pending?page=${page}&size=${pageSize}`);
  }

  getPartnerStats(): Observable<any> {
    return this.api.get('admin/partners/stats');
  }

  approvePartner(id: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/approve`, {});
  }

  approvePartnerWithCommission(id: string, commissionData: {
    commissionType: string;
    commissionRate: number;
    categoryId: number;
    subcategoryIds: number[];
  }): Observable<any> {
    return this.api.post(`admin/partners/${id}/approve`, commissionData);
  }
getPartnerChangeLogsFiltered(
  id: string,
  page: number = 0,
  size: number = 10,
  filters: {
    action?: string;
    adminId?: number;
    dateFrom?: string;
    dateTo?: string;
    changedField?: string;
  } = {}
): Observable<any> {
  const body: any = { page, size };
  if (filters.action)       body['action']       = filters.action;
  if (filters.adminId)      body['adminId']       = filters.adminId;
  if (filters.dateFrom)     body['dateFrom']      = filters.dateFrom;
  if (filters.dateTo)       body['dateTo']        = filters.dateTo;
  if (filters.changedField) body['changedField']  = filters.changedField;
  return this.api.post(`admin/partners/${id}/change-logs/filter`, body);
}
  rejectPartner(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/reject`, { reason });
  }

  suspendPartner(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/suspend`, { reason });
  }

  activatePartner(id: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/activate`, {});
  }

  deactivatePartner(id: string, reason: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/deactivate`, { reason });
  }

  verifyDocuments(id: string, documents: any): Observable<any> {
    return this.api.post(`admin/partners/${id}/verify-documents`, documents);
  }

  requestMoreInfo(id: string, message: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/request-more-info`, { message });
  }

  saveInternalNotes(id: string, note: string): Observable<any> {
    return this.api.post(`admin/partners/${id}/internal-notes`, { note });
  }

  getPartnerChangeLogs(id: string, page: number = 0, size: number = 10): Observable<any> {
    return this.api.get(`admin/partners/${id}/change-logs?page=${page}&size=${size}`);
  }

  updatePartner(id: string, data: {
    businessName?: string;
    brandName?: string;
    email?: string;
    phoneNumber?: string;
    type?: string;
    city?: string;
    address?: string;
    description?: string;
    commissionType?: string;
    commissionRate?: number;
    categoryId?: number | null;
    subcategoryIds?: number[];
  }): Observable<any> {
    return this.api.put(`admin/partners/${id}`, data);
  }
}
