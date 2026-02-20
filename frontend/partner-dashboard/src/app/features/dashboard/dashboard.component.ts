// src/app/features/dashboard/dashboard.component.ts
import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { DashboardService } from './services/dashboard.service';
import { StatsCardComponent } from './components/stats-card/stats-card.component';
import { OrdersChartComponent } from './components/orders-chart/orders-chart.component';
import { AuthService } from '@core/services/auth.service';
import { PartnerService } from '@core/services/partner.service';
import { WebSocketService } from '@core/services/websocket.service';
import { GenericDialogComponent } from '@shared/components/generic-dialog/generic-dialog.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatDialogModule,
    TranslateModule,
    StatsCardComponent,
    OrdersChartComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private partnerService = inject(PartnerService);
  private wsService = inject(WebSocketService);
  private dialog = inject(MatDialog);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private wsSub: Subscription | null = null;

  // Partner info
  partnerName = signal('Partenaire');
  partnerStatus = signal<string>('');
  acceptingOrders = signal(false);
  loading = signal(true);

  // Dashboard stats
  stats = signal({
    ordersToday: 0,
    ordersTrend: 0,
    revenueToday: 0,
    revenueTrend: 0,
    averageRating: 0,
    avgPrepTime: 0,
    ordersChart: [] as number[],
    recentOrders: [] as any[],
    popularProducts: [] as any[],
  });

  // Computed signals
  isPending = computed(() => {
    const status = this.partnerStatus().toLowerCase();
    return status === 'pending' || status === 'in_review';
  });
  isDocumentsMissing = computed(() => this.partnerStatus().toLowerCase() === 'documents_missing');
  isActive = computed(() => this.partnerStatus().toLowerCase() === 'active');
  isApproved = computed(() => this.partnerStatus().toLowerCase() === 'approved');
  isRejected = computed(() => this.partnerStatus().toLowerCase() === 'rejected');
  isSuspended = computed(() => this.partnerStatus().toLowerCase() === 'suspended');
  isDeactivated = computed(() => {
    const s = this.partnerStatus().toLowerCase();
    return s === 'inactive' || s === 'deactivated';
  });
  
  // Store admin message for documents_missing
  adminMessage = signal<string>('');
  hasRecentOrders = computed(() => this.stats().recentOrders.length > 0);
  hasPopularProducts = computed(() => this.stats().popularProducts.length > 0);

  ngOnInit(): void {
    this.loadPartnerInfo();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
  }

  private connectWebSocket(): void {
    this.wsService.connect();

    this.wsSub = this.wsService.onPartnerNotification.subscribe((notif) => {
      const action = notif.data?.['action'];
      const newStatus = notif.data?.['newStatus'];

      if (action === 'PARTNER_APPROVED' && newStatus === 'ACTIVE') {
        this.handlePartnerApproved(notif.message);
      } else if (action === 'PARTNER_ACTIVATED' && newStatus === 'ACTIVE') {
        this.handlePartnerActivated(notif.message);
      } else if (action === 'PARTNER_REJECTED' && newStatus === 'REJECTED') {
        const reason = notif.data?.['reason'] || '';
        this.handlePartnerRejected(notif.message, reason);
      } else if (action === 'PARTNER_SUSPENDED' && newStatus === 'SUSPENDED') {
        this.handlePartnerSuspended(notif.message);
      } else if (action === 'PARTNER_DEACTIVATED' && (newStatus === 'INACTIVE' || newStatus === 'DEACTIVATED')) {
        this.handlePartnerDeactivated(notif.message);
      } else if (action === 'PARTNER_INFO_REQUESTED' && newStatus === 'DOCUMENTS_MISSING') {
        const adminMsg = notif.data?.['adminMessage'] || '';
        this.handlePartnerInfoRequested(notif.message, adminMsg);
      }
    });
  }

  private handlePartnerApproved(message: string): void {
    this.partnerStatus.set('ACTIVE');
    this.dialog.open(GenericDialogComponent, {
      disableClose: true,
      width: '480px',
      data: {
        type: 'success',
        title: this.translate.instant('DASHBOARD.notifications.approved.title'),
        message: message || this.translate.instant('DASHBOARD.notifications.approved.message'),
        confirmText: this.translate.instant('DASHBOARD.notifications.approved.confirmText'),
        showCancel: false,
      },
    });

    // Reload dashboard data
    this.loadPartnerInfo();
  }

  private handlePartnerRejected(message: string, reason: string): void {
    this.partnerStatus.set('REJECTED');
    this.dialog.open(GenericDialogComponent, {
      disableClose: true,
      width: '480px',
      data: {
        type: 'danger',
        title: this.translate.instant('DASHBOARD.notifications.rejected.title'),
        message: message || this.translate.instant('DASHBOARD.notifications.rejected.message'),
        detail: reason ? this.translate.instant('DASHBOARD.notifications.rejected.reason') + ' ' + reason : undefined,
        confirmText: this.translate.instant('DASHBOARD.notifications.rejected.confirmText'),
        showCancel: false,
      },
    });
  }

  private handlePartnerSuspended(message: string): void {
    this.partnerStatus.set('SUSPENDED');
    this.dialog.open(GenericDialogComponent, {
      disableClose: true,
      width: '480px',
      data: {
        type: 'warning',
        title: this.translate.instant('DASHBOARD.notifications.suspended.title'),
        message: message || this.translate.instant('DASHBOARD.notifications.suspended.message'),
        confirmText: this.translate.instant('DASHBOARD.notifications.suspended.confirmText'),
        showCancel: false,
      },
    });
  }

  private handlePartnerDeactivated(message: string): void {
    this.partnerStatus.set('INACTIVE');
    this.dialog.open(GenericDialogComponent, {
      disableClose: true,
      width: '480px',
      data: {
        type: 'warning',
        title: this.translate.instant('DASHBOARD.notifications.deactivated.title'),
        message: message || this.translate.instant('DASHBOARD.notifications.deactivated.message'),
        confirmText: this.translate.instant('DASHBOARD.notifications.deactivated.confirmText'),
        showCancel: false,
      },
    });
  }

  private handlePartnerActivated(message: string): void {
    this.partnerStatus.set('ACTIVE');
    this.dialog.open(GenericDialogComponent, {
      disableClose: true,
      width: '480px',
      data: {
        type: 'success',
        title: this.translate.instant('DASHBOARD.notifications.activated.title'),
        message: message || this.translate.instant('DASHBOARD.notifications.activated.message'),
        confirmText: this.translate.instant('DASHBOARD.notifications.activated.confirmText'),
        showCancel: false,
      },
    });
    this.loadPartnerInfo();
  }

  private handlePartnerInfoRequested(message: string, adminMessage: string): void {
    this.partnerStatus.set('DOCUMENTS_MISSING');
    this.adminMessage.set(adminMessage);
    this.dialog.open(GenericDialogComponent, {
      disableClose: true,
      width: '520px',
      data: {
        type: 'info',
        title: this.translate.instant('DASHBOARD.notifications.documentsMissing.title'),
        message: message || this.translate.instant('DASHBOARD.notifications.documentsMissing.message'),
        detail: adminMessage || undefined,
        confirmText: this.translate.instant('DASHBOARD.notifications.documentsMissing.confirmText'),
        showCancel: false,
      },
    });
    this.loadPartnerInfo();
  }

  private loadPartnerInfo(): void {
    this.loading.set(true);
    const user = this.authService.currentUser();

    if (user) {
      this.partnerName.set(user.firstName || user.partnerName || 'Partenaire');
    }

    const partnerId = this.authService.getPartnerId();
    if (!partnerId) {
      this.loading.set(false);
      return;
    }

    this.partnerService.getPartner(partnerId).subscribe({
      next: (partner: any) => {
        this.partnerStatus.set(partner.status || 'PENDING');
        this.partnerName.set(partner.brandName || partner.businessName || this.partnerName());
        this.acceptingOrders.set(partner.isActive || false);
        
        // Store admin message if status is documents_missing
        if (partner.status === 'DOCUMENTS_MISSING' && partner.adminMessage) {
          this.adminMessage.set(partner.adminMessage);
        }

        // Only load dashboard stats if partner is active/approved
        if (this.isActive() || this.isApproved()) {
          this.loadDashboard();
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.partnerStatus.set('PENDING');
        this.loading.set(false);
      },
    });
  }

  private loadDashboard(): void {
    this.dashboardService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggleAcceptingOrders(): void {
    const newState = !this.acceptingOrders();
    this.acceptingOrders.set(newState);
  }

  navigateToCompleteProfile(): void {
    // Navigate to complete-profile page to update partner information
    this.router.navigate(['/auth/complete-profile']);
  }
}
