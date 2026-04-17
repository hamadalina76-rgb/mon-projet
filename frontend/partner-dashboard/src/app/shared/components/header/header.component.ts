// src/app/shared/components/header/header.component.ts - Angular 19
import { Component, inject, input, output, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { WebSocketService, PartnerNotification } from '@core/services/websocket.service';
import { NotificationService } from '@core/services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit, OnDestroy {
  authService = inject(AuthService); // Made public for template access
  private wsService = inject(WebSocketService);
  private notificationService = inject(NotificationService);
  private translate = inject(TranslateService);
  private wsSub: Subscription | null = null;
  private audioContext: AudioContext | null = null;
  private lastSoundAt = 0;

  currentUser = this.authService.currentUser;

  // Angular 19 Signal Input / Output
  sidebarCollapsed = input(false);
  isMobile = input(false);
  sidebarMobileOpen = input(false);
  toggleSidebar = output<void>();

  isSidebarVisible = computed(() =>
    this.isMobile() ? this.sidebarMobileOpen() : !this.sidebarCollapsed()
  );

  // Angular 19 Signals
  showUserMenu = signal(false);
  showNotifications = signal(false);
  showLangMenu = signal(false);
  notifications = signal<PartnerNotification[]>([]);
  unreadCount = signal(0);
  currentLang = signal<string>(localStorage.getItem('partnerLang') || 'fr');

  availableLanguages = [
    { code: 'fr', name: 'Français', flagIcon: 'assets/image/flags/fr.svg' },
    { code: 'en', name: 'English', flagIcon: 'assets/image/flags/gb.svg' },
    { code: 'ar', name: 'العربية', flagIcon: 'assets/image/flags/tn.svg' },
  ];

  currentLangFlag = computed(() => {
    const code = this.currentLang();
    const found = this.availableLanguages.find((l) => l.code === code);
    return found?.flagIcon ?? 'assets/image/flags/fr.svg';
  });

  ngOnInit(): void {
    const savedLang = localStorage.getItem('partnerLang') || 'fr';
    this.currentLang.set(savedLang);
    this.translate.use(savedLang);
    this.updateDirection(savedLang);

    // NB: MainLayoutComponent gère connect() / disconnect() — le header ne touche pas
    // au cycle de vie du WebSocket (singleton partagé).
    const user = this.currentUser();
    const partnerId = user?.partnerId;

    if (partnerId) {
      this.wsSub = this.wsService.onPartnerNotification.subscribe((notif: PartnerNotification) => {
        this.notifications.update((list) => [notif, ...list].slice(0, 20));
        this.unreadCount.update((c) => c + 1);
        this.playNotificationSound();
      });

      this.loadNotifications(Number(user!.id));
      this.loadUnreadCount(Number(user!.id));
    }
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    // NE PAS appeler wsService.disconnect() ici — MainLayoutComponent s'en charge.
  }

  private loadNotifications(userId: number): void {
    this.notificationService.getNotifications(userId, 0, 20).subscribe({
      next: (response) => {
        if (response.content) {
          this.notifications.set(response.content);
        }
      },
      error: (err) => {
        console.error('Failed to load notifications:', err);
      }
    });
  }

  private loadUnreadCount(userId: number): void {
    this.notificationService.getUnreadCount(userId).subscribe({
      next: (response) => {
        if (response.count !== undefined) {
          this.unreadCount.set(response.count);
        }
      },
      error: (err) => {
        console.error('Failed to load unread count:', err);
      }
    });
  }

  toggleLangMenu(): void {
    this.showLangMenu.update(v => !v);
    this.showNotifications.set(false);
    this.showUserMenu.set(false);
  }

  toggleUserMenu(): void {
    this.showUserMenu.update(v => !v);
    this.showNotifications.set(false);
    this.showLangMenu.set(false);
  }

  toggleNotifications(): void {
    const opening = !this.showNotifications();
    this.showNotifications.update(v => !v);
    this.showUserMenu.set(false);
    this.showLangMenu.set(false);
    // Reload from API when opening so history (including read notifications) is up to date
    if (opening) {
      const user = this.currentUser();
      if (user) {
        this.loadNotifications(Number(user.id));
        this.loadUnreadCount(Number(user.id));
      }
    }
  }

  onViewAllNotifications(): void {
    this.showNotifications.set(false);
  }

  selectLanguage(lang: string): void {
    this.currentLang.set(lang);
    this.translate.use(lang);
    localStorage.setItem('partnerLang', lang);
    this.updateDirection(lang);
    this.showLangMenu.set(false);
  }

  private updateDirection(lang: string): void {
    const html = document.documentElement;
    if (lang === 'ar') {
      html.setAttribute('dir', 'rtl');
      html.setAttribute('lang', 'ar');
    } else {
      html.setAttribute('dir', 'ltr');
      html.setAttribute('lang', lang);
    }
  }

  markAllNotificationsRead(): void {
    const user = this.currentUser();
    if (!user) return;

    this.notificationService.markAllAsRead(Number(user.id)).subscribe({
      next: () => {
        this.unreadCount.set(0);
        this.notifications.update(list => 
          list.map(n => ({ ...n, isRead: true }))
        );
      },
      error: (err) => {
        console.error('Failed to mark all as read:', err);
      }
    });
  }

  markNotificationRead(notif: PartnerNotification): void {
    if (notif.isRead) return;

    this.notificationService.markAsRead(notif.id).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => n.id === notif.id ? { ...n, isRead: true } : n)
        );
        this.unreadCount.update(c => Math.max(0, c - 1));
      },
      error: (err) => {
        console.error('Failed to mark notification as read:', err);
      }
    });
  }

  onNotificationClick(notif: PartnerNotification): void {
    this.markNotificationRead(notif);

    void this.notificationService.navigateFromNotification({
      type: notif.type,
      channel: notif.channel,
      data: notif.data,
    });

    this.showNotifications.set(false);
  }

  /** Icône Material selon le type / action (logique hors template pour NG8107). */
  notifIconName(notif: PartnerNotification): string {
    const action = this.notifDataAction(notif);
    if (action === 'ADMIN_CONTACT') return 'mark_unread_chat_alt';
    if (notif.type === 'ORDER') return 'shopping_bag';
    if (notif.type === 'PARTNER' || action === 'PARTNER_APPROVED') return 'check_circle';
    return 'notifications';
  }

  private notifDataAction(notif: PartnerNotification): string | undefined {
    const d = notif.data;
    if (d == null) return undefined;
    const raw = d['action'];
    if (raw == null) return undefined;
    return typeof raw === 'string' ? raw : String(raw);
  }

  logout(): void {
    this.authService.logout();
  }

  private playNotificationSound(): void {
    // Some browsers block audio until user gesture; this call is triggered on WS events
    // after login interactions, so it generally works once the app is in use.
    try {
      const now = Date.now();
      if (now - this.lastSoundAt < 250) return; // throttle overlapping sounds
      this.lastSoundAt = now;

      const AC = (window as any).AudioContext || (window as any).webkitAudioContext;
      if (!AC) return;
      if (!this.audioContext) this.audioContext = new AC();

      const ctx = this.audioContext;
      if (!ctx) return;
      if (ctx.state === 'suspended') {
        ctx.resume().catch(() => {});
      }

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      // Square wave is more noticeable than sine.
      osc.type = 'square';
      osc.frequency.setValueAtTime(880, ctx.currentTime);

      // Louder than the admin version (user reported partner sound as weak).
      gain.gain.setValueAtTime(0.001, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.12, ctx.currentTime + 0.01);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.18);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.22);
    } catch {
      // ignore autoplay/audio restrictions silently
    }
  }
}
