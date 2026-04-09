// src/app/features/orders/components/order-card/order-card.component.ts - Angular 19
import { Component, input, output, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderStatusBadgeComponent } from '../order-status-badge/order-status-badge.component';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';
import { TranslateModule } from '@ngx-translate/core';
import { Order } from '../../models/order.model';
import { TimerComponent } from '../prep-timer/timer.component';
import { PrepTimerSessionService } from '../../services/prep-timer-session.service';
import { mergePrepTimerContext } from '../../utils/prep-timer.utils';

@Component({
  selector: 'app-order-card',
  standalone: true,
  host: {
    '[class.new-order]': 'isNew()',
    '[class.prep-timer-overdue]': 'prepTimerOverdue()',
  },
  imports: [
    CommonModule,
    RouterLink,
    TranslateModule,
    OrderStatusBadgeComponent,
    TimeAgoPipe,
    TimerComponent,
  ],
  templateUrl: './order-card.component.html',
  styleUrls: ['./order-card.component.scss'],
})
export class OrderCardComponent {
  private prepSession = inject(PrepTimerSessionService);

  // Angular 19 Signal Inputs
  order = input.required<any>();
  /** True for ~2s after the order first arrives via WebSocket */
  isNew = input<boolean>(false);

  // Angular 19 Outputs
  confirm = output<string>();
  reject = output<string>();
  startPreparing = output<string>();
  markReady = output<string>();
  /** Échéance prépa dépassée (minuteur à 0) — pour la liste mobile. */
  prepTimerExpired = output<boolean>();

  prepTimerOverdue = signal(false);

  prepTimerContext = computed(() => {
    const o = this.order() as Order;
    const s = this.prepSession.load(o.id);
    return mergePrepTimerContext(s, o);
  });

  // Computed — compare uppercase to match backend OrderStatus enum
  isPending = computed(() => this.order()?.status === 'PENDING');
  isConfirmed = computed(() => this.order()?.status === 'CONFIRMED');
  isPreparing = computed(() => this.order()?.status === 'PREPARING');
  isReady = computed(() => this.order()?.status === 'READY');
  isCancelled = computed(() => this.order()?.status === 'CANCELLED');

  totalItems = computed(() => {
    const o = this.order();
    return o?.items?.reduce((sum: number, item: any) => sum + item.quantity, 0) ?? 0;
  });

  onConfirm(): void {
    this.confirm.emit(this.order().id);
  }

  onReject(): void {
    this.reject.emit(this.order().id);
  }

  onStartPreparing(): void {
    this.startPreparing.emit(this.order().id);
  }

  onMarkReady(): void {
    this.markReady.emit(this.order().id);
  }

  onPrepTimerExpiredChange(expired: boolean): void {
    this.prepTimerOverdue.set(expired);
    this.prepTimerExpired.emit(expired);
  }
}
