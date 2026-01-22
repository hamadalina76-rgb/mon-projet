// src/app/features/orders/components/order-card/order-card.component.ts - Angular 19
import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { OrderStatusBadgeComponent } from '../order-status-badge/order-status-badge.component';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-order-card',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    OrderStatusBadgeComponent,
    TimeAgoPipe,
  ],
  templateUrl: './order-card.component.html',
  styleUrls: ['./order-card.component.scss'],
})
export class OrderCardComponent {
  // Angular 19 Signal Inputs
  order = input.required<any>();
  
  // Angular 19 Outputs
  confirm = output<string>();
  startPreparing = output<string>();
  markReady = output<string>();

  // Computed
  isPending = computed(() => this.order()?.status === 'pending');
  isConfirmed = computed(() => this.order()?.status === 'confirmed');
  isPreparing = computed(() => this.order()?.status === 'preparing');
  
  totalItems = computed(() => {
    const o = this.order();
    return o?.items?.reduce((sum: number, item: any) => sum + item.quantity, 0) ?? 0;
  });

  onConfirm(): void {
    this.confirm.emit(this.order().id);
  }

  onStartPreparing(): void {
    this.startPreparing.emit(this.order().id);
  }

  onMarkReady(): void {
    this.markReady.emit(this.order().id);
  }
}
