import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { OrderFilters } from '../models/admin-order.model';

export interface FiltersState extends OrderFilters {
  page: number;
  pageSize: number;
  sort: string;
}

const DEFAULT_STATE: FiltersState = {
  search: '',
  partnerId: null,
  courierId: null,
  status: null,
  startDate: undefined,
  endDate: undefined,
  amountMin: null,
  amountMax: null,
  paymentMethod: '',
  page: 1,
  pageSize: 20,
  sort: 'createdAt,desc',
};

@Injectable({ providedIn: 'root' })
export class FiltersStateService {
  private state$ = new BehaviorSubject<FiltersState>({ ...DEFAULT_STATE });

  get filters$(): Observable<FiltersState> {
    return this.state$.asObservable();
  }

  get snapshot(): FiltersState {
    return { ...this.state$.value };
  }

  patch(partial: Partial<FiltersState>): void {
    this.state$.next({ ...this.state$.value, ...partial });
  }

  reset(): void {
    this.state$.next({ ...DEFAULT_STATE });
  }

  toQueryParams(): Record<string, string> {
    const s = this.state$.value;
    const p: Record<string, string> = {};
    if (s.search) p['search'] = s.search;
    if (s.partnerId) p['partnerId'] = String(s.partnerId);
    if (s.courierId) p['courierId'] = String(s.courierId);
    if (s.status?.length) p['status'] = s.status.join(',');
    if (s.startDate) p['startDate'] = s.startDate;
    if (s.endDate) p['endDate'] = s.endDate;
    if (s.amountMin != null) p['amountMin'] = String(s.amountMin);
    if (s.amountMax != null) p['amountMax'] = String(s.amountMax);
    if (s.paymentMethod) p['paymentMethod'] = s.paymentMethod;
    return p;
  }

  fromQueryParams(params: Record<string, string>): void {
    const partial: Partial<FiltersState> = {};
    if (params['search']) partial.search = params['search'];
    if (params['partnerId']) partial.partnerId = +params['partnerId'];
    if (params['courierId']) partial.courierId = +params['courierId'];
    if (params['status']) partial.status = params['status'].split(',') as any;
    if (params['startDate']) partial.startDate = params['startDate'];
    if (params['endDate']) partial.endDate = params['endDate'];
    if (params['amountMin']) partial.amountMin = +params['amountMin'];
    if (params['amountMax']) partial.amountMax = +params['amountMax'];
    if (params['paymentMethod']) partial.paymentMethod = params['paymentMethod'] as any;
    if (Object.keys(partial).length) {
      this.state$.next({ ...DEFAULT_STATE, ...partial });
    }
  }
}
