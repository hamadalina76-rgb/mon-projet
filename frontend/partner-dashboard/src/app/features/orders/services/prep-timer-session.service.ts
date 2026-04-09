import { Injectable } from '@angular/core';
import { PrepTimerContext } from '../utils/prep-timer.utils';

const TIMER_PREFIX = 'speedline_partner_prep_timer_';
const ALERT_PREFIX = 'speedline_partner_prep_alert_';

@Injectable({ providedIn: 'root' })
export class PrepTimerSessionService {
  private timerKey(orderId: string): string {
    return `${TIMER_PREFIX}${orderId}`;
  }

  private alertKey(orderId: string): string {
    return `${ALERT_PREFIX}${orderId}`;
  }

  save(orderId: string, ctx: PrepTimerContext): void {
    try {
      sessionStorage.setItem(this.timerKey(orderId), JSON.stringify(ctx));
    } catch {
      /* quota / private mode */
    }
  }

  load(orderId: string): PrepTimerContext | null {
    try {
      const raw = sessionStorage.getItem(this.timerKey(orderId));
      if (!raw) return null;
      const o = JSON.parse(raw) as PrepTimerContext;
      if (
        !o ||
        typeof o.startTimeIso !== 'string' ||
        typeof o.durationMinutes !== 'number' ||
        o.durationMinutes <= 0
      ) {
        return null;
      }
      return o;
    } catch {
      return null;
    }
  }

  clear(orderId: string): void {
    try {
      sessionStorage.removeItem(this.timerKey(orderId));
      sessionStorage.removeItem(this.alertKey(orderId));
    } catch {
      /* ignore */
    }
  }

  hasAlertPlayed(orderId: string): boolean {
    try {
      return sessionStorage.getItem(this.alertKey(orderId)) === '1';
    } catch {
      return false;
    }
  }

  markAlertPlayed(orderId: string): void {
    try {
      sessionStorage.setItem(this.alertKey(orderId), '1');
    } catch {
      /* ignore */
    }
  }
}
