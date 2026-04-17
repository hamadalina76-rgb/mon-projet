import { Injectable, inject, OnDestroy } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { environment } from '@environments/environment';
import { NotificationService } from './notification.service';
import { OrdersService } from '@features/orders/services/orders.service';
import type { Order } from '@features/orders/models/order.model';
import { formatScheduledSlot } from '@core/utils/format-scheduled-slot';

const STORAGE_PREFIX = 'sl_sched_reminder_';

const REMINDER_STATUSES = new Set(['PENDING', 'CONFIRMED', 'PREPARING']);

function offsetsMinutes(): number[] {
  const o = (environment as { scheduledReminderOffsetsMinutes?: number[] })
    .scheduledReminderOffsetsMinutes;
  if (Array.isArray(o) && o.length > 0) {
    return [...o].sort((a, b) => b - a);
  }
  return [60, 30, 15];
}

@Injectable({ providedIn: 'root' })
export class ScheduledOrderReminderService implements OnDestroy {
  private readonly translate = inject(TranslateService);
  private readonly notif = inject(NotificationService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly ordersService = inject(OrdersService);

  private readonly timeoutHandles = new Map<string, ReturnType<typeof setTimeout>>();
  private refreshSub: Subscription | null = null;

  ngOnDestroy(): void {
    this.clearAllTimeouts();
    this.refreshSub?.unsubscribe();
  }

  /** Annule les timers et recalcule à partir de la liste fournie. */
  syncFromOrders(orders: Order[]): void {
    this.clearAllTimeouts();
    const now = Date.now();
    const offs = offsetsMinutes();
    const lang = this.translate.currentLang || 'fr';

    for (const order of orders) {
      if (!order?.id || !order.isScheduled || !order.scheduledDeliveryTime) continue;
      if (!REMINDER_STATUSES.has(order.status)) continue;

      const slotEnd = new Date(order.scheduledDeliveryTime).getTime();
      if (Number.isNaN(slotEnd)) continue;

      for (const offsetMin of offs) {
        const fireAt = slotEnd - offsetMin * 60_000;
        if (fireAt <= now) continue;

        const storageKey = `${STORAGE_PREFIX}${order.id}_${offsetMin}`;
        try {
          if (sessionStorage.getItem(storageKey) === '1') continue;
        } catch {
          /* private mode */
        }

        const handleKey = `${order.id}_${offsetMin}`;
        const delay = fireAt - now;
        const id = order.id;
        const orderNumber = order.orderNumber ?? id;
        const scheduledIso = order.scheduledDeliveryTime;

        const h = setTimeout(() => {
          this.timeoutHandles.delete(handleKey);
          try {
            sessionStorage.setItem(storageKey, '1');
          } catch {
            /* ignore */
          }
          const slot = formatScheduledSlot(scheduledIso, lang);
          const title = this.translate.instant('ORDERS.SCHEDULED_REMINDER.TITLE');
          const body = this.translate.instant('ORDERS.SCHEDULED_REMINDER.BODY', {
            orderNumber,
            minutes: offsetMin,
            slot,
          });
          this.snackBar.open(body, undefined, {
            duration: 12_000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['sl-snack-scheduled-reminder'],
          });
          this.notif.showBrowserNotification(title, body, {
            notification: {
              type: 'ORDER',
              data: {
                orderId: id,
                id,
                action: 'ORDER_SCHEDULED_PREP_REMINDER',
                orderNumber,
              },
            },
          });
        }, delay);

        this.timeoutHandles.set(handleKey, h);
      }
    }
  }

  /** Charge les commandes actives partenaire puis synchronise les rappels. */
  refreshFromActiveApi(): void {
    this.refreshSub?.unsubscribe();
    this.refreshSub = this.ordersService.getPartnerActiveOrders().subscribe({
      next: (list) => this.syncFromOrders(list),
      error: () => this.syncFromOrders([]),
    });
  }

  private clearAllTimeouts(): void {
    for (const h of this.timeoutHandles.values()) {
      clearTimeout(h);
    }
    this.timeoutHandles.clear();
  }
}
