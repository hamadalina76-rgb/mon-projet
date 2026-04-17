// src/app/features/orders/orders-list/orders-list.component.ts
import {
  Component, OnInit, OnDestroy, AfterViewInit,
  ViewChild, ElementRef, inject, signal, computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, takeUntil, debounceTime, distinctUntilChanged, pairwise, filter, skip } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { OrdersService } from '../services/orders.service';
import { OrdersStoreService } from '../services/orders-store.service';
import { WebSocketService, ConnectionStatus } from '@core/services/websocket.service';
import { NotificationService } from '@core/services/notification.service';
import { ScheduledOrderReminderService } from '@core/services/scheduled-order-reminder.service';
import { formatScheduledSlot } from '@core/utils/format-scheduled-slot';
import { OrderStatusBadgeComponent } from '../components/order-status-badge/order-status-badge.component';
import { OrderCardComponent } from '../components/order-card/order-card.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';
import {
  Order,
  hasProductPrepOnItems,
  suggestedPrepMinutesFromItems,
} from '../models/order.model';
import { partnerOrderItemsLines } from '../utils/order-item-display';
import { TimerComponent } from '../components/prep-timer/timer.component';
import { PrepTimerSessionService } from '../services/prep-timer-session.service';
import { KitchenPrintService } from '../services/kitchen-print.service';
import {
  mergePrepTimerContext,
  buildPrepTimerContextAfterAccept,
  PrepTimerContext,
} from '../utils/prep-timer.utils';
import {
  AcceptOrderDialogComponent,
  AcceptOrderDialogResult,
} from '../dialogs/accept-order-dialog.component';
import {
  RejectOrderDialogComponent,
  RejectOrderDialogResult,
} from '../dialogs/reject-order-dialog.component';

const TAB_DEFS = [
  { value: 'all',       labelKey: 'ORDERS.TABS.ALL'       },
  { value: 'PENDING',   labelKey: 'ORDERS.TABS.PENDING'   },
  { value: 'CONFIRMED', labelKey: 'ORDERS.TABS.CONFIRMED' },
  { value: 'PREPARING', labelKey: 'ORDERS.TABS.PREPARING' },
  { value: 'READY',     labelKey: 'ORDERS.TABS.READY'     },
  { value: 'CANCELLED', labelKey: 'ORDERS.TABS.CANCELLED' },
] as const;

export type DatePreset = 'today' | 'yesterday' | '7d' | 'month' | 'all';

const DATE_PRESET_DEFS: { value: DatePreset; icon: string; labelKey: string }[] = [
  { value: 'today',     icon: 'today',          labelKey: 'ORDERS.DATE.TODAY'       },
  { value: 'yesterday', icon: 'history',        labelKey: 'ORDERS.DATE.YESTERDAY'   },
  { value: '7d',        icon: 'date_range',     labelKey: 'ORDERS.DATE.LAST_7_DAYS' },
  { value: 'month',     icon: 'calendar_month', labelKey: 'ORDERS.DATE.THIS_MONTH'  },
  { value: 'all',       icon: 'all_inclusive',  labelKey: 'ORDERS.DATE.ALL'         },
];

