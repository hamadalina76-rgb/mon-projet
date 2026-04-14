// src/app/core/services/notification.service.ts
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { Title } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { ApiService } from './api.service';

export interface Notification {
  id: string;
  userId: number;
  type: string;
  title: string;
  message: string;
  data?: any;
  isRead: boolean;
  createdAt: string;
}

const SOUND_PREF_KEY = 'speedline_sound_enabled';
const APP_TITLE = 'SpeedLine Partner';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private api = inject(ApiService);
  private title = inject(Title);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  private audioCtx: AudioContext | null = null;
  private pendingAudioPlay = false;

  /** Reactive pending orders count — drives tab title */
  readonly pendingCount = signal(0);

  // ---------------------------------------------------------------------------
  // REST API
  // ---------------------------------------------------------------------------

  getNotifications(userId: number, page = 0, size = 20): Observable<any> {
    return this.api.get(`notifications/${userId}?page=${page}&size=${size}`);
  }

  markAsRead(notificationId: string): Observable<any> {
    return this.api.put(`notifications/${notificationId}/read`, {});
  }

  markAllAsRead(userId: number): Observable<any> {
    return this.api.put(`notifications/${userId}/read-all`, {});
  }

  getUnreadCount(userId: number): Observable<any> {
    return this.api.get(`notifications/${userId}/unread-count`);
  }

  // ---------------------------------------------------------------------------
  // Order alert — sound + snackbar + tab title
  // ---------------------------------------------------------------------------

  /**
   * Full alert pipeline for a new order.
   * Call this from MainLayoutComponent or OrdersListComponent when an order arrives.
   */
  newOrderAlert(orderNumber: string): void {
    this.playNewOrderSound();
    this.incrementPending();
  }

  /**
   * Displays a MatSnackBar banner with customer name and order total.
   * @param order - partial order object with at minimum orderNumber, customer.name, total
   */
  showOrderBanner(order: any): void {
    const customerName =
      order?.customer?.name ??
      order?.customerName ??
      this.translate.instant('ORDERS.DEFAULT_CUSTOMER_NAME');
    const orderNumber = order?.orderNumber ?? '';
    const total = order?.total != null
      ? `${Number(order.total).toFixed(2)} TND`
      : '';

    const message = `🛍️ #${orderNumber} — ${customerName}${total ? '  •  ' + total : ''}`;

    this.snackBar.open(message, this.translate.instant('ORDERS.BANNER_VIEW'), {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['new-order-snackbar'],
    });
  }

  // ---------------------------------------------------------------------------
  // Pending count + tab title
  // ---------------------------------------------------------------------------

  incrementPending(): void {
    this.pendingCount.update(n => n + 1);
    this.updateTabTitle(this.pendingCount());
  }

  decrementPending(): void {
    this.pendingCount.update(n => Math.max(0, n - 1));
    this.updateTabTitle(this.pendingCount());
  }

  resetPending(): void {
    this.pendingCount.set(0);
    this.updateTabTitle(0);
  }

  updateTabTitle(count: number): void {
    this.title.setTitle(count > 0 ? `(${count}) ${APP_TITLE}` : APP_TITLE);
  }

  // ---------------------------------------------------------------------------
  // Audio
  // ---------------------------------------------------------------------------

  playNewOrderSound(): void {
    if (!this.isSoundEnabled()) return;

    try {
      const ctx = this.getAudioContext();
      this.playRestaurantBell(ctx);
    } catch {
      // AudioContext blocked until user interaction
      this.pendingAudioPlay = true;
    }
  }

  /** Short urgent alert when prep deadline is reached (partner dashboard). */
  playPrepDeadlineSound(): void {
    if (!this.isSoundEnabled()) return;
    try {
      const ctx = this.getAudioContext();
      this.playPrepDeadlineTone(ctx);
    } catch {
      this.pendingAudioPlay = true;
    }
  }

  /**
   * Call this on the first user interaction (click anywhere) to unlock autoplay.
   * MainLayoutComponent binds (click) on the root element.
   */
  unlockAudio(): void {
    if (!this.pendingAudioPlay) return;
    this.pendingAudioPlay = false;
    try {
      const ctx = this.getAudioContext();
      // Resume suspended context (required after user gesture)
      if (ctx.state === 'suspended') {
        ctx.resume().then(() => this.playRestaurantBell(ctx));
      } else {
        this.playRestaurantBell(ctx);
      }
    } catch { /* ignore */ }
  }

  // ---------------------------------------------------------------------------
  // Web Audio — restaurant bell synthesizer
  // ---------------------------------------------------------------------------

  private getAudioContext(): AudioContext {
    if (!this.audioCtx || this.audioCtx.state === 'closed') {
      this.audioCtx = new AudioContext();
    }
    return this.audioCtx;
  }

  /**
   * Synthesises a two-tone restaurant bell (DING-DONG).
   * Tone 1: 880 Hz → Tone 2: 659 Hz, each with a sharp attack and long decay.
   * Volume is set to ~0.9 (strong) with a compressor to avoid clipping.
   */
  private playRestaurantBell(ctx: AudioContext): void {
    const masterGain = ctx.createGain();
    masterGain.gain.value = 0.9;

    // Soft limiter to avoid clipping on loud speakers
    const compressor = ctx.createDynamicsCompressor();
    compressor.threshold.value = -6;
    compressor.knee.value = 3;
    compressor.ratio.value = 4;
    compressor.attack.value = 0.002;
    compressor.release.value = 0.2;

    masterGain.connect(compressor);
    compressor.connect(ctx.destination);

    const bell = (freq: number, startTime: number) => {
      const osc  = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, startTime);

      // Harmonics for a metallic bell timbre
      const osc2  = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(freq * 2.756, startTime);
      gain2.gain.value = 0.25;

      // Sharp attack (1ms), long exponential decay (1.4s)
      gain.gain.setValueAtTime(0, startTime);
      gain.gain.linearRampToValueAtTime(1, startTime + 0.001);
      gain.gain.exponentialRampToValueAtTime(0.001, startTime + 1.4);

      osc.connect(gain);
      gain.connect(masterGain);
      osc2.connect(gain2);
      gain2.connect(masterGain);

      osc.start(startTime);
      osc.stop(startTime + 1.4);
      osc2.start(startTime);
      osc2.stop(startTime + 1.4);
    };

    const now = ctx.currentTime;
    bell(880, now);        // DING — La5
    bell(659, now + 0.35); // DONG — Mi5
  }

  /** Two fast high beeps — distinct from new-order bell. */
  private playPrepDeadlineTone(ctx: AudioContext): void {
    const g = ctx.createGain();
    g.gain.value = 0.45;
    const comp = ctx.createDynamicsCompressor();
    comp.threshold.value = -8;
    comp.ratio.value = 3;
    g.connect(comp);
    comp.connect(ctx.destination);

    const beep = (freq: number, t0: number) => {
      const osc = ctx.createOscillator();
      const gn = ctx.createGain();
      osc.type = 'square';
      osc.frequency.setValueAtTime(freq, t0);
      gn.gain.setValueAtTime(0, t0);
      gn.gain.linearRampToValueAtTime(0.7, t0 + 0.01);
      gn.gain.exponentialRampToValueAtTime(0.001, t0 + 0.12);
      osc.connect(gn);
      gn.connect(g);
      osc.start(t0);
      osc.stop(t0 + 0.13);
    };

    const t = ctx.currentTime;
    beep(1200, t);
    beep(1200, t + 0.18);
  }

  // ---------------------------------------------------------------------------
  // Sound preference
  // ---------------------------------------------------------------------------

  isSoundEnabled(): boolean {
    const stored = localStorage.getItem(SOUND_PREF_KEY);
    return stored === null ? true : stored === 'true';
  }

  toggleSound(): boolean {
    const next = !this.isSoundEnabled();
    localStorage.setItem(SOUND_PREF_KEY, String(next));
    return next;
  }

  // ---------------------------------------------------------------------------
  // Browser (OS) notifications
  // ---------------------------------------------------------------------------

  async requestBrowserPermission(): Promise<void> {
    if (!('Notification' in window)) return;
    if (Notification.permission !== 'granted') {
      await Notification.requestPermission();
    }
  }

  showBrowserNotification(title: string, body: string, icon?: string): void {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: icon || '/assets/images/logo.svg',
        badge: '/assets/images/logo-icon.svg',
      });
    }
  }

  /** Rappel émis par le serveur (15 min + prépa avant le créneau). */
  showScheduledPrepReminderFromServer(notif: { title?: string; message?: string }): void {
    this.playPrepDeadlineSound();
    const title =
      (notif.title && notif.title.trim()) ||
      this.translate.instant('ORDERS.NOTIF_SCHEDULED_PREP_TITLE');
    const message = (notif.message && notif.message.trim()) || '';
    const line = message ? `${title} — ${message}` : title;
    this.snackBar.open(line, undefined, {
      duration: 10_000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['sl-snack-scheduled-reminder'],
    });
    this.showBrowserNotification(title, message);
  }
}
