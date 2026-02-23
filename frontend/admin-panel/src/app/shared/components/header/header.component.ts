// src/app/shared/components/header/header.component.ts
import { Component, Input, Output, EventEmitter, inject, signal, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { WebSocketService, WebSocketNotification } from '@core/services/websocket.service';
import { NotificationService } from '@core/services/notification.service';
import { Subscription } from 'rxjs';

interface DropdownItem {
  labelKey: string;
  icon: string;
  action: string;
  danger?: boolean;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Input() notificationCount = 0;
  @Input() alertCount = 0;
  @Output() toggleSidebar = new EventEmitter<void>();

  private authService = inject(AuthService);
  private translate = inject(TranslateService);
  private router = inject(Router);
  private wsService = inject(WebSocketService);
  private notificationService = inject(NotificationService);
  private wsSub: Subscription | null = null;

  currentUser = this.authService.currentUser;
  notifications = signal<WebSocketNotification[]>([]);
  unreadCount = signal(0);

  // Dropdown states
  showUserMenu = signal(false);
  showNotifMenu = signal(false);
  showLangMenu = signal(false);

  // Current language
  currentLang = signal(this.translate.currentLang || 'fr');

  languages = [
    { code: 'fr', label: 'Français', flag: '🇫🇷' },
    { code: 'en', label: 'English', flag: '🇬🇧' },
    { code: 'ar', label: 'العربية', flag: '🇹🇳' },
  ];

  userMenuItems: DropdownItem[] = [
    { labelKey: 'header.profile', icon: 'person', action: 'profile' },
    { labelKey: 'header.settings', icon: 'settings', action: 'settings' },
    { labelKey: 'header.logout', icon: 'logout', action: 'logout', danger: true },
  ];

  ngOnInit(): void {
    this.wsService.connect();
    this.wsSub = this.wsService.onAdminNotification.subscribe((notif) => {
      this.notifications.update((list) => [notif, ...list].slice(0, 20));
      this.unreadCount.update((c) => c + 1);
    });

    // Load existing notifications and unread count (admin-specific endpoints)
    const user = this.currentUser();
    if (user) {
      this.loadAdminNotifications(Number(user.id));
      this.loadAdminUnreadCount(Number(user.id));
    }
  }

  private loadAdminNotifications(adminUserId: number): void {
    this.notificationService.getAdminNotifications(adminUserId, 0, 20).subscribe({
      next: (response) => {
        if (response?.content) {
          this.notifications.set(response.content);
        }
      },
      error: (err) => {
        console.error('Failed to load admin notifications:', err);
      }
    });
  }

  private loadAdminUnreadCount(adminUserId: number): void {
    this.notificationService.getUnreadCountForAdmin(adminUserId).subscribe({
      next: (response) => {
        if (response?.count !== undefined) {
          this.unreadCount.set(response.count);
        }
      },
      error: (err) => {
        console.error('Failed to load admin unread count:', err);
      }
    });
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.wsService.disconnect();
  }

  markAllNotificationsRead(): void {
    const user = this.currentUser();
    if (!user) return;

    this.notificationService.markAllAsReadForAdmin(Number(user.id)).subscribe({
      next: () => {
        this.unreadCount.set(0);
        // Update all notifications to read
        this.notifications.update(list => 
          list.map(n => ({ ...n, isRead: true }))
        );
      },
      error: (err) => {
        console.error('Failed to mark all as read:', err);
      }
    });
  }

  markNotificationRead(notif: WebSocketNotification): void {
    if (notif.isRead) return;

    this.notificationService.markAsRead(notif.id).subscribe({
      next: () => {
        // Update notification in list
        this.notifications.update(list =>
          list.map(n => n.id === notif.id ? { ...n, isRead: true } : n)
        );
        // Decrement unread count
        this.unreadCount.update(c => Math.max(0, c - 1));
      },
      error: (err) => {
        console.error('Failed to mark notification as read:', err);
      }
    });
  }

  navigateToPartner(notif: WebSocketNotification): void {
    this.markNotificationRead(notif);
    const partnerId = notif.data?.['partnerId'];
    if (partnerId) {
      this.router.navigate(['/partners', partnerId, 'approval']);
    }
    this.showNotifMenu.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-block-wrapper')) this.showUserMenu.set(false);
    if (!target.closest('.notif-wrapper')) this.showNotifMenu.set(false);
    if (!target.closest('.lang-wrapper')) this.showLangMenu.set(false);
  }

  getRoleLabel(role?: string): string {
    const map: Record<string, string> = {
      SUPER_ADMIN: 'Super Admin',
      FINANCE_ADMIN: 'Finance Admin',
      SUPPORT_ADMIN: 'Support Admin',
      CONTENT_MODERATOR: 'Content Moderator',
    };
    return role ? map[role] || role : 'Admin';
  }

  getInitials(): string {
    const u = this.currentUser();
    if (!u) return 'A';
    return ((u.firstName?.[0] || '') + (u.lastName?.[0] || '')).toUpperCase() || 'A';
  }

  toggleUserMenu(): void {
    this.showUserMenu.update(v => !v);
    this.showNotifMenu.set(false);
    this.showLangMenu.set(false);
  }

  toggleNotifMenu(): void {
    this.showNotifMenu.update(v => !v);
    this.showUserMenu.set(false);
    this.showLangMenu.set(false);
  }

  toggleLangMenu(): void {
    this.showLangMenu.update(v => !v);
    this.showUserMenu.set(false);
    this.showNotifMenu.set(false);
  }

  switchLang(code: string): void {
    this.translate.use(code);
    this.currentLang.set(code);
    document.documentElement.lang = code;
    document.documentElement.dir = code === 'ar' ? 'rtl' : 'ltr';
    localStorage.setItem('admin_lang', code);
    this.showLangMenu.set(false);
  }

  onUserMenuAction(action: string): void {
    this.showUserMenu.set(false);
    switch (action) {
      case 'logout':
        this.authService.logout();
        break;
      case 'profile':
        this.router.navigate(['/settings/profile']);
        break;
      case 'settings':
        this.router.navigate(['/settings/change-password']);
        break;
    }
  }
}
