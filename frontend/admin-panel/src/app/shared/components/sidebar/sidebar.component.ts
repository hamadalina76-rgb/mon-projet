// src/app/shared/components/sidebar/sidebar.component.ts
import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  permission?: string;
  children?: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatListModule,
    MatIconModule,
    MatTooltipModule,
    MatExpansionModule,
    TranslateModule,
  ],
  template: `
    <aside class="sidebar" [class.collapsed]="collapsed">
      <div class="sidebar-header">
        <img
          [src]="collapsed ? 'assets/images/logo-icon.svg' : 'assets/images/logo.svg'"
          alt="SpeedLine Admin"
          class="logo"
        />
      </div>

      <nav class="sidebar-nav">
        <mat-nav-list>
          @for (item of navItems; track item.label) {
            @if (!item.permission || hasPermission(item.permission)) {
              @if (item.children && item.children.length > 0) {
                <mat-expansion-panel class="nav-panel" [class.collapsed]="collapsed">
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon>{{ item.icon }}</mat-icon>
                      @if (!collapsed) {
                        <span>{{ item.label | translate }}</span>
                      }
                    </mat-panel-title>
                  </mat-expansion-panel-header>
                  @for (child of item.children; track child.label) {
                    @if (!child.permission || hasPermission(child.permission)) {
                      <a
                        mat-list-item
                        [routerLink]="child.route"
                        routerLinkActive="active"
                        [matTooltip]="collapsed ? (child.label | translate) : ''"
                        matTooltipPosition="right"
                      >
                        <mat-icon matListItemIcon>{{ child.icon }}</mat-icon>
                        @if (!collapsed) {
                          <span matListItemTitle>{{ child.label | translate }}</span>
                        }
                      </a>
                    }
                  }
                </mat-expansion-panel>
              } @else {
                <a
                  mat-list-item
                  [routerLink]="item.route"
                  routerLinkActive="active"
                  [matTooltip]="collapsed ? (item.label | translate) : ''"
                  matTooltipPosition="right"
                >
                  <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
                  @if (!collapsed) {
                    <span matListItemTitle>{{ item.label | translate }}</span>
                  }
                </a>
              }
            }
          }
        </mat-nav-list>
      </nav>

      <div class="sidebar-footer">
        <button mat-icon-button (click)="toggleCollapse.emit()">
          <mat-icon>{{ collapsed ? 'chevron_right' : 'chevron_left' }}</mat-icon>
        </button>
      </div>
    </aside>
  `,
  styles: [
    `
      .sidebar {
        width: 280px;
        height: 100vh;
        background: var(--bg-sidebar);
        display: flex;
        flex-direction: column;
        transition: width 0.3s ease;
        border-right: 1px solid var(--border-color);

        &.collapsed {
          width: 72px;
        }
      }

      .sidebar-header {
        padding: 1.5rem;
        display: flex;
        justify-content: center;
        border-bottom: 1px solid var(--border-color);

        .logo {
          height: 40px;
          transition: all 0.3s ease;
        }
      }

      .sidebar-nav {
        flex: 1;
        overflow-y: auto;
        padding: 0.5rem;

        .active {
          background-color: rgba(79, 70, 229, 0.1);
          color: var(--primary);
        }
      }

      .nav-panel {
        box-shadow: none;
        background: transparent;

        &.collapsed {
          ::ng-deep .mat-expansion-panel-header {
            padding: 0 16px;
          }
        }
      }

      .sidebar-footer {
        padding: 1rem;
        border-top: 1px solid var(--border-color);
        display: flex;
        justify-content: flex-end;
      }
    `,
  ],
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Output() toggleCollapse = new EventEmitter<void>();

  private authService = inject(AuthService);

  navItems: NavItem[] = [
    { label: 'nav.dashboard', icon: 'dashboard', route: '/dashboard' },
    {
      label: 'nav.users',
      icon: 'people',
      permission: 'users:read',
      children: [
        { label: 'nav.customers', icon: 'person', route: '/users/customers' },
        { label: 'nav.couriers', icon: 'delivery_dining', route: '/users/couriers' },
        { label: 'nav.admins', icon: 'admin_panel_settings', route: '/users/admins', permission: 'admins:read' },
      ],
    },
    { label: 'nav.partners', icon: 'store', route: '/partners', permission: 'partners:read' },
    { label: 'nav.orders', icon: 'receipt', route: '/orders', permission: 'orders:read' },
    {
      label: 'nav.payments',
      icon: 'payments',
      permission: 'payments:read',
      children: [
        { label: 'nav.transactions', icon: 'credit_card', route: '/payments/transactions' },
        { label: 'nav.payouts', icon: 'account_balance', route: '/payments/payouts' },
        { label: 'nav.refunds', icon: 'replay', route: '/payments/refunds' },
      ],
    },
    { label: 'nav.promotions', icon: 'local_offer', route: '/promotions', permission: 'promotions:read' },
    { label: 'nav.reviews', icon: 'reviews', route: '/reviews', permission: 'reviews:read' },
    { label: 'nav.support', icon: 'support_agent', route: '/support', permission: 'support:read' },
    { label: 'nav.zones', icon: 'map', route: '/zones', permission: 'zones:read' },
    {
      label: 'nav.analytics',
      icon: 'analytics',
      permission: 'analytics:read',
      children: [
        { label: 'nav.overview', icon: 'insights', route: '/analytics/overview' },
        { label: 'nav.revenue', icon: 'trending_up', route: '/analytics/revenue' },
        { label: 'nav.reports', icon: 'summarize', route: '/analytics/reports' },
      ],
    },
    { label: 'nav.notifications', icon: 'notifications', route: '/notifications', permission: 'notifications:send' },
    { label: 'nav.settings', icon: 'settings', route: '/settings', permission: 'settings:read' },
    { label: 'nav.monitoring', icon: 'monitor_heart', route: '/monitoring', permission: 'monitoring:read' },
  ];

  hasPermission(permission: string): boolean {
    const user = this.authService.currentUser();
    if (!user) return false;
    return user.permissions?.includes(permission) ?? false;
  }
}
