// src/app/layout/main-layout/main-layout.component.ts - Angular 19
import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, NavigationEnd, Router } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { SidebarComponent } from '@shared/components/sidebar/sidebar.component';
import { HeaderComponent } from '@shared/components/header/header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { WebSocketService } from '@core/services/websocket.service';
import { NotificationService } from '@core/services/notification.service';
import { LoadingService } from '@core/services/loading.service';
import { AuthService } from '@core/services/auth.service';
import { PartnerService } from '@core/services/partner.service';
import { Subject, takeUntil, filter } from 'rxjs';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
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
  private authService = inject(AuthService);
  private partnerService = inject(PartnerService);
  private breakpointObserver = inject(BreakpointObserver);
  private router = inject(Router);
  loadingService = inject(LoadingService);

  sidebarCollapsed = signal(false);
  sidebarMobileOpen = signal(false);
  isMobile = signal(false);
  partnerStatus = signal<string>('');
  isSuspended = computed(() => this.partnerStatus().toLowerCase() === 'suspended');
  isDeactivated = computed(() => {
    const s = this.partnerStatus().toLowerCase();
    return s === 'inactive' || s === 'deactivated';
  });
  showStatusBanner = computed(() => this.isSuspended() || this.isDeactivated());

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadPartnerStatus();
    this.initWebSocket();
    this.requestNotificationPermission();
    this.initResponsive();
    this.initRouterCloseSidebar();
  }

  private initResponsive(): void {
    this.breakpointObserver
      .observe('(max-width: 767px)')
      .pipe(takeUntil(this.destroy$))
      .subscribe((state) => {
        this.isMobile.set(state.matches);
        if (!state.matches) this.sidebarMobileOpen.set(false);
      });
  }

  private initRouterCloseSidebar(): void {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        if (this.isMobile()) this.sidebarMobileOpen.set(false);
      });
  }

  private loadPartnerStatus(): void {
    const partnerId = this.authService.getPartnerId();
    if (partnerId) {
      this.partnerService.getPartner(partnerId).subscribe({
        next: (partner: any) => this.partnerStatus.set(partner?.status || ''),
        error: () => this.partnerStatus.set(''),
      });
    }
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

    this.wsService.onPartnerNotification
      .pipe(takeUntil(this.destroy$))
      .subscribe((notif: any) => {
        const newStatus = notif?.data?.['newStatus'];
        if (newStatus) this.partnerStatus.set(newStatus);
      });
  }

  private async requestNotificationPermission(): Promise<void> {
    await this.notificationService.requestBrowserPermission();
  }

  onSidebarToggle(): void {
    if (this.isMobile()) {
      this.sidebarMobileOpen.update((v) => !v);
    } else {
      this.sidebarCollapsed.update((v) => !v);
    }
  }

  onSidebarCollapsedChange(collapsed: boolean): void {
    if (this.isMobile()) {
      this.sidebarMobileOpen.set(!collapsed);
    } else {
      this.sidebarCollapsed.set(collapsed);
    }
  }

  closeSidebarMobile(): void {
    this.sidebarMobileOpen.set(false);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.wsService.disconnect();
  }
}
