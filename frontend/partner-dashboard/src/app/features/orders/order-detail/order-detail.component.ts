// src/app/features/orders/order-detail/order-detail.component.ts
import { Component, OnInit, AfterViewInit, inject, signal, computed, ViewChild, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { finalize } from 'rxjs/operators';
import { OrdersService, OrderHistoryParams } from '../services/orders.service';
import { OrdersStoreService } from '../services/orders-store.service';
import { OrderStatusBadgeComponent } from '../components/order-status-badge/order-status-badge.component';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import {
  AcceptOrderDialogComponent,
  AcceptOrderDialogResult,
} from '../dialogs/accept-order-dialog.component';
import {
  RejectOrderDialogComponent,
  RejectOrderDialogResult,
} from '../dialogs/reject-order-dialog.component';
import {
  hasProductPrepOnItems,
  suggestedPrepMinutesFromItems,
  Order,
} from '../models/order.model';
import { TimerComponent } from '../components/prep-timer/timer.component';
import { PrepTimerSessionService } from '../services/prep-timer-session.service';
import {
  mergePrepTimerContext,
  buildPrepTimerContextAfterAccept,
} from '../utils/prep-timer.utils';
import { KitchenPrintService } from '../services/kitchen-print.service';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    RouterLink,
    MatDialogModule,
    MatSnackBarModule,
    MatPaginatorModule,
    OrderStatusBadgeComponent,
    TimeAgoPipe,
    LoadingSpinnerComponent,
    TimerComponent,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit, AfterViewInit {
  @ViewChild('historyPaginator') historyPaginator?: MatPaginator;

  private route         = inject(ActivatedRoute);
  private ordersService = inject(OrdersService);
  private ordersStore   = inject(OrdersStoreService);
  private dialog        = inject(MatDialog);
  private snackBar      = inject(MatSnackBar);
  private translate     = inject(TranslateService);
  private destroyRef    = inject(DestroyRef);
  private prepTimerSession = inject(PrepTimerSessionService);
  private kitchenPrint     = inject(KitchenPrintService);

  order   = signal<any>(null);
  /** Bandeau minuteur en surbrillance quand l’échéance est dépassée. */
  detailPrepOverdue = signal(false);
  loading = signal(false);
  actionLoading = signal(false);

  /** Lignes d'historique (API filtrée + pagination) */
  historyRows           = signal<any[]>([]);
  historyLoading        = signal(false);
  historyTotalElements  = signal(0);
  historyPageIndex      = signal(0);
  historyPageSize       = signal(10);
  historyFilterStatus = signal('');
  historyFilterActor  = signal('');
  historyFilterFrom   = signal('');
  historyFilterTo     = signal('');

  readonly historyStatusFilterOptions: { value: string; labelKey: string }[] = [
    { value: '', labelKey: 'ORDERS.HISTORY.FILTER_ALL_STATUS' },
    { value: 'PENDING', labelKey: 'ORDERS.HISTORY.STATUS_PENDING' },
    { value: 'CONFIRMED', labelKey: 'ORDERS.HISTORY.STATUS_CONFIRMED' },
    { value: 'PREPARING', labelKey: 'ORDERS.HISTORY.STATUS_PREPARING' },
    { value: 'READY_FOR_PICKUP', labelKey: 'ORDERS.HISTORY.STATUS_READY' },
    { value: 'PICKED_UP', labelKey: 'ORDERS.HISTORY.STATUS_PICKED_UP' },
    { value: 'IN_DELIVERY', labelKey: 'ORDERS.HISTORY.STATUS_IN_DELIVERY' },
    { value: 'DELIVERED', labelKey: 'ORDERS.HISTORY.STATUS_DELIVERED' },
    { value: 'CANCELLED', labelKey: 'ORDERS.HISTORY.STATUS_CANCELLED' },
  ];

  readonly historyActorFilterOptions: { value: string; labelKey: string }[] = [
    { value: '', labelKey: 'ORDERS.HISTORY.FILTER_ALL_ACTORS' },
    { value: 'SYSTEM', labelKey: 'ORDERS.HISTORY.ACTOR_SYSTEM' },
    { value: 'PARTNER', labelKey: 'ORDERS.HISTORY.ACTOR_PARTNER' },
    { value: 'CUSTOMER', labelKey: 'ORDERS.HISTORY.ACTOR_CUSTOMER' },
    { value: 'COURIER', labelKey: 'ORDERS.HISTORY.ACTOR_COURIER' },
    { value: 'ADMIN', labelKey: 'ORDERS.HISTORY.ACTOR_ADMIN' },
  ];

  isPending   = computed(() => this.order()?.status === 'PENDING');
  isConfirmed = computed(() => this.order()?.status === 'CONFIRMED');
  isPreparing = computed(() => this.order()?.status === 'PREPARING');
  isReady     = computed(() => this.order()?.status === 'READY');
  isCancelled = computed(() => this.order()?.status === 'CANCELLED');

  prepTimerDetailContext = computed(() => {
    const o = this.order() as Order | null;
    if (!o) return null;
    return mergePrepTimerContext(this.prepTimerSession.load(o.id), o);
  });

  totalItems = computed(() =>
    (this.order()?.items ?? []).reduce((s: number, it: any) => s + (it.quantity ?? 1), 0)
  );

  /** Lignes affichées (depuis l'API historique, avec filtres) */
  timelineEntries = computed(() => {
    const rows = this.historyRows();
    if (!rows.length) return [];
    return rows.map((h: any) => ({
      status:               h.status as string,
      timestamp:            h.timestamp,
      eventKey:             `ORDERS.HISTORY.EVENT_${h.status as string}`,
      description:          h.description ?? null,
      notes:                h.notes ?? null,
      actorKey:             this.actorTypeKey(h.updatedBy, h.actorType),
      estimatedPrepMinutes: h.estimatedPrepMinutes ?? null,
    }));
  });

  historyHasActiveFilters = computed(() => {
    return !!(
      this.historyFilterStatus() ||
      this.historyFilterActor() ||
      this.historyFilterFrom() ||
      this.historyFilterTo()
    );
  });

  ngOnInit(): void {
    this.translate.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyHistoryPaginatorIntl());

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const cached = this.ordersStore.getOrder(id);
      if (cached) this.order.set(cached);
      this.fetchOrder(id);
    }
  }

  ngAfterViewInit(): void {
    this.applyHistoryPaginatorIntl();
  }

  private applyHistoryPaginatorIntl(): void {
    const p = this.historyPaginator;
    if (!p) return;
    const t = (k: string, params?: object) => this.translate.instant(k, params);
    p._intl.itemsPerPageLabel = t('ORDERS.PAGINATOR_PER_PAGE');
    p._intl.nextPageLabel     = t('ORDERS.PAGINATOR_NEXT');
    p._intl.previousPageLabel = t('ORDERS.PAGINATOR_PREV');
    p._intl.firstPageLabel   = t('ORDERS.PAGINATOR_FIRST');
    p._intl.lastPageLabel    = t('ORDERS.PAGINATOR_LAST');
    p._intl.getRangeLabel = (page, pageSize, length) => {
      if (length === 0) return t('ORDERS.PAGINATOR_RANGE_ZERO');
      const start = page * pageSize + 1;
      const end   = Math.min((page + 1) * pageSize, length);
      return t('ORDERS.PAGINATOR_RANGE', { start, end, length });
    };
    p._intl.changes.next();
  }

  fetchOrder(id: string): void {
    if (!this.order()) this.loading.set(true);
    this.detailPrepOverdue.set(false);
    this.ordersService.getOrder(id).subscribe({
      next: (o) => {
        this.clearPrepTimerIfTerminal(o);
        this.order.set(o);
        this.ordersStore.updateOrder(o);
        this.loading.set(false);
        this.historyPageIndex.set(0);
        this.loadHistory();
      },
      error: () => this.loading.set(false),
    });
  }

  loadHistory(): void {
    const id = this.order()?.id;
    if (!id) return;
    this.historyLoading.set(true);
    const params: OrderHistoryParams = {
      page: this.historyPageIndex(),
      size: this.historyPageSize(),
    };
    const st = this.historyFilterStatus();
    if (st) params.status = st;
    const act = this.historyFilterActor();
    if (act) params.actorType = act;
    const from = this.datetimeLocalToHistoryParam(this.historyFilterFrom());
    const to = this.datetimeLocalToHistoryParam(this.historyFilterTo());
    if (from) params.from = from;
    if (to) params.to = to;

    this.ordersService
      .getOrderHistory(id, params)
      .pipe(finalize(() => this.historyLoading.set(false)))
      .subscribe((page) => {
        this.historyRows.set(page.content ?? []);
        this.historyTotalElements.set(page.totalElements ?? 0);
        this.historyPageIndex.set(page.number ?? 0);
        this.historyPageSize.set(page.size ?? this.historyPageSize());
        setTimeout(() => this.applyHistoryPaginatorIntl(), 0);
      });
  }

  onHistoryPage(evt: PageEvent): void {
    this.historyPageIndex.set(evt.pageIndex);
    this.historyPageSize.set(evt.pageSize);
    this.loadHistory();
  }

  onHistoryFiltersChange(): void {
    this.historyPageIndex.set(0);
    this.loadHistory();
  }

  resetHistoryFilters(): void {
    this.historyFilterStatus.set('');
    this.historyFilterActor.set('');
    this.historyFilterFrom.set('');
    this.historyFilterTo.set('');
    this.historyPageIndex.set(0);
    this.loadHistory();
  }

  /** Recharge l’historique depuis la page 1 après une action métier */
  private refreshHistoryAfterAction(): void {
    this.historyPageIndex.set(0);
    this.loadHistory();
  }

  private datetimeLocalToHistoryParam(v: string): string | undefined {
    const t = v?.trim();
    if (!t) return undefined;
    return t.length === 16 ? `${t}:00` : t;
  }

  // ---------------------------------------------------------------------------
  // Actions
  // ---------------------------------------------------------------------------

  openAcceptDialog(): void {
    const o = this.order();
    if (!o) return;

    const items = o.items ?? [];
    const fromOrder = o.suggestedPreparationMinutes;
    const fromItems = suggestedPrepMinutesFromItems(items);
    const suggested =
      fromOrder != null && fromOrder > 0 ? fromOrder : fromItems;
    const showHint =
      hasProductPrepOnItems(items) || (fromOrder != null && fromOrder > 0);

    this.dialog.open(AcceptOrderDialogComponent, {
      data: {
        orderNumber: o.orderNumber,
        orderId: o.id,
        suggestedFromProductsMinutes: showHint ? suggested : undefined,
        showProductPrepHint: showHint,
      },
      panelClass: 'sl-dialog-panel',
      maxWidth: '90vw',
    }).afterClosed().subscribe((result: AcceptOrderDialogResult | null) => {
      if (!result?.accepted) return;
      this.doAccept(o.id, result.prepTime);
    });
  }

  openRejectDialog(): void {
    const o = this.order();
    if (!o) return;

    this.dialog.open(RejectOrderDialogComponent, {
      data: { orderNumber: o.orderNumber, orderId: o.id },
      panelClass: 'sl-dialog-panel',
      maxWidth: '90vw',
    }).afterClosed().subscribe((result: RejectOrderDialogResult | null) => {
      if (!result?.rejected) return;
      this.doReject(o.id, result.reason);
    });
  }

  private doAccept(id: string, prepTime: number): void {
    const prevStatus = this.order()?.status;
    this.actionLoading.set(true);
    // Optimistic update
    this.order.update(o => ({ ...o, status: 'CONFIRMED' }));
    this.ordersStore.updateOrder({ id, status: 'CONFIRMED' });

    this.ordersService.confirmOrder(id, prepTime).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.ordersStore.updateOrder(updated);
        const ctx = buildPrepTimerContextAfterAccept(updated as Order, prepTime);
        if (ctx) this.prepTimerSession.save(id, ctx);
        this.kitchenPrint.printKitchenTicket(String(id));
        this.actionLoading.set(false);
        this.refreshHistoryAfterAction();
        this.snackBar.open(
          this.translate.instant('ORDERS.TOAST.ACCEPTED', { minutes: prepTime }),
          undefined,
          { duration: 3000, panelClass: ['sl-snack-success'] },
        );
      },
      error: () => {
        this.order.update(o => ({ ...o, status: prevStatus }));
        this.ordersStore.rollbackStatus(id, prevStatus);
        this.actionLoading.set(false);
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.ACCEPT_ERROR'), 'OK', { duration: 4000 });
      },
    });
  }

  private doReject(id: string, reason: string): void {
    const prevStatus = this.order()?.status;
    this.actionLoading.set(true);
    // Optimistic update
    this.order.update(o => ({ ...o, status: 'CANCELLED', cancelReason: reason }));
    this.ordersStore.updateOrder({ id, status: 'CANCELLED' });

    this.ordersService.cancelOrder(id, reason).subscribe({
      next: (updated) => {
        this.prepTimerSession.clear(id);
        this.detailPrepOverdue.set(false);
        this.order.set(updated);
        this.ordersStore.updateOrder(updated);
        this.actionLoading.set(false);
        this.refreshHistoryAfterAction();
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.REJECTED'), undefined, {
          duration: 3000, panelClass: ['sl-snack-error'],
        });
      },
      error: () => {
        this.order.update(o => ({ ...o, status: prevStatus, cancelReason: undefined }));
        this.ordersStore.rollbackStatus(id, prevStatus);
        this.actionLoading.set(false);
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.REJECT_ERROR'), 'OK', { duration: 4000 });
      },
    });
  }

  startPreparing(): void {
    const o = this.order();
    if (!o) return;
    const prevStatus = o.status;
    this.actionLoading.set(true);
    this.order.update(x => ({ ...x, status: 'PREPARING' }));
    this.ordersStore.updateOrder({ id: o.id, status: 'PREPARING' });

    this.ordersService.startPreparing(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.ordersStore.updateOrder(updated);
        this.actionLoading.set(false);
        this.refreshHistoryAfterAction();
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.PREPARING'), undefined, {
          duration: 2500, panelClass: ['sl-snack-info'],
        });
      },
      error: () => {
        this.order.update(x => ({ ...x, status: prevStatus }));
        this.ordersStore.rollbackStatus(o.id, prevStatus);
        this.actionLoading.set(false);
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.PREP_ERROR'), 'OK', { duration: 3000 });
      },
    });
  }

  markReady(): void {
    const o = this.order();
    if (!o) return;
    const prevStatus = o.status;
    this.actionLoading.set(true);
    this.order.update(x => ({ ...x, status: 'READY' }));
    this.ordersStore.updateOrder({ id: o.id, status: 'READY' });

    this.ordersService.markReady(o.id).subscribe({
      next: (updated) => {
        this.prepTimerSession.clear(o.id);
        this.detailPrepOverdue.set(false);
        this.order.set(updated);
        this.ordersStore.updateOrder(updated);
        this.actionLoading.set(false);
        this.refreshHistoryAfterAction();
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.READY_OK'), undefined, {
          duration: 2500, panelClass: ['sl-snack-success'],
        });
      },
      error: () => {
        this.order.update(x => ({ ...x, status: prevStatus }));
        this.ordersStore.rollbackStatus(o.id, prevStatus);
        this.actionLoading.set(false);
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.READY_ERROR'), 'OK', { duration: 3000 });
      },
    });
  }

  reprintKitchenTicket(): void {
    const o = this.order();
    if (!o) return;
    this.kitchenPrint.printKitchenTicket(String(o.id));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  getOrderTypeIcon(type: string): string {
    const map: Record<string, string> = {
      DELIVERY: 'delivery_dining',
      PICKUP:   'shopping_bag',
      DINE_IN:  'restaurant',
    };
    return map[type] ?? 'receipt_long';
  }

  private clearPrepTimerIfTerminal(o: Order | null): void {
    if (!o) return;
    const s = o.status;
    if (s === 'READY' || s === 'DELIVERED' || s === 'CANCELLED' || s === 'PICKED_UP') {
      this.prepTimerSession.clear(o.id);
      this.detailPrepOverdue.set(false);
    }
  }

  getItemTotal(item: any): number {
    const qty = item.quantity ?? 1;
    const price = item.unitPrice ?? item.price ?? 0;
    return qty * price;
  }

  /** Clé i18n ORDERS.HISTORY.ACTOR_* ou null */
  private actorTypeKey(updatedBy: string | null, actorType: string | null): string | null {
    const type = (actorType ?? updatedBy?.split(':')[0] ?? '').toUpperCase();
    const map: Record<string, string> = {
      SYSTEM:   'ORDERS.HISTORY.ACTOR_SYSTEM',
      CUSTOMER: 'ORDERS.HISTORY.ACTOR_CUSTOMER',
      PARTNER:  'ORDERS.HISTORY.ACTOR_PARTNER',
      COURIER:  'ORDERS.HISTORY.ACTOR_COURIER',
      ADMIN:    'ORDERS.HISTORY.ACTOR_ADMIN',
    };
    return map[type] ?? null;
  }
}
