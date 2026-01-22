// src/app/features/payments/services/payments.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class PaymentsService {
  private api = inject(ApiService);

  getTransactions(page: number, pageSize: number): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    return this.api.get('admin/payments/transactions', params);
  }

  getPartnerPayouts(): Observable<any> {
    return this.api.get('admin/payments/partner-payouts');
  }

  getCourierPayouts(): Observable<any> {
    return this.api.get('admin/payments/courier-payouts');
  }

  processPartnerPayout(partnerId: string): Observable<any> {
    return this.api.post(`admin/payments/partners/${partnerId}/payout`, {});
  }

  processCourierPayout(courierId: string): Observable<any> {
    return this.api.post(`admin/payments/couriers/${courierId}/payout`, {});
  }

  getRefunds(): Observable<any> {
    return this.api.get('admin/payments/refunds');
  }

  processRefund(transactionId: string, amount: number): Observable<any> {
    return this.api.post(`admin/payments/${transactionId}/refund`, { amount });
  }
}
