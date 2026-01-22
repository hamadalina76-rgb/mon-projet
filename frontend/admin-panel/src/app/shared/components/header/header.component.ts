// src/app/shared/components/header/header.component.ts
import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule,
    TranslateModule,
  ],
  template: `
    <mat-toolbar class="header">
      <button mat-icon-button (click)="toggleSidebar.emit()">
        <mat-icon>menu</mat-icon>
      </button>

      <span class="spacer"></span>

      <!-- Language Selector -->
      <button mat-icon-button [matMenuTriggerFor]="langMenu">
        <mat-icon>language</mat-icon>
      </button>
      <mat-menu #langMenu="matMenu">
        <button mat-menu-item (click)="changeLanguage('fr')">
          <span>🇫🇷 Français</span>
        </button>
        <button mat-menu-item (click)="changeLanguage('en')">
          <span>🇬🇧 English</span>
        </button>
        <button mat-menu-item (click)="changeLanguage('ar')">
          <span>🇹🇳 العربية</span>
        </button>
      </mat-menu>

      <!-- Alerts -->
      <button mat-icon-button [matMenuTriggerFor]="alertsMenu">
        <mat-icon [matBadge]="alertCount" matBadgeColor="warn" [matBadgeHidden]="alertCount === 0">
          warning
        </mat-icon>
      </button>
      <mat-menu #alertsMenu="matMenu" class="alerts-menu">
        <div class="menu-header">
          <span>{{ 'header.systemAlerts' | translate }}</span>
        </div>
        <mat-divider></mat-divider>
        <div class="menu-content">
          <p class="no-alerts">{{ 'header.noAlerts' | translate }}</p>
        </div>
      </mat-menu>

      <!-- Notifications -->
      <button mat-icon-button [matMenuTriggerFor]="notifMenu">
        <mat-icon [matBadge]="notificationCount" matBadgeColor="primary" [matBadgeHidden]="notificationCount === 0">
          notifications
        </mat-icon>
      </button>
      <mat-menu #notifMenu="matMenu" class="notifications-menu">
        <div class="menu-header">
          <span>{{ 'header.notifications' | translate }}</span>
        </div>
        <mat-divider></mat-divider>
        <div class="menu-content">
          <p class="no-notifications">{{ 'header.noNotifications' | translate }}</p>
        </div>
        <mat-divider></mat-divider>
        <a mat-menu-item routerLink="/notifications/history" class="view-all">
          {{ 'header.viewAll' | translate }}
        </a>
      </mat-menu>

      <!-- User Menu -->
      <button mat-button [matMenuTriggerFor]="userMenu" class="user-menu-trigger">
        <div class="user-info">
          <mat-icon>account_circle</mat-icon>
          <span class="user-name">{{ currentUser()?.firstName }} {{ currentUser()?.lastName }}</span>
          <span class="user-role">{{ currentUser()?.role }}</span>
        </div>
        <mat-icon>arrow_drop_down</mat-icon>
      </button>
      <mat-menu #userMenu="matMenu">
        <a mat-menu-item routerLink="/profile">
          <mat-icon>person</mat-icon>
          <span>{{ 'header.profile' | translate }}</span>
        </a>
        <a mat-menu-item routerLink="/settings">
          <mat-icon>settings</mat-icon>
          <span>{{ 'header.settings' | translate }}</span>
        </a>
        <mat-divider></mat-divider>
        <button mat-menu-item (click)="onLogout()">
          <mat-icon>logout</mat-icon>
          <span>{{ 'header.logout' | translate }}</span>
        </button>
      </mat-menu>
    </mat-toolbar>
  `,
  styles: [
    `
      .header {
        position: sticky;
        top: 0;
        z-index: 100;
        background: var(--bg-card);
        border-bottom: 1px solid var(--border-color);
      }

      .spacer {
        flex: 1;
      }

      .user-menu-trigger {
        .user-info {
          display: flex;
          align-items: center;
          gap: 0.5rem;

          .user-name {
            font-weight: 500;
          }

          .user-role {
            font-size: 0.75rem;
            color: var(--text-secondary);
          }
        }
      }

      .menu-header {
        padding: 0.75rem 1rem;
        font-weight: 600;
      }

      .menu-content {
        padding: 1rem;
        min-width: 280px;
      }

      .no-alerts,
      .no-notifications {
        color: var(--text-secondary);
        text-align: center;
        margin: 0;
      }

      .view-all {
        text-align: center;
        color: var(--primary);
      }
    `,
  ],
})
export class HeaderComponent {
  @Input() notificationCount = 0;
  @Input() alertCount = 0;
  @Output() toggleSidebar = new EventEmitter<void>();

  private authService = inject(AuthService);
  private translateService = inject(TranslateService);

  currentUser = this.authService.currentUser;

  changeLanguage(lang: string): void {
    this.translateService.use(lang);
    localStorage.setItem('language', lang);
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }

  onLogout(): void {
    this.authService.logout();
  }
}
