// src/app/core/services/websocket.service.ts
import { Injectable } from '@angular/core';
import { Socket } from 'ngx-socket-io';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  constructor(
    private socket: Socket,
    private authService: AuthService
  ) {}

  connect(): void {
    const token = this.authService.getToken();
    const partnerId = this.authService.getPartnerId();

    if (token && partnerId) {
      this.socket.ioSocket.auth = { token };
      this.socket.connect();
      this.socket.emit('join_partner_room', { partnerId });
    }
  }

  disconnect(): void {
    this.socket.disconnect();
  }

  // Orders Events
  onNewOrder(): Observable<unknown> {
    return this.socket.fromEvent('new_order');
  }

  onOrderStatusChanged(): Observable<unknown> {
    return this.socket.fromEvent('order_status_changed');
  }

  onOrderCancelled(): Observable<unknown> {
    return this.socket.fromEvent('order_cancelled');
  }

  // Emit Events
  confirmOrder(orderId: string): void {
    this.socket.emit('confirm_order', { orderId });
  }

  updateOrderStatus(orderId: string, status: string): void {
    this.socket.emit('update_order_status', { orderId, status });
  }

  markOrderReady(orderId: string): void {
    this.socket.emit('order_ready', { orderId });
  }

  // Connection Events
  onConnect(): Observable<unknown> {
    return this.socket.fromEvent('connect');
  }

  onDisconnect(): Observable<unknown> {
    return this.socket.fromEvent('disconnect');
  }

  onError(): Observable<unknown> {
    return this.socket.fromEvent('error');
  }
}
