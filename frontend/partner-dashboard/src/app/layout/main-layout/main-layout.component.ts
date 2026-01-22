// src/app/layout/main-layout/main-layout.component.ts - Angular 19
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { HeaderComponent } from '@shared/components/header/header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { WebSocketService } from '@core/services/websocket.service';
import { NotificationService } from '@core/services/notification.service';
import { LoadingService } from '@core/services/loading.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarComponent,
    HeaderComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss'],
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  private wsService = inject(WebSocketService);
  private notificationService = inject(NotificationService);
  loadingService = inject(LoadingService);

  // Angular 19 Signals
  sidebarCollapsed = signal(false);
  
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.initWebSocket();
    this.requestNotificationPermission();
  }

  private initWebSocket(): void {
    this.wsService.connect();

    this.wsService
      .onNewOrder()
      .pipe(takeUntil(this.destroy$))
      .subscribe((order: any) => {
        this.notificationService.newOrderAlert(order.orderNumber);
        this.notificationService.showBrowserNotification(
          'Nouvelle commande!',
          `Commande #${order.orderNumber} reçue`
        );
      });
  }

  private async requestNotificationPermission(): Promise<void> {
    await this.notificationService.requestBrowserPermission();
  }

  onSidebarToggle(collapsed: boolean): void {
    this.sidebarCollapsed.set(collapsed);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.wsService.disconnect();
  }
}