/** Taille des lots en mode mobile (style Glovo) */
const MOBILE_BATCH_SIZE = 5;

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [
    CommonModule,
    TranslateModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatSnackBarModule,
    MatDialogModule,
    OrderStatusBadgeComponent,
    OrderCardComponent,
    TimerComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    TimeAgoPipe,
  ],
  templateUrl: './orders-list.component.html',
  styleUrls: ['./orders-list.component.scss'],
})
export class OrdersListComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild('mobileScrollSentinel') mobileScrollSentinel?: ElementRef<HTMLElement>;

  private ordersService = inject(OrdersService);
  private ordersStore   = inject(OrdersStoreService);
  private wsService     = inject(WebSocketService);
  private notifService  = inject(NotificationService);
  private snackBar      = inject(MatSnackBar);
  private dialog        = inject(MatDialog);
  private router        = inject(Router);
  private breakpoints   = inject(BreakpointObserver);
  private translate     = inject(TranslateService);
  private prepTimerSession = inject(PrepTimerSessionService);
  private kitchenPrint   = inject(KitchenPrintService);
  private scheduledReminders = inject(ScheduledOrderReminderService);
  private destroy$      = new Subject<void>();

  private searchInput$ = new Subject<string>();

  loading          = signal(false);
  loadingMore      = signal(false);
  isMobile         = signal(false);
  selectedStatus   = signal<string>('all');
  searchQuery      = signal('');
  connectionStatus = signal<ConnectionStatus>('disconnected');
  newOrderIds      = signal<Set<string>>(new Set());
  soundEnabled     = signal(this.notifService.isSoundEnabled());
  /** Filtre date actif (style Glovo : "Aujourd'hui" par défaut) */
  datePreset       = signal<DatePreset>('today');

  totalElements  = signal(0);
  currentPage    = signal(0);
  currentSize    = signal(10);
  /** « Toutes » : tri urgence (type Glovo) ; onglets filtrés : plus récent en premier */
  sortBy         = signal('priority');
  sortDir        = signal<'asc' | 'desc'>('desc');

  counts = signal<Record<string, number>>({
    ALL: 0, all: 0, PENDING: 0, CONFIRMED: 0, PREPARING: 0, READY: 0, CANCELLED: 0,
  });

  /** Mobile: commandes accumulées + page suivante à charger */
  mobileOrders       = signal<Order[]>([]);
  mobileNextPage     = signal(0);
  mobileHasMore      = computed(() => {
    const loaded = this.mobileOrders().length;
    const total  = this.totalElements();
    return loaded > 0 && loaded < total;
  });

  tabDefs        = TAB_DEFS;
  datePresetDefs = DATE_PRESET_DEFS;
  displayedColumns = [
    'indicator', 'orderNumber', 'customer', 'items', 'subtotal', 'status', 'time', 'prepTimer', 'actions',
  ];

  /** Lignes dont le minuteur de prépa a dépassé l’échéance (clignotement). */
  prepTimerExpiredIds = signal<Set<string>>(new Set());
  /** Déduplication des rappels "attente terminée" côté UI. */
  scheduledDueReminderIds = signal<Set<string>>(new Set());

  dataSource = new MatTableDataSource<Order>([]);

  private mobileObserver?: IntersectionObserver;
  private destroyed = false;
  private highlightTimeouts = new Map<string, ReturnType<typeof setTimeout>>();
  private prepTimerContextCache = new Map<string, { signature: string; context: PrepTimerContext | null }>();

  ngOnInit(): void {
    this.isMobile.set(this.breakpoints.isMatched('(max-width: 767px)'));
    this.subscribeToWebSocket();
    this.subscribeSearchDebounce();
    this.loadCounts();

    this.translate.onLangChange.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.applyPaginatorIntl();
    });

    this.breakpoints.observe('(max-width: 767px)')
      .pipe(takeUntil(this.destroy$))
      .subscribe((r) => {
        const now = r.matches;
        const prev = this.isMobile();
        if (prev === now) return;
        this.isMobile.set(now);
        this.teardownMobileObserver();
        if (now) {
          this.loadMobileOrders(true);
        } else {
          this.currentPage.set(0);
          this.loadOrders();
        }
      });
  }

  ngAfterViewInit(): void {
    if (this.sort) {
      this.sort.sortChange.pipe(takeUntil(this.destroy$)).subscribe((s) => {
        const active = s.active;
        if (active === 'time') {
          this.sortBy.set('orderTime');
        } else {
          this.sortBy.set(active);
        }
        this.sortDir.set((s.direction || 'desc') as 'asc' | 'desc');
        this.currentPage.set(0);
        if (this.isMobile()) this.loadMobileOrders(true);
        else this.loadOrders();
      });
    }

    if (this.paginator) {
      this.applyPaginatorIntl();

      /* skip(1): évite double chargement — le 1er load est fait explicitement ci-dessous */
      this.paginator.page.pipe(skip(1), takeUntil(this.destroy$)).subscribe((evt) => {
        this.currentPage.set(evt.pageIndex);
        this.currentSize.set(evt.pageSize);
        this.loadOrders();
      });
    }

    if (this.isMobile()) {
      this.loadMobileOrders(true);
    } else {
      this.loadOrders();
    }

    setTimeout(() => {
      if (this.destroyed) return;
      this.setupMobileInfiniteScroll();
    }, 0);
  }

  private applyPaginatorIntl(): void {
    if (!this.paginator) return;
    const t = (k: string, p?: object) => this.translate.instant(k, p);
    this.paginator._intl.itemsPerPageLabel = t('ORDERS.PAGINATOR_PER_PAGE');
    this.paginator._intl.nextPageLabel     = t('ORDERS.PAGINATOR_NEXT');
    this.paginator._intl.previousPageLabel = t('ORDERS.PAGINATOR_PREV');
    this.paginator._intl.firstPageLabel   = t('ORDERS.PAGINATOR_FIRST');
    this.paginator._intl.lastPageLabel    = t('ORDERS.PAGINATOR_LAST');
    this.paginator._intl.getRangeLabel = (page, pageSize, length) => {
      if (length === 0) return t('ORDERS.PAGINATOR_RANGE_ZERO');
      const start = page * pageSize + 1;
      const end   = Math.min((page + 1) * pageSize, length);
      return t('ORDERS.PAGINATOR_RANGE', { start, end, length });
    };
    this.paginator._intl.changes.next();
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.teardownMobileObserver();
    this.clearHighlightTimeouts();
    this.prepTimerContextCache.clear();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Compteur chip: backend ALL vs clé UI `all` */
  countForTab(tabValue: string): number {
    const c = this.counts();
    if (tabValue === 'all') return c['all'] ?? c['ALL'] ?? 0;
    return c[tabValue] ?? 0;
  }

  // ── Desktop: pagination serveur ───────────────────────────────────────────

  /** Calcule les bornes ISO LocalDateTime à envoyer à l'API depuis le preset actif. */
  private computeRange(preset: DatePreset): { from: string; to: string } | null {
    const pad = (n: number) => String(n).padStart(2, '0');
    const fmt  = (d: Date) =>
      `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;

    const now        = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    const todayEnd   = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);

    switch (preset) {
      case 'today':
        return { from: fmt(todayStart), to: fmt(todayEnd) };
      case 'yesterday': {
        const s = new Date(todayStart.getTime() - 86_400_000);
        const e = new Date(todayStart.getTime() - 1000);
        return { from: fmt(s), to: fmt(e) };
      }
      case '7d': {
        const s = new Date(todayStart.getTime() - 6 * 86_400_000);
        return { from: fmt(s), to: fmt(todayEnd) };
      }
      case 'month': {
        const s = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
        return { from: fmt(s), to: fmt(todayEnd) };
      }
      default:
        return null;
    }
  }

  setDatePreset(preset: DatePreset): void {
    this.datePreset.set(preset);
    this.currentPage.set(0);
    this.loadCounts();
    if (this.isMobile()) this.loadMobileOrders(true);
    else this.loadOrders();
  }

  loadOrders(): void {
    if (this.isMobile()) return;

    this.loading.set(true);
    const range = this.computeRange(this.datePreset());

    this.ordersService.getOrders({
      page:    this.currentPage(),
      size:    this.currentSize(),
      status:  this.selectedStatus(),
      search:  this.searchQuery(),
      sortBy:  this.sortBy(),
      sortDir: this.sortDir(),
      from:    range?.from,
      to:      range?.to,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        const orders = page.content as Order[];
        this.prepTimerContextCache.clear();
        this.clearPrepSessionsForTerminalOrders(orders);
        const prioritized = this.prioritizeOrdersForAction(orders);
        this.dataSource.data = prioritized;
        this.totalElements.set(page.totalElements);
        this.currentPage.set(page.number);
        this.currentSize.set(page.size);

        if (this.paginator) {
          this.paginator.length = page.totalElements;
          if (this.paginator.pageSize !== page.size) {
            this.paginator.pageSize = page.size;
          }
          if (this.paginator.pageIndex !== page.number) {
            this.paginator.pageIndex = page.number;
          }
        }

        this.ordersStore.setOrders(prioritized);
        this.notifService.updateTabTitle(this.counts()['PENDING'] ?? 0);
        this.loading.set(false);
        this.scheduledReminders.refreshFromActiveApi();
      },
      error: () => this.loading.set(false),
    });
  }

  // ── Mobile: lots de 5 + scroll infini ────────────────────────────────────

  loadMobileOrders(reset: boolean): void {
    if (!this.isMobile()) return;

    if (reset) {
      this.loading.set(true);
      this.mobileNextPage.set(0);
      this.mobileOrders.set([]);
    } else {
      if (!this.mobileHasMore() || this.loadingMore()) return;
      this.loadingMore.set(true);
    }

    const pageIdx = reset ? 0 : this.mobileNextPage();
    const range   = this.computeRange(this.datePreset());

    this.ordersService.getOrders({
      page:    pageIdx,
      size:    MOBILE_BATCH_SIZE,
      status:  this.selectedStatus(),
      search:  this.searchQuery(),
      sortBy:  this.sortBy(),
      sortDir: this.sortDir(),
      from:    range?.from,
      to:      range?.to,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page) => {
        const chunk = page.content as Order[];
        this.prepTimerContextCache.clear();
        this.clearPrepSessionsForTerminalOrders(chunk);
        let nextMobile: Order[];
        if (reset) {
          nextMobile = chunk;
        } else {
          const prev = this.mobileOrders();
          const seen = new Set(prev.map((o) => o.id));
          const merged = [...prev];
          for (const o of chunk) {
            if (!seen.has(o.id)) merged.push(o);
          }
          nextMobile = merged;
        }
        const prioritized = this.prioritizeOrdersForAction(nextMobile);
        this.mobileOrders.set(prioritized);
        this.mobileNextPage.set(pageIdx + 1);
        this.totalElements.set(page.totalElements);
        this.ordersStore.setOrders(prioritized);
        this.notifService.updateTabTitle(this.counts()['PENDING'] ?? 0);
        this.loading.set(false);
        this.loadingMore.set(false);
        if (reset) this.scheduledReminders.refreshFromActiveApi();
        setTimeout(() => {
          if (this.destroyed) return;
          this.setupMobileInfiniteScroll();
        }, 100);
      },
      error: () => {
        this.loading.set(false);
        this.loadingMore.set(false);
      },
    });
  }

  loadMoreMobile(): void {
    this.loadMobileOrders(false);
  }

  private setupMobileInfiniteScroll(): void {
    this.teardownMobileObserver();
    if (!this.isMobile() || !this.mobileScrollSentinel?.nativeElement) return;

    this.mobileObserver = new IntersectionObserver(
      (entries) => {
        const e = entries[0];
        if (e?.isIntersecting && this.mobileHasMore() && !this.loadingMore() && !this.loading()) {
          this.loadMoreMobile();
        }
      },
      { root: null, rootMargin: '120px', threshold: 0 }
    );
    this.mobileObserver.observe(this.mobileScrollSentinel.nativeElement);
  }

  private teardownMobileObserver(): void {
    this.mobileObserver?.disconnect();
    this.mobileObserver = undefined;
  }

  loadCounts(): void {
    const range = this.computeRange(this.datePreset());
    this.ordersService
      .getOrderCounts({ from: range?.from, to: range?.to })
      .pipe(takeUntil(this.destroy$))
      .subscribe((c) => {
        this.counts.set(c);
      });
  }

  setStatus(status: string): void {
    this.selectedStatus.set(status);
    this.currentPage.set(0);
    if (status === 'all') {
      this.sortBy.set('priority');
      this.sortDir.set('desc');
    } else {
      this.sortBy.set('orderTime');
      this.sortDir.set('desc');
    }
    if (this.isMobile()) this.loadMobileOrders(true);
    else this.loadOrders();
  }

  /** Colonne Material alignée sur sortBy (time → API orderTime) */
  matSortActiveColumn(): string {
    const s = this.sortBy();
    if (s === 'orderTime') return 'time';
    return s;
  }

  onSearch(event: Event): void {
    this.searchInput$.next((event.target as HTMLInputElement).value);
  }

  onClearSearch(): void {
    this.searchQuery.set('');
    this.currentPage.set(0);
    if (this.isMobile()) this.loadMobileOrders(true);
    else this.loadOrders();
  }

  private subscribeSearchDebounce(): void {
    this.searchInput$.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe((value) => {
      this.searchQuery.set(value);
      this.currentPage.set(0);
      if (this.isMobile()) this.loadMobileOrders(true);
      else this.loadOrders();
    });
  }

  /** Lignes produit + options / suppléments / notes pour la colonne Articles. */
  itemsLines(order: any): { title: string; meta: string[] }[] {
    return partnerOrderItemsLines(order);
  }

  totalItems(order: any): number {
    return (order.items ?? []).reduce((s: number, it: any) => s + (it.quantity ?? 1), 0);
  }

  isNewOrder(id: string): boolean {
    return this.newOrderIds().has(id);
  }

  prepTimerContextForRow(row: Order): PrepTimerContext | null {
    const session = this.prepTimerSession.load(row.id);
    const signature = [
      row.id,
      row.status ?? '',
      row.isScheduled ? '1' : '0',
      row.scheduledDeliveryTime ?? '',
      row.prepTime ?? '',
      row.confirmedAt ?? '',
      row.preparingAt ?? '',
      row.suggestedPreparationMinutes ?? '',
      session?.startTimeIso ?? '',
      session?.durationMinutes ?? '',
    ].join('|');

    const cached = this.prepTimerContextCache.get(row.id);
    if (cached?.signature === signature) return cached.context;

    const context = mergePrepTimerContext(session, row);
    this.prepTimerContextCache.set(row.id, { signature, context });
    return context;
  }

  isPrepTimerRowOverdue(orderId: string): boolean {
    return this.prepTimerExpiredIds().has(orderId);
  }

  onPrepTimerExpired(orderId: string, expired: boolean): void {
    if (this.destroyed) return;
    this.prepTimerExpiredIds.update((set) => {
      const next = new Set(set);
      if (expired) next.add(orderId);
      else next.delete(orderId);
      return next;
    });
    if (expired) {
      this.notifyScheduledDueNow(orderId);
    }
    this.reprioritizeVisibleOrders();
  }

  private clearPrepSessionsForTerminalOrders(orders: Order[]): void {
    for (const o of orders) this.clearPrepTimerIfTerminal(o);
  }

  private clearPrepTimerIfTerminal(o: Order): void {
    const s = o.status;
    if (s === 'READY' || s === 'DELIVERED' || s === 'CANCELLED' || s === 'PICKED_UP') {
      this.prepTimerSession.clear(o.id);
      this.prepTimerExpiredIds.update((set) => {
        const next = new Set(set);
        next.delete(o.id);
        return next;
      });
      this.scheduledDueReminderIds.update((set) => {
        const next = new Set(set);
        next.delete(o.id);
        return next;
      });
    }
  }

  openDetail(order: any): void {
    this.router.navigate(['/orders', order.id]);
  }

  /** Ouvre le dialogue temps de préparation puis confirme (liste + cartes). */
  openAcceptDialog(orderId: string, event?: Event): void {
    event?.stopPropagation();
    const o = this.findOrderForId(orderId);
    if (!o) return;

    const items = o.items ?? [];
    const fromOrder = o.suggestedPreparationMinutes;
    const fromItems = suggestedPrepMinutesFromItems(items);
    const suggested =
      fromOrder != null && fromOrder > 0 ? fromOrder : fromItems;
    const showHint =
      hasProductPrepOnItems(items) || (fromOrder != null && fromOrder > 0);

    this.dialog
      .open(AcceptOrderDialogComponent, {
        data: {
          orderNumber: o.orderNumber,
          orderId: o.id,
          suggestedFromProductsMinutes: showHint ? suggested : undefined,
          showProductPrepHint: showHint,
          isScheduled: o.isScheduled === true,
          scheduledDeliveryTime: o.scheduledDeliveryTime,
          orderLines: partnerOrderItemsLines(o),
        },
        panelClass: 'sl-dialog-panel',
        maxWidth: '90vw',
      })
      .afterClosed()
      .subscribe((result: AcceptOrderDialogResult | null) => {
        if (!result?.accepted) return;
        this.executeAccept(orderId, result.prepTime);
      });
  }

  openRejectDialog(orderId: string, event?: Event): void {
    event?.stopPropagation();
    const o = this.findOrderForId(orderId);
    if (!o) return;

    this.dialog
      .open(RejectOrderDialogComponent, {
        data: { orderNumber: o.orderNumber, orderId: o.id },
        panelClass: 'sl-dialog-panel',
        maxWidth: '90vw',
      })
      .afterClosed()
      .subscribe((result: RejectOrderDialogResult | null) => {
        if (!result?.rejected) return;
        this.executeReject(orderId, result.reason);
      });
  }

  private findOrderForId(orderId: string): Order | undefined {
    const fromTable = this.dataSource.data.find((x) => x.id === orderId);
    if (fromTable) return fromTable;
    return this.mobileOrders().find((x) => x.id === orderId);
  }

  private applyOrderUpdate(u: Order): void {
    this.prepTimerContextCache.delete(u.id);
    this.clearPrepTimerIfTerminal(u);
    if (u.status !== 'CONFIRMED') {
      this.scheduledDueReminderIds.update((set) => {
        const next = new Set(set);
        next.delete(u.id);
        return next;
      });
    }
    this.ordersStore.updateOrder(u);
    this.dataSource.data = this.dataSource.data.map((o) =>
      o.id === u.id ? { ...o, ...u } : o
    );
    this.mobileOrders.update((list) =>
      list.map((o) => (o.id === u.id ? { ...o, ...u } : o))
    );
    this.reprioritizeVisibleOrders();
  }

  private executeAccept(orderId: string, prepTime: number): void {
    const sourceOrder = this.findOrderForId(orderId);
    const effectivePrepTime = this.resolveEffectivePrepTime(sourceOrder, prepTime);

    const prev = (this.ordersStore.getOrder(orderId) as any)?.status;
    this.ordersStore.updateOrder({ id: orderId, status: 'CONFIRMED' } as any);
    this.refreshRowEverywhere(orderId, 'CONFIRMED');
    this.ordersService.confirmOrder(orderId, effectivePrepTime).subscribe({
      next: (u) => {
        this.applyOrderUpdate(u);
        const ctx = buildPrepTimerContextAfterAccept(u, effectivePrepTime);
        if (ctx) {
          this.prepTimerSession.save(orderId, ctx);
          this.prepTimerContextCache.delete(orderId);
        }
        this.kitchenPrint.printKitchenTicket(String(orderId));
        this.loadCounts();
        this.snackBar.open(this.acceptSuccessToast(u as Order, effectivePrepTime), undefined, {
          duration: 5000,
          panelClass: ['sl-snack-success'],
        });
        this.scheduledReminders.refreshFromActiveApi();
      },
      error: () => {
        this.ordersStore.rollbackStatus(orderId, prev);
        this.refreshRowEverywhere(orderId, prev);
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.CONFIRM_ERROR'), 'OK', { duration: 3000 });
      },
    });
  }

  private resolveEffectivePrepTime(order: Order | undefined, prepTime: number): number {
    const provided = Number.isFinite(prepTime) && prepTime > 0 ? Math.round(prepTime) : 0;
    if (!order) return provided > 0 ? provided : 15;

    const fromItems = suggestedPrepMinutesFromItems(order.items ?? []);
    const fromOrder = order.suggestedPreparationMinutes != null && order.suggestedPreparationMinutes > 0
      ? Math.round(order.suggestedPreparationMinutes)
      : 0;
    const suggested = fromOrder > 0 ? fromOrder : fromItems;

    // Scheduled flow: enforce at least product-suggested prep for reliable due-time scheduling.
    if (order.isScheduled === true) {
      return Math.max(provided > 0 ? provided : suggested, suggested, 10);
    }

    return Math.max(provided > 0 ? provided : suggested, 10);
  }

  private acceptSuccessToast(updated: Order, prepTime: number): string {
    if (updated.isScheduled && updated.scheduledDeliveryTime) {
      const slot = formatScheduledSlot(
        updated.scheduledDeliveryTime,
        this.translate.currentLang || 'fr',
      );
      return this.translate.instant('ORDERS.TOAST.ACCEPTED_SCHEDULED', { minutes: prepTime, slot });
    }
    return this.translate.instant('ORDERS.TOAST.ACCEPTED', { minutes: prepTime });
  }

  private executeReject(orderId: string, reason: string): void {
    const prev = (this.ordersStore.getOrder(orderId) as any)?.status;
    this.ordersStore.updateOrder({ id: orderId, status: 'CANCELLED', cancelReason: reason } as any);
    this.refreshRowEverywhere(orderId, 'CANCELLED');
    this.ordersService.cancelOrder(orderId, reason).subscribe({
      next: (u) => {
        this.prepTimerSession.clear(orderId);
        this.prepTimerContextCache.delete(orderId);
        this.onPrepTimerExpired(orderId, false);
        this.applyOrderUpdate(u);
        this.loadCounts();
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.REJECTED'), undefined, {
          duration: 3000,
          panelClass: ['sl-snack-error'],
        });
      },
      error: () => {
        this.ordersStore.rollbackStatus(orderId, prev);
        this.refreshRowEverywhere(orderId, prev);
        this.snackBar.open(this.translate.instant('ORDERS.TOAST.REJECT_ERROR'), 'OK', { duration: 4000 });
      },
    });
  }

  startPreparing(orderId: string, event?: Event): void {
    event?.stopPropagation();
    const prev = (this.ordersStore.getOrder(orderId) as any)?.status;
    this.ordersStore.updateOrder({ id: orderId, status: 'PREPARING' } as any);
    this.refreshRowEverywhere(orderId, 'PREPARING');
    this.ordersService.startPreparing(orderId).subscribe({
      next: (u) => { this.ordersStore.updateOrder(u); this.refreshRowEverywhere(orderId, u.status); this.loadCounts(); },
      error: () => { this.ordersStore.rollbackStatus(orderId, prev); this.refreshRowEverywhere(orderId, prev); this.snackBar.open(this.translate.instant('ORDERS.TOAST.PREP_ERROR'), 'OK', { duration: 3000 }); },
    });
  }

  markReady(orderId: string, event?: Event): void {
    event?.stopPropagation();
    const prev = (this.ordersStore.getOrder(orderId) as any)?.status;
    this.ordersStore.updateOrder({ id: orderId, status: 'READY' } as any);
    this.refreshRowEverywhere(orderId, 'READY');
    this.ordersService.markReady(orderId).subscribe({
      next: (u) => {
        this.prepTimerSession.clear(orderId);
        this.prepTimerContextCache.delete(orderId);
        this.onPrepTimerExpired(orderId, false);
        this.ordersStore.updateOrder(u);
        this.refreshRowEverywhere(orderId, u.status);
        this.loadCounts();
      },
      error: () => { this.ordersStore.rollbackStatus(orderId, prev); this.refreshRowEverywhere(orderId, prev); this.snackBar.open(this.translate.instant('ORDERS.TOAST.READY_ERROR'), 'OK', { duration: 3000 }); },
    });
  }

  private refreshRowEverywhere(orderId: string, status: string): void {
    this.prepTimerContextCache.delete(orderId);
    this.dataSource.data = this.dataSource.data.map((o: any) =>
      o.id === orderId ? { ...o, status } : o
    );
    this.mobileOrders.update((list) =>
      list.map((o: any) => (o.id === orderId ? { ...o, status } : o))
    );
  }

  toggleSound(): void {
    this.soundEnabled.set(this.notifService.toggleSound());
  }

  private subscribeToWebSocket(): void {
    this.wsService.getConnectionStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe((s) => this.connectionStatus.set(s));

    this.wsService.getConnectionStatus().pipe(
      takeUntil(this.destroy$),
      distinctUntilChanged(),
      pairwise(),
      filter(([prev, curr]) => prev !== 'connected' && curr === 'connected'),
    ).subscribe(() => {
      this.loadCounts();
      if (this.isMobile()) this.loadMobileOrders(true);
      else this.loadOrders();
    });

    this.wsService.onNewOrder()
      .pipe(takeUntil(this.destroy$))
      .subscribe((raw: any) => {
        const rawId = raw?.id != null ? String(raw.id) : String(raw?.orderId ?? '');
        if (!rawId) return;

        // The WebSocket payload is a partial notification (orderId + orderNumber only).
        // Always fetch the full order from the API so items, customer and amount are populated.
        this.ordersService.getOrder(rawId).pipe(takeUntil(this.destroy$)).subscribe({
          next: (fullOrder) => {
            this.ordersStore.addOrder(fullOrder);

            if (this.isMobile()) {
              if (this.mobileNextPage() <= 1) {
                this.mobileOrders.update((list) =>
                  list.some((o) => o.id === fullOrder.id) ? list : [fullOrder, ...list]
                );
                this.totalElements.update((t) => t + 1);
                this.reprioritizeVisibleOrders();
              }
            } else if (this.currentPage() === 0) {
              const current = this.dataSource.data;
              if (!current.some((o: any) => o.id === fullOrder.id)) {
                this.dataSource.data = [fullOrder, ...current];
                this.totalElements.update((t) => t + 1);
                if (this.paginator) this.paginator.length = this.totalElements();
                this.reprioritizeVisibleOrders();
              } else {
                // Order already in list (race with polling): update in place
                this.dataSource.data = this.dataSource.data.map((o: any) =>
                  o.id === fullOrder.id ? fullOrder : o
                );
                this.reprioritizeVisibleOrders();
              }
            }

            this.newOrderIds.update((ids) => new Set([...ids, fullOrder.id]));
            const existingTimer = this.highlightTimeouts.get(fullOrder.id);
            if (existingTimer) clearTimeout(existingTimer);
            const timerId = setTimeout(() => {
              if (this.destroyed) return;
              this.newOrderIds.update((ids) => { const n = new Set(ids); n.delete(fullOrder.id); return n; });
            }, 3000);
            this.highlightTimeouts.set(fullOrder.id, timerId);

            this.loadCounts();
            this.notifService.newOrderAlert(fullOrder.orderNumber ?? '');
            this.notifService.showOrderBanner(fullOrder);
            this.notifService.showBrowserNotification(
              this.translate.instant('ORDERS.NOTIF_NEW_ORDER'),
              `#${fullOrder.orderNumber}`,
              {
                notification: {
                  type: 'ORDER',
                  data: {
                    orderId: fullOrder.id,
                    id: fullOrder.id,
                    action: 'ORDER_NEW',
                    orderNumber: fullOrder.orderNumber,
                  },
                },
              }
            );
            this.scheduledReminders.refreshFromActiveApi();
          },
          // On error fall back to partial data so the row at least appears
          error: () => {
            const fallback = {
              ...raw,
              id: rawId,
            } as Order;
            this.ordersStore.addOrder(fallback);
            if (this.isMobile()) {
              if (this.mobileNextPage() <= 1) {
                this.mobileOrders.update((list) =>
                  list.some((o) => o.id === fallback.id) ? list : [fallback, ...list]
                );
                this.totalElements.update((t) => t + 1);
                this.reprioritizeVisibleOrders();
              }
            } else if (this.currentPage() === 0) {
              const current = this.dataSource.data;
              if (!current.some((o: any) => o.id === fallback.id)) {
                this.dataSource.data = [fallback, ...current];
                this.totalElements.update((t) => t + 1);
                if (this.paginator) this.paginator.length = this.totalElements();
                this.reprioritizeVisibleOrders();
              }
            }
            this.loadCounts();
            this.notifService.newOrderAlert(fallback.orderNumber ?? '');
            this.scheduledReminders.refreshFromActiveApi();
          },
        });
      });
  }

  private reprioritizeVisibleOrders(): void {
    this.prepTimerContextCache.clear();
    this.dataSource.data = this.prioritizeOrdersForAction(this.dataSource.data);
    this.mobileOrders.update((list) => this.prioritizeOrdersForAction(list));
  }

  private clearHighlightTimeouts(): void {
    for (const t of this.highlightTimeouts.values()) {
      clearTimeout(t);
    }
    this.highlightTimeouts.clear();
  }

  private prioritizeOrdersForAction(orders: Order[]): Order[] {
    if (!orders || orders.length <= 1) return orders;

    const ranked = orders.map((order, index) => ({
      order,
      index,
      score: this.prepActionPriority(order),
    }));

    if (!ranked.some((x) => x.score > 0)) return orders;

    ranked.sort((a, b) => {
      if (a.score !== b.score) return b.score - a.score;
      return a.index - b.index;
    });
    return ranked.map((x) => x.order);
  }

  private prepActionPriority(order: Order): number {
    if (!this.isPrepDeadlineReached(order)) return 0;
    if (order.status === 'PREPARING') return 3;
    if (order.status === 'CONFIRMED') return 2;
    return 0;
  }

  private isPrepDeadlineReached(order: Order): boolean {
    if (order.status !== 'CONFIRMED' && order.status !== 'PREPARING') return false;
    const ctx = mergePrepTimerContext(this.prepTimerSession.load(order.id), order);
    if (!ctx) return false;
    const startMs = Date.parse(ctx.startTimeIso);
    if (Number.isNaN(startMs)) return false;
    const deadlineMs = startMs + ctx.durationMinutes * 60_000;
    return Date.now() >= deadlineMs;
  }

  private notifyScheduledDueNow(orderId: string): void {
    if (this.scheduledDueReminderIds().has(orderId)) return;
    const order = this.findOrderForId(orderId);
    if (!order) return;
    if (order.status !== 'CONFIRMED' || order.isScheduled !== true) return;
    if (!this.isPrepDeadlineReached(order)) return;

    const title = this.translate.instant('ORDERS.NOTIF_SCHEDULED_PREP_TITLE');
    const message = this.translate.instant('ORDERS.TOAST.PREP_DEADLINE_CONFIRMED', {
      orderNumber: order.orderNumber ?? orderId,
    });

    this.notifService.showScheduledPrepReminderFromServer({
      title,
      message,
      type: 'ORDER',
      channel: 'IN_APP',
      data: {
        id: orderId,
        orderId,
        action: 'ORDER_SCHEDULED_PREP_REMINDER',
        orderNumber: order.orderNumber,
        status: order.status,
      },
    });

    this.scheduledDueReminderIds.update((set) => {
      const next = new Set(set);
      next.add(orderId);
      return next;
    });
  }

  get statusIcon(): string {
    switch (this.connectionStatus()) {
      case 'connected':    return 'wifi';
      case 'connecting':   return 'wifi_find';
      case 'disconnected': return 'wifi_off';
    }
  }

  get statusLabel(): string {
    switch (this.connectionStatus()) {
      case 'connected':    return this.translate.instant('ORDERS.WS_CONNECTED');
      case 'connecting':   return this.translate.instant('ORDERS.WS_CONNECTING');
      case 'disconnected': return this.translate.instant('ORDERS.WS_DISCONNECTED');
    }
  }

}
