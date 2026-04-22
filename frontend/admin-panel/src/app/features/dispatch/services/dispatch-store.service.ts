import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import {
  DispatchCourierPosition,
  DispatchCycleEvent,
  DispatchDashboardKpis,
  DispatchPendingOrder,
  DispatchProposal,
  DispatchProposalResolvedEvent,
  DispatchZoneMetrics,
} from '../models/dispatch-dashboard.model';

const CYCLE_FEED_MAX = 20;

interface DispatchState {
  kpis: DispatchDashboardKpis | null;
  couriers: DispatchCourierPosition[];
  pendingOrders: DispatchPendingOrder[];
  selectedZoneMetrics: DispatchZoneMetrics | null;
  proposals: DispatchProposal[];
  /** Last {@link CYCLE_FEED_MAX} dispatch cycle events for the selected zone */
  zoneCycleFeed: DispatchCycleEvent[];
  proposalFeed: DispatchProposalResolvedEvent[];
}

const initialState: DispatchState = {
  kpis: null,
  couriers: [],
  pendingOrders: [],
  selectedZoneMetrics: null,
  proposals: [],
  zoneCycleFeed: [],
  proposalFeed: [],
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

  setProposals(proposals: DispatchProposal[]): void {
    this.patch({ proposals });
  }

  upsertProposal(p: DispatchProposal): void {
    const cur = this.snapshot.proposals.filter((x) => x.orderId !== p.orderId);
    this.patch({ proposals: [...cur, p] });
  }

  removeProposal(orderId: number): void {
    this.patch({ proposals: this.snapshot.proposals.filter((x) => x.orderId !== orderId) });
  }

  pushZoneCycleEvent(ev: DispatchCycleEvent): void {
    const next = [ev, ...this.snapshot.zoneCycleFeed].slice(0, CYCLE_FEED_MAX);
    this.patch({ zoneCycleFeed: next });
  }

  clearZoneCycleFeed(): void {
    this.patch({ zoneCycleFeed: [] });
  }

  pushProposalResolved(ev: DispatchProposalResolvedEvent): void {
    const next = [ev, ...this.snapshot.proposalFeed].slice(0, CYCLE_FEED_MAX);
    this.patch({ proposalFeed: next });
  }

  clearProposalFeed(): void {
    this.patch({ proposalFeed: [] });
  }

  private patch(partial: Partial<DispatchState>): void {
    this.stateSubject.next({ ...this.stateSubject.value, ...partial });
  }
}
