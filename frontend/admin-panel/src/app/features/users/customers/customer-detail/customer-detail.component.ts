import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { CustomersService, Customer } from '../services/customers.service';
import { getMediaUrl } from '@core/utils/media-url.util';
import { ActivityLogService } from '@core/services/activity-log.service';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    TranslateModule,
  ],
  templateUrl: './customer-detail.component.html',
  styleUrls: ['./customer-detail.component.scss'],
})
export class CustomerDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private customersService = inject(CustomersService);
  private activityLogService = inject(ActivityLogService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);

  customer = signal<Customer | null>(null);
  loading = signal(true);
  avatarLoadError = signal(false);
  orders = signal<unknown[]>([]);
  ordersTotal = signal(0);
  reviews = signal<unknown[]>([]);
  reviewsTotal = signal(0);
  logs = signal<unknown[]>([]);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCustomer(id);
    }
  }

  loadCustomer(id: string): void {
    this.loading.set(true);
    this.customersService.getCustomer(id).subscribe({
      next: (c) => {
        this.customer.set(c);
        this.avatarLoadError.set(false);
        this.loading.set(false);
        this.loadOrders(Number(id));
        this.loadReviews(Number(id));
        this.loadLogs(id);
      },
      error: () => this.loading.set(false),
    });
  }

  loadOrders(customerId: number): void {
    this.customersService.getCustomerOrders(customerId, 0, 20).subscribe({
      next: (res) => {
        this.orders.set(res.content ?? []);
        this.ordersTotal.set(res.totalElements ?? 0);
      },
      error: () => this.orders.set([]),
    });
  }

  loadReviews(customerId: number): void {
    this.customersService.getCustomerReviews(customerId, 0, 20).subscribe({
      next: (res) => {
        this.reviews.set(res.content ?? []);
        this.reviewsTotal.set(res.totalElements ?? 0);
      },
      error: () => this.reviews.set([]),
    });
  }

  loadLogs(customerId: string): void {
    this.activityLogService.getActivityLogsByResource('customers', customerId, 0, 25).subscribe({
      next: (res) => {
        this.logs.set(res.content ?? []);
      },
      error: () => this.logs.set([]),
    });
  }

  getFullName(c: Customer): string {
    const first = c.firstName ?? '';
    const last = c.lastName ?? '';
    return [first, last].filter(Boolean).join(' ') || c.email || '-';
  }

  getStatusLabel(status: string): string {
    const key = `users.customers.status.${(status || '').toLowerCase()}`;
    const t = this.translate.instant(key);
    return t !== key ? t : status;
  }

  getInitials(c: Customer): string {
    const first = (c.firstName ?? '').charAt(0);
    const last = (c.lastName ?? '').charAt(0);
    return (first + last).toUpperCase() || (c.email ?? '').charAt(0).toUpperCase() || '?';
  }

  getCustomerIdFormatted(c: Customer): string {
    const id = String(c.id ?? c.userId ?? '').padStart(5, '0');
    return `SL-${id}-${this.getInitials(c)}`;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-TN', { style: 'currency', currency: 'TND', minimumFractionDigits: 2 }).format(amount);
  }

  getAvgOrder(c: Customer): number {
    const orders = c.totalOrders ?? 0;
    const spent = Number(c.totalSpent ?? 0);
    return orders > 0 ? spent / orders : 0;
  }

  getMembershipYears(c: Customer): number {
    const created = c.createdAt ? new Date(c.createdAt) : new Date();
    return Math.max(0, Math.floor((Date.now() - created.getTime()) / (365.25 * 24 * 60 * 60 * 1000)));
  }

  getProfilePictureUrl(url: string | null | undefined): string | null {
    return getMediaUrl(url);
  }

  formatAddress(addr: { street?: string; city?: string; building?: string; postalCode?: string; country?: string }): string {
    const parts = [addr.street, addr.building, addr.city, addr.postalCode, addr.country].filter(Boolean);
    return parts.join(', ') || '-';
  }

  block(): void {
    const c = this.customer();
    if (!c) return;
    const name = this.getFullName(c);
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('users.customers.actions.blockTitle'),
        message: this.translate.instant('users.customers.actions.blockMessage', { name }),
        confirmLabel: this.translate.instant('common.block'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'warning',
        icon: 'block',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((ok) => {
      if (ok) {
        this.customersService.blockCustomer(String(c.id)).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.customers.actions.blockSuccess'));
            this.loadCustomer(String(c.id));
          },
        });
      }
    });
  }

  unblock(): void {
    const c = this.customer();
    if (!c) return;
    const name = this.getFullName(c);
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('users.customers.actions.unblockTitle'),
        message: this.translate.instant('users.customers.actions.unblockMessage', { name }),
        confirmLabel: this.translate.instant('common.unblock'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'info',
        icon: 'check_circle',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((ok) => {
      if (ok) {
        this.customersService.unblockCustomer(String(c.id)).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.customers.actions.unblockSuccess'));
            this.loadCustomer(String(c.id));
          },
        });
      }
    });
  }

  resetPassword(): void {
    const c = this.customer();
    if (!c) return;
    this.customersService.resetPassword(String(c.id)).subscribe({
      next: () => this.toastr.success(this.translate.instant('users.customers.actions.resetPasswordSuccess')),
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }

  sendNotification(): void {
    const c = this.customer();
    if (!c) return;
    const subject = this.translate.instant('users.customers.notificationSubject');
    const body = '';
    this.customersService.sendNotification(String(c.id), subject, body).subscribe({
      next: () => this.toastr.success(this.translate.instant('users.customers.actions.sendNotificationSuccess')),
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }

  deleteCustomer(): void {
    const c = this.customer();
    if (!c) return;
    const name = this.getFullName(c);
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: this.translate.instant('users.customers.actions.deleteTitle'),
        message: this.translate.instant('users.customers.actions.deleteMessage', { name }),
        confirmLabel: this.translate.instant('common.delete'),
        cancelLabel: this.translate.instant('common.cancel'),
        type: 'danger',
        icon: 'delete',
      } as ConfirmationDialogData,
    });
    dialogRef.afterClosed().subscribe((ok) => {
      if (ok) {
        this.customersService.deleteCustomer(String(c.id)).subscribe({
          next: () => {
            this.toastr.success(this.translate.instant('users.customers.actions.deleteSuccess'));
            this.routerNavigate();
          },
        });
      }
    });
  }

  private routerNavigate(): void {
    this.router.navigate(['/users/customers']);
  }
}
