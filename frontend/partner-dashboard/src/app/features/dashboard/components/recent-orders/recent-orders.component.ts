// src/app/features/dashboard/components/recent-orders/recent-orders.component.ts - Angular 19
import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-recent-orders',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, TimeAgoPipe],
  templateUrl: './recent-orders.component.html',
  styleUrls: ['./recent-orders.component.scss'],
})
export class RecentOrdersComponent {
  // Angular 19 Signal Input
  orders = input<any[]>([]);

  // Computed
  hasOrders = computed(() => this.orders().length > 0);
}
