// src/app/layout/admin-layout/admin-layout.component.ts
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { HeaderComponent } from '@shared/components/header/header.component';
import { LoadingService } from '@core/services/loading.service';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarComponent,
    HeaderComponent,
    LoadingSpinnerComponent,
  ],
  template: `
    <div class="admin-layout" [class.sidebar-collapsed]="sidebarCollapsed()">
      <app-sidebar
        [collapsed]="sidebarCollapsed()"
        (toggleCollapse)="toggleSidebar()"
      ></app-sidebar>

      <div class="main-content">
        <app-header
          [notificationCount]="notificationCount()"
          [alertCount]="alertCount()"
          (toggleSidebar)="toggleSidebar()"
        ></app-header>

        <main class="content-area">
          @if (loadingService.isLoading()) {
            <app-loading-spinner [overlay]="true"></app-loading-spinner>
          }
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [
    `
      .admin-layout {
        display: flex;
        min-height: 100vh;
        background: var(--bg-main);
      }

      .main-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        margin-left: 280px;
        transition: margin-left 0.3s ease;
      }

      .sidebar-collapsed .main-content {
        margin-left: 72px;
      }

      .content-area {
        flex: 1;
        position: relative;
        padding: 1.5rem;
        overflow-y: auto;
      }

      @media (max-width: 1024px) {
        .main-content {
          margin-left: 0;
        }
      }
    `,
  ],
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  loadingService = inject(LoadingService);

  sidebarCollapsed = signal(false);
  notificationCount = signal(0);
  alertCount = signal(0);

  private pollingInterval?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    // Polling pour les notifications/alertes système
    this.pollingInterval = setInterval(() => {
      this.checkSystemAlerts();
    }, 60000); // Vérifier chaque minute
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
  }

  private checkSystemAlerts(): void {
    // Logique pour récupérer les alertes système
    // À implémenter avec le service approprié
  }
}
