// src/app/features/orders/order-detail/order-detail.component.ts - Angular 19
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { OrdersService } from '../services/orders.service';
import { OrderStatusBadgeComponent } from '../components/order-status-badge/order-status-badge.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    TranslateModule,
    OrderStatusBadgeComponent,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private ordersService = inject(OrdersService);
  private dialog = inject(MatDialog);

  // Angular 19 Signals
  order = signal<any>(null);
  loading = signal(false);

  ngOnInit(): void {
    const orderId = this.route.snapshot.paramMap.get('id');
    if (orderId) {
      this.loadOrder(orderId);
    }
  }

  loadOrder(id: string): void {
    this.loading.set(true);
    this.ordersService.getOrder(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  confirmOrder(): void {
    const o = this.order();
    if (!o) return;
    this.ordersService.confirmOrder(o.id).subscribe({
      next: (updated: any) => this.order.set(updated),
    });
  }

  startPreparing(): void {
    const o = this.order();
    if (!o) return;
    this.ordersService.startPreparing(o.id).subscribe({
      next: (updated: any) => this.order.set(updated),
    });
  }

  markReady(): void {
    const o = this.order();
    if (!o) return;
    this.ordersService.markReady(o.id).subscribe({
      next: (updated: any) => this.order.set(updated),
    });
  }

  cancelOrder(): void {
    const o = this.order();
    if (!o) return;
    this.ordersService.cancelOrder(o.id, 'Annulée par le partenaire').subscribe({
      next: (updated: any) => this.order.set(updated),
    });
  }
}
