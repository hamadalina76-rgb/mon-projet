import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { OrdersService } from '../services/orders.service';
import { ORDER_STATUS_CONFIG, OrderStatus } from '../models/admin-order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private ordersService = inject(OrdersService);

  order: any = null;
  loading = true;
  statusConfig = ORDER_STATUS_CONFIG;

  getStatusProp(prop: 'color' | 'bg' | 'icon'): string {
    const cfg = this.statusConfig[this.order?.status as OrderStatus];
    return cfg ? cfg[prop] : '';
  }

  /** Ordered pipeline steps */
  readonly pipeline = [
    'PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP', 'PICKED_UP', 'IN_DELIVERY', 'DELIVERED',
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadOrder(id);
  }

  loadOrder(id: string): void {
    this.loading = true;
    this.ordersService.getOrder(+id).subscribe({
      next: (order) => { this.order = order; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  /** Returns step index of current status in the pipeline (0-based). -1 if cancelled. */
  get currentStepIndex(): number {
    if (!this.order) return -1;
    if (this.order.status === 'CANCELLED') return -1;
    return this.pipeline.indexOf(this.order.status);
  }

  getStepState(i: number): 'done' | 'active' | 'upcoming' {
    const cur = this.currentStepIndex;
    if (cur < 0) return 'upcoming';
    if (i < cur) return 'done';
    if (i === cur) return 'active';
    return 'upcoming';
  }

  /** Timeline icon per action type */
  getTimelineIcon(entry: any): string {
    const map: Record<string, string> = {
      PENDING: 'schedule', CONFIRMED: 'thumb_up', PREPARING: 'restaurant',
      READY_FOR_PICKUP: 'inventory_2', PICKED_UP: 'local_shipping',
      IN_DELIVERY: 'delivery_dining', DELIVERED: 'check_circle', CANCELLED: 'cancel',
    };
    return map[entry.status] || 'fiber_manual_record';
  }

  getTimelineDotClass(entry: any): string {
    const map: Record<string, string> = {
      DELIVERED: 'dot-success', CANCELLED: 'dot-danger',
      PREPARING: 'dot-warning', READY_FOR_PICKUP: 'dot-warning',
      CONFIRMED: 'dot-info', PICKED_UP: 'dot-info', IN_DELIVERY: 'dot-info',
      PENDING: 'dot-default',
    };
    return map[entry.status] || 'dot-default';
  }
}
