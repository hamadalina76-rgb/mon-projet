// src/app/features/finance/services/finance.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class FinanceService {
  private api = inject(ApiService);

  getEarningsSummary(period: string): Observable<any> {
    return this.api.get('partner/finance/earnings', { period });
  }

  getEarningsDetails(startDate: string, endDate: string): Observable<any> {
    return this.api.get('partner/finance/earnings/details', { startDate, endDate });
  }

  getPayouts(): Observable<any> {
    return this.api.get('partner/finance/payouts');
  }

  getPayoutDetails(id: string): Observable<any> {
    return this.api.get(`partner/finance/payouts/${id}`);
  }

  requestPayout(amount: number): Observable<any> {
    return this.api.post('partner/finance/payouts/request', { amount });
  }

  getBankAccount(): Observable<any> {
    return this.api.get('partner/finance/bank-account');
  }

  updateBankAccount(data: any): Observable<any> {
    return this.api.put('partner/finance/bank-account', data);
  }

  downloadInvoice(payoutId: string): Observable<Blob> {
    return this.api.get(`partner/finance/payouts/${payoutId}/invoice`);
  }

  exportEarnings(period: string): Observable<Blob> {
    return this.api.get(`partner/finance/earnings/export`, { period });
  }
}
