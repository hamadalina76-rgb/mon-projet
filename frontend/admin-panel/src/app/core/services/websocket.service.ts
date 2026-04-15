// src/app/core/services/websocket.service.ts
import { Injectable, signal, OnDestroy } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { Subject, Observable } from 'rxjs';

export interface WebSocketNotification {
  id: string;
  userId: number;
  type: string;
  title: string;
  message: string;
  data: Record<string, any>;
  isRead: boolean;
  channel: string;
  createdAt: string;
  readAt: string | null;
}

export interface CourierPositionEvent {
  courierId: number;
  lat: number;
  lng: number;
  heading?: number;
  speed?: number;
  estimatedArrivalMin?: number;
  timestamp?: string;
}

export interface OrderNoteEvent {
  id: number;
  orderId: number;
  content: string;
  visibility: string;
  authorId: number;
  authorName: string;
  authorRole: string;
  createdAt: string;
}

export interface OrderTimelineEvent {
  orderId: number;
  status: string;
  previousStatus?: string;
  description?: string;
  actorType?: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root',
})
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private connected = signal(false);

  // Observable subjects for notifications
  private adminNotification$ = new Subject<WebSocketNotification>();

  get isConnected() {
    return this.connected.asReadonly();
  }

  get onAdminNotification() {
    return this.adminNotification$.asObservable();
  }

  connect(): void {
    if (this.client?.active) return;

    const wsUrl = environment.wsUrl || 'ws://localhost:8080';
    const sockJsUrl = wsUrl.replace('ws://', 'http://').replace('wss://', 'https://') + '/ws/notifications';

    this.client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.connected.set(true);
        console.log('[WebSocket] Connected to notification service');
        this.subscribeToAdminNotifications();
      },
      onDisconnect: () => {
        this.connected.set(false);
        console.log('[WebSocket] Disconnected');
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected.set(false);
    }
  }

  /**
   * Subscribe to courier GPS position updates for a specific order.
   * Returns an Observable that emits position events and a teardown function.
   */
  subscribeToCourierTracking(orderId: number): { positions$: Observable<CourierPositionEvent>; unsubscribe: () => void } {
    const subject = new Subject<CourierPositionEvent>();
    let subscription: StompSubscription | null = null;

    const doSubscribe = () => {
      if (!this.client?.active) return;
      subscription = this.client.subscribe(`/topic/tracking/${orderId}`, (message: IMessage) => {
        try {
          const pos: CourierPositionEvent = JSON.parse(message.body);
          subject.next(pos);
        } catch (e) {
          console.error('[WebSocket] Failed to parse courier position:', e);
        }
      });
    };

    if (this.client?.active) {
      doSubscribe();
    }

    return {
      positions$: subject.asObservable(),
      unsubscribe: () => {
        subscription?.unsubscribe();
        subject.complete();
      },
    };
  }

  /**
   * Subscribe to internal notes for a specific order.
   * Returns an Observable that emits new notes and a teardown function.
   */
  subscribeToOrderNotes(orderId: number): { notes$: Observable<OrderNoteEvent>; unsubscribe: () => void } {
    const subject = new Subject<OrderNoteEvent>();
    let subscription: StompSubscription | null = null;

    const doSubscribe = () => {
      if (!this.client?.active) return;
      subscription = this.client.subscribe(`/topic/orders/${orderId}/notes`, (message: IMessage) => {
        try {
          const note: OrderNoteEvent = JSON.parse(message.body);
          subject.next(note);
        } catch (e) {
          console.error('[WebSocket] Failed to parse order note:', e);
        }
      });
    };

    if (this.client?.active) {
      doSubscribe();
    }

    return {
      notes$: subject.asObservable(),
      unsubscribe: () => {
        subscription?.unsubscribe();
        subject.complete();
      },
    };
  }

  /**
   * Subscribe to order timeline (status changes) for a specific order.
   * Returns an Observable that emits timeline events and a teardown function.
   */
  subscribeToOrderTimeline(orderId: number): { timeline$: Observable<OrderTimelineEvent>; unsubscribe: () => void } {
    const subject = new Subject<OrderTimelineEvent>();
    let subscription: StompSubscription | null = null;

    const doSubscribe = () => {
      if (!this.client?.active) return;
      subscription = this.client.subscribe(`/topic/orders/${orderId}/timeline`, (message: IMessage) => {
        try {
          const event: OrderTimelineEvent = JSON.parse(message.body);
          subject.next(event);
        } catch (e) {
          console.error('[WebSocket] Failed to parse timeline event:', e);
        }
      });
    };

    if (this.client?.active) {
      doSubscribe();
    }

    return {
      timeline$: subject.asObservable(),
      unsubscribe: () => {
        subscription?.unsubscribe();
        subject.complete();
      },
    };
  }

  private subscribeToAdminNotifications(): void {
    if (!this.client?.active) return;

    this.client.subscribe('/topic/admin/notifications', (message: IMessage) => {
      try {
        const notification: WebSocketNotification = JSON.parse(message.body);
        console.log('[WebSocket] Admin notification received:', notification);
        this.adminNotification$.next(notification);
      } catch (e) {
        console.error('[WebSocket] Failed to parse notification:', e);
      }
    });
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.adminNotification$.complete();
  }
}
