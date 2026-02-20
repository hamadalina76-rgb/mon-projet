// src/app/core/services/websocket.service.ts
import { Injectable, signal, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../environments/environment';
import { Subject } from 'rxjs';

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
