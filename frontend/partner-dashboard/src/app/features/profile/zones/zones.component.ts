import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-zones',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatIconModule, TranslateModule],
  template: `
    <div class="zones-page">
      <nav class="breadcrumb">
        <a routerLink="/dashboard">{{ 'profilePages.breadcrumbSettings' | translate }}</a>
        <span class="sep">›</span>
        <a routerLink="/profile">{{ 'profilePages.breadcrumbStoreProfile' | translate }}</a>
        <span class="sep">›</span>
        <span class="current">{{ 'profilePages.breadcrumbZones' | translate }}</span>
      </nav>
      <header class="page-header">
        <h1>{{ 'profilePages.zonesTitle' | translate }}</h1>
        <p class="page-subtitle">{{ 'profilePages.zonesSubtitle' | translate }}</p>
      </header>
      <mat-card class="coming-card">
        <mat-icon>map</mat-icon>
        <h2>{{ 'profilePages.comingSoon' | translate }}</h2>
        <p>{{ 'profilePages.zonesComingSoon' | translate }}</p>
      </mat-card>
    </div>
  `,
  styles: [`
    @use 'assets/styles/variables' as *;
    .zones-page {
      padding: 0 $spacing-lg 2rem;
      max-width: 800px;
      margin: 0 auto;
    }
    .breadcrumb {
      display: flex; align-items: center; flex-wrap: wrap; gap: 0.35rem;
      font-size: 0.875rem; margin-bottom: 1rem;
      .back-link {
        display: inline-flex; align-items: center; gap: 0.35rem;
        color: $text-secondary; text-decoration: none; font-weight: 500;
        &:hover { color: $primary-color; }
        mat-icon { font-size: 1.2rem; width: 1.2rem; height: 1.2rem; }
      }
      a:not(.back-link) { color: $text-secondary; text-decoration: none; &:hover { color: $primary-color; } }
      .sep { color: $gray-400; }
      .current { color: $text-primary; font-weight: 500; }
    }
    .page-header {
      margin-bottom: 1.5rem;
      h1 { margin: 0 0 0.35rem; font-size: 1.5rem; font-weight: 700; color: $text-primary; }
      .page-subtitle { margin: 0; font-size: 0.9375rem; color: $text-secondary; line-height: 1.5; }
    }
    .coming-card {
      padding: 3rem; text-align: center;
      border-radius: $border-radius-lg;
      box-shadow: $shadow-sm;
      border: 1px solid $gray-200;
      background: $white;
      mat-icon { font-size: 64px; width: 64px; height: 64px; color: $primary-color; margin-bottom: 1rem; }
      h2 { margin: 0 0 0.5rem; font-size: 1.25rem; color: $text-primary; }
      p { margin: 0; color: $text-secondary; }
    }
  `],
})
export class ZonesComponent {}
