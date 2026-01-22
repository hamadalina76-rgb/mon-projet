// src/app/features/orders/orders-list/orders-list.component.ts
import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';
import { OrdersService } from '../services/orders.service';
import { WebSocketService } from '@core/services/websocket.service';
import { OrderCardComponent } from '../components/order-card/order-card.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatBadgeModule,
    TranslateModule,
    OrderCardComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './orders-list.component.html',
  styleUrls: ['./orders-list.component.scss'],
})
export class OrdersListComponent implements OnInit, OnDestroy {
  private ordersService = inject(OrdersService);
  private wsService = inject(WebSocketService);
  private destroy$ = new Subject<void>();

  // Angular 19 Signals
  orders = signal<any[]>([]);
  loading = signal(false);
  selectedStatus = signal('all');

  // Computed signals for reactive counts
  filteredOrders = computed(() => {
    const status = this.selectedStatus();
    const allOrders = this.orders();
    return status === 'all' ? allOrders : allOrders.filter(o => o.status === status);
  });

  statusTabs = computed(() => [
    { value: 'all', label: 'Toutes', count: this.orders().length },
    { value: 'PENDING', label: 'En attente', count: this.orders().filter(o => o.status === 'PENDING').length },
    { value: 'CONFIRMED', label: 'Confirmées', count: this.orders().filter(o => o.status === 'CONFIRMED').length },
    { value: 'PREPARING', label: 'En préparation', count: this.orders().filter(o => o.status === 'PREPARING').length },
    { value: 'READY', label: 'Prêtes', count: this.orders().filter(o => o.status === 'READY').length },
  ]);

  ngOnInit(): void {
    this.loadOrders();
    this.subscribeToNewOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.ordersService.getOrders()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.orders.set(data);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  subscribeToNewOrders(): void {
    this.wsService.onNewOrder()
      .pipe(takeUntil(this.destroy$))
      .subscribe((order) => {
        this.orders.update(orders => [order, ...orders]);
        this.playNotificationSound();
      });
  }

  confirmOrder(orderId: string): void {
    this.ordersService.confirmOrder(orderId).subscribe(() => {
      this.orders.update(orders => 
        orders.map(o => o.id === orderId ? { ...o, status: 'CONFIRMED' } : o)
      );
    });
  }

  startPreparing(orderId: string): void {
    this.ordersService.startPreparing(orderId).subscribe(() => {
      this.orders.update(orders => 
        orders.map(o => o.id === orderId ? { ...o, status: 'PREPARING' } : o)
      );
    });
  }

  markReady(orderId: string): void {
    this.ordersService.markReady(orderId).subscribe(() => {
      this.orders.update(orders => 
        orders.map(o => o.id === orderId ? { ...o, status: 'READY' } : o)
      );
    });
  }

  playNotificationSound(): void {
    const audio = new Audio('assets/sounds/notification.mp3');
    audio.play().catch(() => {});
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
