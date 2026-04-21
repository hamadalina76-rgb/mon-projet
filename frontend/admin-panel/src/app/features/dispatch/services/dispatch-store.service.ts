import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import {
  DispatchCourierPosition,
  DispatchDashboardKpis,
  DispatchPendingOrder,
  DispatchZoneMetrics,
} from '../models/dispatch-dashboard.model';

interface DispatchState {
  kpis: DispatchDashboardKpis | null;
  couriers: DispatchCourierPosition[];
  pendingOrders: DispatchPendingOrder[];
  selectedZoneMetrics: DispatchZoneMetrics | null;
}

const initialState: DispatchState = {
  kpis: null,
  couriers: [],
  pendingOrders: [],
  selectedZoneMetrics: null,
};

@Injectable({ providedIn: 'root' })
export class DispatchStoreService {
  private stateSubject = new BehaviorSubject<DispatchState>(initialState);
  readonly state$ = this.stateSubject.asObservable();

  get snapshot(): DispatchState {
    return this.stateSubject.value;
  }

  setKpis(kpis: DispatchDashboardKpis): void {
    this.patch({ kpis });
  }

  setCouriers(couriers: DispatchCourierPosition[]): void {
    this.patch({ couriers });
  }

  setPendingOrders(pendingOrders: DispatchPendingOrder[]): void {
    this.patch({ pendingOrders });
  }

  setZoneMetrics(selectedZoneMetrics: DispatchZoneMetrics): void {
    this.patch({ selectedZoneMetrics });
  }

  private patch(partial: Partial<DispatchState>): void {
    this.stateSubject.next({ ...this.stateSubject.value, ...partial });
  }
}
