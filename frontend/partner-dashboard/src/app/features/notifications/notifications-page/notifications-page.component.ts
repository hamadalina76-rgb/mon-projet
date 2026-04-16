import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  inject,
  signal,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs/operators';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { PartnerNotification } from '@core/services/websocket.service';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    MatPaginatorModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './notifications-page.component.html',
  styleUrls: ['./notifications-page.component.scss'],
})
export class NotificationsPageComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator?: MatPaginator;

  private auth = inject(AuthService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private destroyRef = inject(DestroyRef);

  rows = signal<PartnerNotification[]>([]);
  totalElements = signal(0);
  pageIndex = signal(0);
  readonly pageSize = signal(20);
  loading = signal(true);
  loadError = signal(false);
  unreadCount = signal(0);

  ngOnInit(): void {
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyPaginatorIntl());

    this.loadUnreadCount();
    this.loadPage();
  }

  ngAfterViewInit(): void {
    this.applyPaginatorIntl();
  }

  private applyPaginatorIntl(): void {
    const p = this.paginator;
    if (!p) return;
    const t = (k: string, params?: object) => this.translate.instant(k, params);
    p._intl.itemsPerPageLabel = t('ORDERS.PAGINATOR_PER_PAGE');
    p._intl.nextPageLabel = t('ORDERS.PAGINATOR_NEXT');
    p._intl.previousPageLabel = t('ORDERS.PAGINATOR_PREV');
    p._intl.firstPageLabel = t('ORDERS.PAGINATOR_FIRST');
    p._intl.lastPageLabel = t('ORDERS.PAGINATOR_LAST');
    p._intl.getRangeLabel = (page, pageSize, length) => {
      if (length === 0) return t('ORDERS.PAGINATOR_RANGE_ZERO');
      const start = page * pageSize + 1;
      const end = Math.min((page + 1) * pageSize, length);
      return t('ORDERS.PAGINATOR_RANGE', { start, end, length });
    };
    p._intl.changes.next();
  }

  private loadUnreadCount(): void {
    const user = this.auth.currentUser();
    if (!user?.id) return;
    this.notificationService.getUnreadCount(Number(user.id)).subscribe({
      next: (response: { count?: number }) => {
        if (response.count !== undefined) this.unreadCount.set(response.count);
      },
      error: () => {},
    });
  }

  loadPage(): void {
    const user = this.auth.currentUser();
    if (!user?.id) {
      this.loading.set(false);
      this.rows.set([]);
      return;
    }
    this.loading.set(true);
    this.loadError.set(false);
    this.notificationService
      .getNotifications(Number(user.id), this.pageIndex(), this.pageSize())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response: {
          content?: PartnerNotification[];
          totalElements?: number;
          number?: number;
          size?: number;
        }) => {
          this.rows.set(response.content ?? []);
          this.totalElements.set(response.totalElements ?? 0);
          if (response.number != null) this.pageIndex.set(response.number);
          setTimeout(() => this.applyPaginatorIntl(), 0);
        },
        error: () => {
          this.loadError.set(true);
          this.rows.set([]);
        },
      });
  }

  onPage(evt: PageEvent): void {
    this.pageIndex.set(evt.pageIndex);
    this.pageSize.set(evt.pageSize);
    this.loadPage();
  }

  markAllRead(): void {
    const user = this.auth.currentUser();
    if (!user?.id) return;
    this.notificationService.markAllAsRead(Number(user.id)).subscribe({
      next: () => {
        this.unreadCount.set(0);
        this.rows.update((list) => list.map((n) => ({ ...n, isRead: true })));
      },
      error: () => {},
    });
  }

  notifIcon(notif: PartnerNotification): string {
    const action = notif.data?.['action'];
    if (action === 'ADMIN_CONTACT') return 'mark_unread_chat_alt';
    if (notif.type === 'ORDER') return 'shopping_bag';
    if (notif.type === 'PARTNER' || action === 'PARTNER_APPROVED') return 'check_circle';
    return 'notifications';
  }

  onNotifClick(notif: PartnerNotification): void {
    if (!notif.isRead) {
      this.notificationService.markAsRead(notif.id).subscribe({
        next: () => {
          this.rows.update((list) =>
            list.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n))
          );
          this.unreadCount.update((c) => Math.max(0, c - 1));
        },
        error: () => {},
      });
    }

    if (notif.data?.['action'] === 'PARTNER_APPROVED') {
      this.router.navigate(['/dashboard']);
    } else if (
      notif.data?.['action'] === 'PRODUCT_APPROVED' ||
      notif.data?.['action'] === 'PRODUCT_REJECTED'
    ) {
      const productId = notif.data?.['productId'];
      if (productId != null) {
        this.router.navigate(['/menu/products', productId, 'edit']);
      }
    } else if (notif.data?.['action'] === 'ADMIN_CONTACT') {
      const rawId = notif.data?.['orderId'] ?? notif.data?.['id'];
      if (rawId != null && String(rawId).trim().length > 0) {
        this.router.navigate(['/orders', String(rawId)]);
      }
    }
  }
}
