import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject } from 'rxjs';
import { environment } from '@environments/environment';
import {
  DispatchCourierPosition,
  DispatchCycleEvent,
  DispatchProposal,
  DispatchProposalResolvedEvent,
} from '../models/dispatch-dashboard.model';

@Injectable({ providedIn: 'root' })
export class DispatchRealtimeService implements OnDestroy {
  private client: Client | null = null;
  private stompConnected = false;
  private positionSub: StompSubscription | null = null;
  private zoneSubs = new Map<number, StompSubscription>();
  private pendingZoneSubscriptions = new Set<number>();
  private proposalSub: StompSubscription | null = null;
  private proposalResolvedSub: StompSubscription | null = null;

  private courierPositionsSubject = new Subject<DispatchCourierPosition[]>();
  private zoneCycleSubject = new Subject<DispatchCycleEvent>();
  private proposalSubject = new Subject<DispatchProposal>();
  private proposalResolvedSubject = new Subject<DispatchProposalResolvedEvent>();

  courierPositions$ = this.courierPositionsSubject.asObservable();
  zoneCycle$ = this.zoneCycleSubject.asObservable();
  proposalEvents$ = this.proposalSubject.asObservable();
  proposalResolved$ = this.proposalResolvedSubject.asObservable();

  connect(): void {
    if (this.client?.active) return;
    const wsUrl = environment.wsUrl || 'ws://localhost:8080';
    const sockJsUrl = wsUrl.replace('ws://', 'http://').replace('wss://', 'https://') + '/ws/dispatch';

    this.client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.stompConnected = true;
        this.positionSub = this.client?.subscribe('/topic/couriers/positions', (message: IMessage) => {
          try {
            this.courierPositionsSubject.next(JSON.parse(message.body));
          } catch {
            // no-op
          }
        }) ?? null;
        this.proposalSub = this.client?.subscribe('/topic/dispatch/proposals', (message: IMessage) => {
          try {
            this.proposalSubject.next(JSON.parse(message.body));
          } catch {
            // no-op
          }
        }) ?? null;
        this.proposalResolvedSub = this.client?.subscribe('/topic/dispatch/proposals/resolved', (message: IMessage) => {
          try {
            this.proposalResolvedSubject.next(JSON.parse(message.body));
          } catch {
            // no-op
          }
        }) ?? null;
        // Subscribe queued zones once STOMP is really connected.
        [...this.pendingZoneSubscriptions].forEach((zoneId) => this.subscribeZoneCycle(zoneId));
      },
      onDisconnect: () => {
        this.stompConnected = false;
      },
    });
    this.client.activate();
  }

  subscribeZoneCycle(zoneId: number): void {
    if (this.zoneSubs.has(zoneId)) return;
    // `client.active` can be true before STOMP CONNECT frame is completed.
    if (!this.client?.active || !this.stompConnected) {
      this.pendingZoneSubscriptions.add(zoneId);
      return;
    }
    const sub = this.client.subscribe(`/topic/zone/${zoneId}/cycle`, (message: IMessage) => {
      try {
        this.zoneCycleSubject.next(JSON.parse(message.body));
      } catch {
        // no-op
      }
    });
    this.zoneSubs.set(zoneId, sub);
    this.pendingZoneSubscriptions.delete(zoneId);
  }

  disconnect(): void {
    this.positionSub?.unsubscribe();
    this.positionSub = null;
    this.proposalSub?.unsubscribe();
    this.proposalSub = null;
    this.proposalResolvedSub?.unsubscribe();
    this.proposalResolvedSub = null;
    this.zoneSubs.forEach((s) => s.unsubscribe());
    this.zoneSubs.clear();
    this.pendingZoneSubscriptions.clear();
    this.stompConnected = false;
    this.client?.deactivate();
    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.courierPositionsSubject.complete();
    this.zoneCycleSubject.complete();
    this.proposalSubject.complete();
    this.proposalResolvedSubject.complete();
  }
}
