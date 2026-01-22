// src/app/features/orders/live-tracking/live-tracking.component.ts
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { TranslateModule } from '@ngx-translate/core';
import { OrdersService } from '../services/orders.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-live-tracking',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    TranslateModule,
  ],
  templateUrl: './live-tracking.component.html',
  styleUrls: ['./live-tracking.component.scss'],
})
export class LiveTrackingComponent implements OnInit, OnDestroy {
  private ordersService = inject(OrdersService);
  private subscription?: Subscription;

  liveOrders: any[] = [];
  selectedOrder: any = null;

  ngOnInit(): void {
    this.loadLiveOrders();
    this.subscribeToUpdates();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  loadLiveOrders(): void {
    // TODO: Implement
  }

  subscribeToUpdates(): void {
    // TODO: Implement WebSocket subscription
  }

  selectOrder(order: any): void {
    this.selectedOrder = order;
  }
}
