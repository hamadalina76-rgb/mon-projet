// src/app/core/services/inactivity.service.ts
import { Injectable, inject, NgZone, OnDestroy } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from './auth.service';
import { ActivityLogService } from './activity-log.service';

const INACTIVITY_TIMEOUT = 15 * 60 * 1000;   // 15 minutes
const WARNING_BEFORE   =  2 * 60 * 1000;     // warn 2 minutes before

@Injectable({ providedIn: 'root' })
export class InactivityService implements OnDestroy {
  private authService = inject(AuthService);
  private activityLog = inject(ActivityLogService);
  private ngZone = inject(NgZone);

  private timeoutId: ReturnType<typeof setTimeout> | null = null;
  private warningId: ReturnType<typeof setTimeout> | null = null;
  private events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
  private boundReset = this.resetTimer.bind(this);
  private started = false;

  /**
   * Start monitoring user activity
   */
  start(): void {
    if (this.started) return;
    this.started = true;

    this.ngZone.runOutsideAngular(() => {
      this.events.forEach((e) => document.addEventListener(e, this.boundReset, { passive: true }));
      this.resetTimer();
    });
  }

  /**
   * Stop monitoring
   */
  stop(): void {
    this.started = false;
    this.clearTimers();
    this.events.forEach((e) => document.removeEventListener(e, this.boundReset));
  }

  ngOnDestroy(): void {
    this.stop();
  }

  private resetTimer(): void {
    this.clearTimers();

    // Warning timer
    this.warningId = setTimeout(() => {
      this.ngZone.run(() => {
        // Could show a dialog here; for now we just log
        console.warn('[InactivityService] Session will expire in 2 minutes');
      });
    }, INACTIVITY_TIMEOUT - WARNING_BEFORE);

    // Logout timer
    this.timeoutId = setTimeout(() => {
      this.ngZone.run(() => {
        this.activityLog.log('AUTO_LOGOUT', 'session', undefined, 'Logged out due to inactivity');
        this.authService.logout();
      });
    }, INACTIVITY_TIMEOUT);
  }

  private clearTimers(): void {
    if (this.timeoutId) { clearTimeout(this.timeoutId); this.timeoutId = null; }
    if (this.warningId) { clearTimeout(this.warningId); this.warningId = null; }
  }
}
