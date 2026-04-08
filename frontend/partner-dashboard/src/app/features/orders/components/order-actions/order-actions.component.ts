// src/app/features/orders/components/order-actions/order-actions.component.ts - Angular 19
import { Component, input, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-order-actions',
  standalone: true,
  imports: [CommonModule, TranslateModule, MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './order-actions.component.html',
  styleUrls: ['./order-actions.component.scss'],
})
export class OrderActionsComponent {
  // Angular 19 Signal Input
  status = input('');

  // Angular 19 Signal Outputs
  confirm = output<void>();
  startPreparing = output<void>();
  markReady = output<void>();
  cancel = output<void>();
  print = output<void>();

  // Computed
  isPending = computed(() => this.status() === 'PENDING');
  isConfirmed = computed(() => this.status() === 'CONFIRMED');
  isPreparing = computed(() => this.status() === 'PREPARING');
}
