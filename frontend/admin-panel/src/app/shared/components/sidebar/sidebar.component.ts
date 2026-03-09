// src/app/shared/components/sidebar/sidebar.component.ts
import { Component, Input, Output, EventEmitter, inject, computed, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';
import { ThemeService } from '@core/services/theme.service';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  permission?: string;
  section?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule, MatTooltipModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent implements OnInit, OnDestroy {
  @Input() mobileOpen = false;
  @Input() collapsed = false;
  @Output() closeMobile = new EventEmitter<void>();
  @Output() toggle = new EventEmitter<void>();

  private authService = inject(AuthService);
  themeService = inject(ThemeService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();

  currentLang = signal(this.translate.currentLang || 'fr');

  /** Chevron inversé en RTL (arabe) */
  chevronExpand = computed(() =>
    this.currentLang() === 'ar' ? 'chevron_left' : 'chevron_right'
  );
  chevronCollapse = computed(() =>
    this.currentLang() === 'ar' ? 'chevron_right' : 'chevron_left'
  );

  sections = ['nav.mainMenu', 'nav.operations', 'nav.system'];

  navItems: NavItem[] = [
    // ── Main Menu ──
    { label: 'nav.dashboard', icon: 'dashboard', route: '/dashboard', permission: 'dashboard:view', section: 'nav.mainMenu' },
    { label: 'nav.orders', icon: 'shopping_cart', route: '/orders', permission: 'orders:view', section: 'nav.mainMenu' },
    { label: 'nav.partners', icon: 'storefront', route: '/partners', permission: 'partners:view', section: 'nav.mainMenu' },
    { label: 'nav.delivery', icon: 'local_shipping', route: '/delivery', permission: 'delivery:view', section: 'nav.mainMenu' },
    { label: 'nav.couriers', icon: 'delivery_dining', route: '/users/couriers', permission: 'users:view', section: 'nav.mainMenu' },
    { label: 'nav.customers', icon: 'group', route: '/users/customers', permission: 'users:view', section: 'nav.mainMenu' },
    { label: 'nav.admins', icon: 'admin_panel_settings', route: '/users/admins', permission: 'admins:view', section: 'nav.mainMenu' },

    // ── Operations ──
    { label: 'nav.categories', icon: 'category', route: '/categories', permission: 'categories:view', section: 'nav.operations' },
    { label: 'nav.payments', icon: 'payments', route: '/payments', permission: 'payments:view', section: 'nav.operations' },
    { label: 'nav.promotions', icon: 'local_offer', route: '/promotions', permission: 'promotions:view', section: 'nav.operations' },
    { label: 'nav.reviews', icon: 'rate_review', route: '/reviews', permission: 'reviews:view', section: 'nav.operations' },
    { label: 'nav.support', icon: 'support_agent', route: '/support', permission: 'support:view', section: 'nav.operations' },
    { label: 'nav.zones', icon: 'map', route: '/zones', permission: 'zones:view', section: 'nav.operations' },
    { label: 'nav.analytics', icon: 'analytics', route: '/analytics', permission: 'analytics:view', section: 'nav.operations' },
    { label: 'nav.notifications', icon: 'notifications_active', route: '/notifications', permission: 'notifications:view', section: 'nav.operations' },

    // ── System ──
    { label: 'nav.settings', icon: 'settings', route: '/settings', permission: 'settings:view', section: 'nav.system' },
    { label: 'nav.monitoring', icon: 'monitor_heart', route: '/monitoring', permission: 'monitoring:view', section: 'nav.system' },
  ];

  ngOnInit(): void {
    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((e) => this.currentLang.set(e.lang));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  hasPermission(permission: string): boolean {
    return this.authService.hasPermission(permission);
  }

  getItemsBySection(section: string): NavItem[] {
    return this.navItems.filter(item => item.section === section);
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  onNavClick(): void {
    this.closeMobile.emit();
  }

  onToggle(): void {
    this.closeMobile.emit();
    this.toggle.emit();
  }
}
