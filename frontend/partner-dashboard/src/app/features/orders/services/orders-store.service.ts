// src/app/features/orders/services/orders-store.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Order, OrderStatus } from '../models/order.model';

/**
 * Reactive in-memory store for the partner orders list.
 * Single source of truth — both the list component and the detail component
 * can read from / write to this store without extra HTTP calls.
 */
@Injectable({ providedIn: 'root' })
export class OrdersStoreService {
  private _orders = new BehaviorSubject<Order[]>([]);
  readonly orders$ = this._orders.asObservable();

  // ---------------------------------------------------------------------------
  // Write
  // ---------------------------------------------------------------------------

  setOrders(orders: Order[]): void {
    this._orders.next(orders);
  }

  /** Prepend a new order (WebSocket push). Ignores duplicates. */
  addOrder(order: Order): void {
    const current = this._orders.getValue();
    if (current.some(o => o.id === order.id)) return;
    this._orders.next([order, ...current]);
  }

  /** Optimistic / confirmed update of a single order. */
  updateOrder(patch: { id: string } & Partial<Order>): void {
    this._orders.next(
      this._orders.getValue().map(o => o.id === patch.id ? { ...o, ...patch } : o)
    );
  }

  /** Rollback an optimistic update to the previous status. */
  rollbackStatus(id: string, previousStatus: OrderStatus): void {
    this.updateOrder({ id, status: previousStatus });
  }

  // ---------------------------------------------------------------------------
  // Read
  // ---------------------------------------------------------------------------

  getOrders(): Order[] {
    return this._orders.getValue();
  }

  getOrder(id: string): Order | undefined {
    return this._orders.getValue().find(o => o.id === id);
  }

  // ---------------------------------------------------------------------------
  // Computed counts
  // ---------------------------------------------------------------------------

  getCounts(): Record<string, number> {
    const orders = this._orders.getValue();
    return {
      all: orders.length,
      PENDING: orders.filter(o => o.status === 'PENDING').length,
      CONFIRMED: orders.filter(o => o.status === 'CONFIRMED').length,
      PREPARING: orders.filter(o => o.status === 'PREPARING').length,
      READY: orders.filter(o => o.status === 'READY').length,
      CANCELLED: orders.filter(o => o.status === 'CANCELLED').length,
    };
  }
}
