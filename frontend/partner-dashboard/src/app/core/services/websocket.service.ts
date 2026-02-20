// src/app/core/services/websocket.service.ts
import { Injectable, signal, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

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
  private connected = signal(false);

  private partnerNotification$ = new Subject<PartnerNotification>();
  private orderNotification$ = new Subject<any>();

  get isConnected() {
    return this.connected.asReadonly();
  }

  get onPartnerNotification(): Observable<PartnerNotification> {
    return this.partnerNotification$.asObservable();
  }

  get onOrderNotification(): Observable<any> {
    return this.orderNotification$.asObservable();
  }

  constructor(private authService: AuthService) {}

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
        this.subscribeToPartnerNotifications();
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

  private subscribeToPartnerNotifications(): void {
    if (!this.client?.active) return;

    const partnerId = this.authService.getPartnerId();
    if (!partnerId) {
      console.warn('[WebSocket] No partnerId, skipping partner topic subscription');
      return;
    }

    // Subscribe to partner-specific notifications (approval, rejection, etc.)
    this.client.subscribe(`/topic/partner/${partnerId}/notifications`, (message: IMessage) => {
      try {
        const notification: PartnerNotification = JSON.parse(message.body);
        console.log('[WebSocket] Partner notification received:', notification);
        this.partnerNotification$.next(notification);
      } catch (e) {
        console.error('[WebSocket] Failed to parse notification:', e);
      }
    });

    // Subscribe to user-specific notifications
    const userId = this.authService.currentUser()?.id;
    if (userId) {
      this.client.subscribe(`/topic/user/${userId}/notifications`, (message: IMessage) => {
        try {
          const notification: PartnerNotification = JSON.parse(message.body);
          this.partnerNotification$.next(notification);
        } catch (e) {
          console.error('[WebSocket] Failed to parse user notification:', e);
        }
      });
    }
  }

  // Legacy methods for order events compatibility
  onNewOrder(): Observable<unknown> {
    return this.orderNotification$.asObservable();
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.partnerNotification$.complete();
    this.orderNotification$.complete();
  }
}
