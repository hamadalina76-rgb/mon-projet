// src/app/features/orders/order-history/order-history.component.ts
import {
  Component,
  OnInit,
  AfterViewInit,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { debounceTime, distinctUntilChanged, filter, takeUntil } from 'rxjs/operators';
import { Subject, firstValueFrom } from 'rxjs';
import { OrderHistoryDataSource } from './order-history.datasource';
import {
  OrdersService,
  PartnerHistoryBaseFilters,
  PartnerHistoryStatusChip,
  PartnerOrderHistorySummary,
} from '../services/orders.service';
import { Order } from '../models/order.model';
import { OrderStatusBadgeComponent } from '../components/order-status-badge/order-status-badge.component';
import { TimeAgoPipe } from '@shared/pipes/time-ago.pipe';

export type HistoryDatePreset = 'today' | 'yesterday' | '7d' | 'month' | 'custom';

const DATE_PRESET_DEFS: { value: HistoryDatePreset; icon: string; labelKey: string }[] = [
  { value: 'today',     icon: 'today',          labelKey: 'ORDERS.DATE.TODAY'       },
  { value: 'yesterday', icon: 'history',        labelKey: 'ORDERS.DATE.YESTERDAY'   },
  { value: '7d',        icon: 'date_range',     labelKey: 'ORDERS.DATE.LAST_7_DAYS' },
  { value: 'month',     icon: 'calendar_month', labelKey: 'ORDERS.DATE.THIS_MONTH'  },
  { value: 'custom',    icon: 'edit_calendar',  labelKey: 'ORDERS.PARTNER_HISTORY.CUSTOM' },
];

const STATUS_CHIP_DEFS: { value: PartnerHistoryStatusChip; labelKey: string }[] = [
  { value: 'ALL',       labelKey: 'ORDERS.PARTNER_HISTORY.CHIP_ALL'       },
  { value: 'DELIVERED', labelKey: 'ORDERS.PARTNER_HISTORY.CHIP_DELIVERED' },
  { value: 'CANCELLED', labelKey: 'ORDERS.PARTNER_HISTORY.CHIP_CANCELLED' },
  { value: 'REFUSED',   labelKey: 'ORDERS.PARTNER_HISTORY.CHIP_REFUSED'   },
];

function pad(n: number): string { return String(n).padStart(2, '0'); }

function fmtLocal(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function computePresetRange(preset: HistoryDatePreset): { from: string; to: string } | null {
  const now        = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
  const todayEnd   = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
  switch (preset) {
    case 'today':
      return { from: fmtLocal(todayStart), to: fmtLocal(todayEnd) };
    case 'yesterday': {
      const s = new Date(todayStart.getTime() - 86_400_000);
      const e = new Date(todayStart.getTime() - 1000);
      return { from: fmtLocal(s), to: fmtLocal(e) };
    }
    case '7d': {
      const s = new Date(todayStart.getTime() - 6 * 86_400_000);
      return { from: fmtLocal(s), to: fmtLocal(todayEnd) };
    }
    case 'month': {
      const s = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
      return { from: fmtLocal(s), to: fmtLocal(todayEnd) };
    }
    default:
      return null;
  }
}

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSidenavModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    TranslateModule,
    OrderStatusBadgeComponent,
    TimeAgoPipe,
  ],
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.scss'],
})
export class OrderHistoryComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatPaginator) paginator?: MatPaginator;
  @ViewChild('detailNav') detailNav?: MatSidenav;

  private readonly fb            = inject(FormBuilder);
  private readonly ordersService = inject(OrdersService);
  private readonly translate     = inject(TranslateService);
  private readonly destroy$      = new Subject<void>();

  readonly pageSize  = 20;
  readonly displayedColumns = [
    'indicator', 'orderNumber', 'customer', 'items', 'total', 'status', 'date',
  ] as const;

  readonly dataSource = new OrderHistoryDataSource(this.ordersService);

  readonly totalElements = signal(0);
  readonly loading       = signal(false);
  readonly exporting     = signal(false);
  readonly summary       = signal<PartnerOrderHistorySummary | null>(null);
  readonly selectedOrder = signal<Order | null>(null);
  readonly detailLoading = signal(false);

  readonly datePreset  = signal<HistoryDatePreset>('7d');
  readonly statusChip  = signal<PartnerHistoryStatusChip>('ALL');
  readonly searchQuery = signal('');

  readonly datePresetDefs = DATE_PRESET_DEFS;
  readonly statusChipDefs = STATUS_CHIP_DEFS;

  readonly customRangeForm = this.fb.group({
    start: [null as Date | null],
    end:   [null as Date | null],
  });

  private customFrom: string | null = null;
  private customTo:   string | null = null;

  private searchSubject$ = new Subject<string>();

  ngOnInit(): void {
    this.dataSource.loading$.pipe(takeUntil(this.destroy$)).subscribe((v) => this.loading.set(v));

    this.searchSubject$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((v) => {
        this.searchQuery.set(v);
        this.paginator?.firstPage();
        this.reloadTableOnly();
      });

    this.customRangeForm.valueChanges
      .pipe(
        debounceTime(50),
        filter(() => {
          const s = this.customRangeForm.value.start;
          const e = this.customRangeForm.value.end;
          return s != null && e != null;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        const s = this.customRangeForm.value.start!;
        const e = this.customRangeForm.value.end!;
        s.setHours(0, 0, 0);
        e.setHours(23, 59, 59);
        this.customFrom = fmtLocal(s);
        this.customTo   = fmtLocal(e);
        this.paginator?.firstPage();
        this.reloadTableAndSummary();
      });
  }

  ngAfterViewInit(): void {
    this.dataSource.total$.pipe(takeUntil(this.destroy$)).subscribe((total) => {
      this.totalElements.set(total);
      if (this.paginator) this.paginator.length = total;
    });

    this.applyPaginatorIntl();
    this.translate.onLangChange.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.applyPaginatorIntl();
    });

    this.reloadTableAndSummary();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.dataSource.disconnect();
  }

  setDatePreset(value: HistoryDatePreset): void {
    this.datePreset.set(value);
    if (value !== 'custom') {
      this.paginator?.firstPage();
      this.reloadTableAndSummary();
    }
  }

  setStatusChip(value: PartnerHistoryStatusChip): void {
    this.statusChip.set(value);
    this.paginator?.firstPage();
    this.reloadTableOnly();
  }

  onSearch(event: Event): void {
    this.searchSubject$.next((event.target as HTMLInputElement).value);
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchSubject$.next('');
  }

  onPage(_ev: PageEvent): void {
    this.reloadTableOnly();
  }

  openDetail(row: Order): void {
    this.selectedOrder.set(row);
    this.detailNav?.open();
    this.detailLoading.set(true);
    this.ordersService.getOrder(row.id).subscribe({
      next: (o) => { this.selectedOrder.set(o as Order); this.detailLoading.set(false); },
      error: () => this.detailLoading.set(false),
    });
  }

  closeDetail(): void {
    this.detailNav?.close();
    this.selectedOrder.set(null);
  }

  itemLineTotal(item: Order['items'][number]): number {
    return (item.quantity ?? 0) * (item.unitPrice ?? item.price ?? 0);
  }

  getItemsSummary(order: Order): string {
    const items = order.items ?? [];
    if (items.length === 0) return '';
    const first = items[0].productName;
    return items.length > 1 ? `${first} +${items.length - 1}` : first;
  }

  totalItems(order: Order): number {
    return (order.items ?? []).reduce((s, it) => s + (it.quantity ?? 0), 0);
  }

  async runExport(format: 'excel' | 'pdf'): Promise<void> {
    if (this.totalElements() === 0 || this.exporting()) return;
    const base = this.buildBaseFilters();
    if (!base) return;
    this.exporting.set(true);
    try {
      const lang = this.translate.currentLang || 'fr';
      const blob = await firstValueFrom(this.ordersService.exportHistoryBlob(format, base, lang));
      const d = this.buildDateRange();
      const datePart = d ? `${d.from.slice(0, 10)}_${d.to.slice(0, 10)}` : 'export';
      const ext = format === 'excel' ? 'xlsx' : 'pdf';
      const name = `speedline-commandes-${datePart}.${ext}`;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('[OrderHistory] export', err);
    } finally {
      this.exporting.set(false);
    }
  }

  private buildBaseFilters(): PartnerHistoryBaseFilters | null {
    const dates = this.buildDateRange();
    if (!dates) return null;
    return {
      from:       dates.from,
      to:         dates.to,
      statusChip: this.statusChip(),
      search:     this.searchQuery(),
    };
  }

  private buildDateRange(): { from: string; to: string } | null {
    if (this.datePreset() === 'custom') {
      if (!this.customFrom || !this.customTo) return null;
      return { from: this.customFrom, to: this.customTo };
    }
    return computePresetRange(this.datePreset());
  }

  private reloadTableOnly(): void {
    const dates = this.buildDateRange();
    if (!dates || !this.paginator) return;
    this.dataSource.load({
      from:       dates.from,
      to:         dates.to,
      statusChip: this.statusChip(),
      search:     this.searchQuery(),
      page:       this.paginator.pageIndex,
      size:       this.paginator.pageSize,
    });
  }

  private reloadTableAndSummary(): void {
    const dates = this.buildDateRange();
    if (!dates || !this.paginator) return;
    this.ordersService.getPartnerOrderHistorySummary(dates.from, dates.to)
      .subscribe((s) => this.summary.set(s));
    this.dataSource.load({
      from:       dates.from,
      to:         dates.to,
      statusChip: this.statusChip(),
      search:     this.searchQuery(),
      page:       this.paginator.pageIndex,
      size:       this.paginator.pageSize,
    });
  }

  private applyPaginatorIntl(): void {
    if (!this.paginator) return;
    const t = (k: string, p?: object) => this.translate.instant(k, p);
    this.paginator._intl.itemsPerPageLabel = t('ORDERS.PAGINATOR_PER_PAGE');
    this.paginator._intl.nextPageLabel     = t('ORDERS.PAGINATOR_NEXT');
    this.paginator._intl.previousPageLabel = t('ORDERS.PAGINATOR_PREV');
    this.paginator._intl.firstPageLabel    = t('ORDERS.PAGINATOR_FIRST');
    this.paginator._intl.lastPageLabel     = t('ORDERS.PAGINATOR_LAST');
    this.paginator._intl.getRangeLabel = (page, pageSize, length) => {
      if (length === 0) return t('ORDERS.PAGINATOR_RANGE_ZERO');
      const start = page * pageSize + 1;
      const end   = Math.min((page + 1) * pageSize, length);
      return t('ORDERS.PAGINATOR_RANGE', { start, end, length });
    };
    this.paginator._intl.changes.next();
  }
}
