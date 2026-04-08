// src/app/features/orders/components/order-status-badge/order-status-badge.component.ts - Angular 19
import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-order-status-badge',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './order-status-badge.component.html',
  styleUrls: ['./order-status-badge.component.scss'],
})
export class OrderStatusBadgeComponent {
  status = input('');

  /** Clé i18n : ORDERS.STATUS.{enum} */
  statusTranslateKey = computed(() => {
    const s = (this.status() || 'PENDING').toUpperCase();
    return `ORDERS.STATUS.${s}`;
  });
}
