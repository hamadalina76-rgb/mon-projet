// src/app/shared/components/header/header.component.ts
import { Component, Input, Output, EventEmitter, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';

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
export class HeaderComponent {
  @Input() notificationCount = 0;
  @Input() alertCount = 0;
  @Output() toggleSidebar = new EventEmitter<void>();

  private authService = inject(AuthService);
  private translate = inject(TranslateService);

  currentUser = this.authService.currentUser;

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
        // TODO: navigate to profile
        break;
      case 'settings':
        // TODO: navigate to settings
        break;
    }
  }
}
