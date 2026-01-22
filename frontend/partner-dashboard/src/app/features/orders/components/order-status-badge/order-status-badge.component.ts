// src/app/features/orders/components/order-status-badge/order-status-badge.component.ts - Angular 19
import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-order-status-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-status-badge.component.html',
  styleUrls: ['./order-status-badge.component.scss'],
})
export class OrderStatusBadgeComponent {
  // Angular 19 Signal Input
  status = input('');

  // Computed statusLabel
  statusLabel = computed(() => {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      CONFIRMED: 'Confirmée',
      PREPARING: 'En préparation',
      READY: 'Prête',
      PICKED_UP: 'Récupérée',
      CANCELLED: 'Annulée',
    };
    return labels[this.status()] || this.status();
  });
}
