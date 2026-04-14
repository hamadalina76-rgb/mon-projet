// src/app/core/services/websocket.service.ts
import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export type ConnectionStatus = 'connected' | 'connecting' | 'disconnected';

export interface PartnerNotification {
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

@Injectable({
  providedIn: 'root',
})
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private partnerId: number | null = null;

  private connectionStatus$ = new BehaviorSubject<ConnectionStatus>('disconnected');
  private partnerNotification$ = new Subject<PartnerNotification>();
  private orderNotification$ = new Subject<any>();

  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_DELAY_MS = 30_000;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  /** Timestamp of the last disconnection — used to fetch missed orders on reconnect */
  private lastDisconnectAt: Date | null = null;

  get onPartnerNotification(): Observable<PartnerNotification> {
    return this.partnerNotification$.asObservable();
  }

  get onOrderNotification(): Observable<any> {
    return this.orderNotification$.asObservable();
  }

  /** @deprecated Use getConnectionStatus() for reactive status */
  get isConnected() {
    return this.connectionStatus$.value === 'connected';
  }

  constructor(private authService: AuthService) {}

  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatus$.asObservable();
  }

  getLastDisconnectAt(): Date | null {
    return this.lastDisconnectAt;
  }

  connect(): void {
    if (this.client?.active) return;

    this.partnerId = this.authService.getPartnerId();
    this.connectionStatus$.next('connecting');

    const wsUrl = environment.wsUrl || 'ws://localhost:8080';
    const sockJsUrl = wsUrl
      .replace('ws://', 'http://')
      .replace('wss://', 'https://') + '/ws/notifications';

    this.client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      // Fixed small reconnectDelay — we handle exponential backoff manually via onDisconnect
      reconnectDelay: 0,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      onConnect: () => {
        this.reconnectAttempts = 0;
        this.clearReconnectTimer();
        this.connectionStatus$.next('connected');
        console.log('[WebSocket] Connected');
        this.subscribeToPartnerNotifications();
      },
      onDisconnect: () => {
        this.lastDisconnectAt = new Date();
        this.connectionStatus$.next('disconnected');
        console.log('[WebSocket] Disconnected');
        this.scheduleReconnect();
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
        this.connectionStatus$.next('disconnected');
        this.scheduleReconnect();
      },
    });

    this.client.activate();
  }

  disconnect(): void {
    this.clearReconnectTimer();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.connectionStatus$.next('disconnected');
  }

  /** Called on legacy onNewOrder() subscriptions */
  onNewOrder(): Observable<unknown> {
    return this.orderNotification$.asObservable();
  }

  // ---------------------------------------------------------------------------
  // Private
  // ---------------------------------------------------------------------------

  private subscribeToPartnerNotifications(): void {
    if (!this.client?.active) return;

    const partnerId = this.partnerId || this.authService.getPartnerId();
    if (!partnerId) {
      console.warn('[WebSocket] No partnerId — skipping partner topic subscription');
      return;
    }

    // Partner-scoped topic: order events + admin actions
    this.client.subscribe(
      `/topic/partner/${partnerId}/notifications`,
      (message: IMessage) => {
        try {
          const notification: PartnerNotification = JSON.parse(message.body);
          console.log('[WebSocket] Partner notification received:', notification);
          this.partnerNotification$.next(notification);
          this.routeOrderNotification(notification);
        } catch (e) {
          console.error('[WebSocket] Failed to parse partner notification:', e);
        }
      }
    );

    // User-scoped topic (general notifications)
    const userId = this.authService.currentUser()?.id;
    if (userId) {
      this.client.subscribe(
        `/topic/user/${userId}/notifications`,
        (message: IMessage) => {
          try {
            const notification: PartnerNotification = JSON.parse(message.body);
            this.partnerNotification$.next(notification);
            this.routeOrderNotification(notification);
          } catch (e) {
            console.error('[WebSocket] Failed to parse user notification:', e);
          }
        }
      );
    }
  }

  /**
   * Routes ORDER-type notifications to the dedicated orderNotification$ stream
   * so consumers (OrdersListComponent, MainLayoutComponent) receive typed order data.
   */
  private routeOrderNotification(notification: PartnerNotification): void {
    const action = notification.data?.['action'];
    if (action === 'ORDER_SCHEDULED_PREP_REMINDER') {
      return;
    }
    if (notification.type === 'ORDER' || notification.type === 'ORDER_NEW') {
      const orderPayload = {
        ...(notification.data || {}),
        // Ensure top-level convenience fields for existing consumers
        orderNumber: notification.data?.['orderNumber'] ?? '',
        title: notification.title,
        message: notification.message,
      };
      this.orderNotification$.next(orderPayload);
    }
  }

  /**
   * Exponential backoff: delay = min(1s × 2^attempts, 30s)
   */
  private scheduleReconnect(): void {
    if (this.reconnectTimer) return;
    const delay = Math.min(1_000 * Math.pow(2, this.reconnectAttempts), this.MAX_RECONNECT_DELAY_MS);
    this.reconnectAttempts++;
    console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
    this.connectionStatus$.next('connecting');

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delay);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.partnerNotification$.complete();
    this.orderNotification$.complete();
    this.connectionStatus$.complete();
  }
}
