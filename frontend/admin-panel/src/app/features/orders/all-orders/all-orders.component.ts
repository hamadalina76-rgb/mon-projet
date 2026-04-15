import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormGroup, FormControl } from '@angular/forms';
import { Subject, Observable, of, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil, switchMap, map, startWith, catchError } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminOrder, OrderFilters, ORDER_STATUS_CONFIG, OrderStatus, PaymentMethod, PaymentStatus } from '../models/admin-order.model';
import { OrdersService } from '../services/orders.service';
import { PartnersService } from '../../partners/services/partners.service';
import { CouriersService } from '../../users/couriers/services/couriers.service';
import { WebSocketService } from '@core/services/websocket.service';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';

type DateRange = 'today' | '7days' | '30days' | 'all' | 'custom';

interface ActiveFilter {
  key: string;
  label: string;
  icon: string;
}

interface QuickFilter {
  id: string;
  labelKey: string;
  icon: string;
  color?: 'warn' | 'default';
  filters: Partial<OrderFilters>;
  count: number | null;
}

@Component({
  selector: 'app-all-orders',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule, ReactiveFormsModule,
    MatFormFieldModule, MatSelectModule, MatTooltipModule,
    MatAutocompleteModule, MatInputModule, MatButtonModule, MatIconModule,
    MatDatepickerModule, MatNativeDateModule, MatMenuModule, MatSnackBarModule,
    TranslateModule, ListPageComponent,
  ],
  templateUrl: './all-orders.component.html',
  styleUrls: ['./all-orders.component.scss'],
})
export class AllOrdersComponent implements OnInit, OnDestroy {
  private ordersService = inject(OrdersService);
  private partnersService = inject(PartnersService);
  private couriersService = inject(CouriersService);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private wsService = inject(WebSocketService);
  private snackBar = inject(MatSnackBar);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();
  private wsSub?: Subscription;

  orders: AdminOrder[] = [];
  loading = false;
  totalItems = 0;
  currentPage = 1;
  itemsPerPage = 20;

  searchText = '';
  selectedStatus: OrderStatus | '' = '';
  selectedPayment: PaymentMethod | '' = '';
  selectedRange: DateRange = 'today';

  // ── Advanced filters ───────────────────────────────
  partnerSearchCtrl = new FormControl('');
  courierSearchCtrl = new FormControl('');
  filteredPartners$!: Observable<any[]>;
  filteredCouriers$!: Observable<any[]>;
  selectedPartnerId: number | null = null;
  selectedPartnerName = '';
  selectedCourierId: number | null = null;
  selectedCourierName = '';
  amountMin: number | null = null;
  amountMax: number | null = null;
  showAdvancedFilters = false;
  dateRange = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null),
  });

  statusConfig = ORDER_STATUS_CONFIG;

  // ── Quick filters ────────────────────────────────
  activeQuickFilter = 'all';

  quickFilters: QuickFilter[] = [
    { id: 'all',            labelKey: 'orders.quick.all',           icon: 'list_alt',        filters: {},                                                count: null },
    { id: 'pending_urgent', labelKey: 'orders.quick.pendingUrgent', icon: 'schedule',        color: 'warn', filters: { status: 'PENDING' as OrderStatus },  count: null },
    { id: 'no_courier',     labelKey: 'orders.quick.noCourier',     icon: 'person_off',      filters: { status: 'CONFIRMED' as OrderStatus },              count: null },
    { id: 'late_delivery',  labelKey: 'orders.quick.lateDelivery',  icon: 'timer_off',       color: 'warn', filters: { status: 'IN_DELIVERY' as OrderStatus }, count: null },
    { id: 'cancelled',      labelKey: 'orders.quick.cancelled',     icon: 'cancel',          filters: { status: 'CANCELLED' as OrderStatus },              count: null },
    { id: 'payment_failed', labelKey: 'orders.quick.paymentFailed', icon: 'credit_card_off', color: 'warn', filters: { paymentStatus: 'FAILED' as PaymentStatus }, count: null },
  ];

  dateTags: { value: DateRange; label: string; icon: string }[] = [
    { value: 'today',  label: 'orders.filters.today',      icon: 'today' },
    { value: '7days',  label: 'orders.filters.last7days',   icon: 'date_range' },
    { value: '30days', label: 'orders.filters.last30days',  icon: 'calendar_month' },
    { value: 'all',    label: 'orders.filters.all',          icon: 'all_inclusive' },
  ];

  statuses: OrderStatus[] = [
    'PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP',
    'PICKED_UP', 'IN_DELIVERY', 'DELIVERED', 'CANCELLED',
  ];

  private statusI18n: Record<string, string> = {
    PENDING: 'pending', CONFIRMED: 'confirmed', PREPARING: 'preparing',
    READY_FOR_PICKUP: 'ready', PICKED_UP: 'pickedUp',
    IN_DELIVERY: 'delivering', DELIVERED: 'delivered', CANCELLED: 'cancelled',
  };

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());

    this.filteredPartners$ = this.partnerSearchCtrl.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(val => {
        const term = typeof val === 'string' ? val : '';
        return this.partnersService.getPartners(0, 20, 'ACTIVE', term).pipe(
          map((res: any) => res.content || []),
          catchError(() => of([])),
        );
      }),
    );

    this.filteredCouriers$ = this.courierSearchCtrl.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(val => {
        const term = typeof val === 'string' ? val : '';
        return this.couriersService.getCouriers(0, 20, 'ACTIVE', term).pipe(
          map((res: any) => res.content || []),
          catchError(() => of([])),
        );
      }),
    );

    this.loadOrders();
    this.loadBadgeCounts();

    // WebSocket: refresh badge counts on any admin notification
    this.wsSub = this.wsService.onAdminNotification
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadBadgeCounts());
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(): void {
    this.searchInput$.next(this.searchText);
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  onPaymentChange(): void {
    this.applyFilters();
  }

  onRangeChange(range: DateRange): void {
    this.selectedRange = range;
    if (range !== 'custom') {
      this.dateRange.reset();
    }
    this.applyFilters();
  }

  selectQuickFilter(qf: QuickFilter): void {
    this.activeQuickFilter = qf.id;
    if (qf.id === 'all') {
      this.resetFilters();
      return;
    }
    // Apply quick-filter preset on top of current advanced filters
    this.selectedStatus = (qf.filters.status as OrderStatus) || '';
    this.selectedPayment = '';
    this.selectedRange = 'all';
    this.dateRange.reset();
    this.applyFilters();
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadOrders();
  }

  onRowClick(order: AdminOrder): void {
    this.router.navigate(['/orders', order.id]);
  }

  // ── Partner autocomplete ──────────────────────────
  displayPartner(partner: any): string {
    return partner?.businessName || partner?.name || '';
  }

  onPartnerSelected(event: any): void {
    const p = event.option.value;
    this.selectedPartnerId = p.id;
    this.selectedPartnerName = p.businessName || p.name || '';
    this.partnerSearchCtrl.setValue(this.selectedPartnerName);
    this.applyFilters();
  }

  clearPartner(): void {
    this.selectedPartnerId = null;
    this.selectedPartnerName = '';
    this.partnerSearchCtrl.setValue('');
    this.applyFilters();
  }

  // ── Courier autocomplete ──────────────────────────
  displayCourier(courier: any): string {
    return courier ? `${courier.firstName || ''} ${courier.lastName || ''}`.trim() : '';
  }

  onCourierSelected(event: any): void {
    const c = event.option.value;
    this.selectedCourierId = c.id;
    this.selectedCourierName = `${c.firstName || ''} ${c.lastName || ''}`.trim();
    this.courierSearchCtrl.setValue(this.selectedCourierName);
    this.applyFilters();
  }

  clearCourier(): void {
    this.selectedCourierId = null;
    this.selectedCourierName = '';
    this.courierSearchCtrl.setValue('');
    this.applyFilters();
  }

  // ── Amount ────────────────────────────────────────
  onAmountChange(): void {
    this.applyFilters();
  }

  // ── Custom date range ─────────────────────────────
  onDateRangeChange(): void {
    if (this.dateRange.value.start && this.dateRange.value.end) {
      this.selectedRange = 'custom';
      this.applyFilters();
    }
  }

  // ── Active filters / chips ────────────────────────
  get activeFilters(): ActiveFilter[] {
    const chips: ActiveFilter[] = [];
    if (this.searchText) {
      chips.push({ key: 'search', label: `"${this.searchText}"`, icon: 'search' });
    }
    if (this.selectedStatus) {
      chips.push({ key: 'status', label: this.getStatusLabel(this.selectedStatus), icon: 'flag' });
    }
    if (this.selectedPayment) {
      chips.push({ key: 'payment', label: this.getPaymentLabel(this.selectedPayment), icon: 'payment' });
    }
    if (this.selectedPartnerName) {
      chips.push({ key: 'partner', label: this.selectedPartnerName, icon: 'store' });
    }
    if (this.selectedCourierName) {
      chips.push({ key: 'courier', label: this.selectedCourierName, icon: 'delivery_dining' });
    }
    if (this.amountMin != null) {
      chips.push({ key: 'amountMin', label: `≥ ${this.amountMin} TND`, icon: 'attach_money' });
    }
    if (this.amountMax != null) {
      chips.push({ key: 'amountMax', label: `≤ ${this.amountMax} TND`, icon: 'attach_money' });
    }
    if (this.selectedRange === 'custom' && this.dateRange.value.start && this.dateRange.value.end) {
      const fmt = (d: Date) => d.toLocaleDateString();
      chips.push({ key: 'dateRange', label: `${fmt(this.dateRange.value.start)} — ${fmt(this.dateRange.value.end)}`, icon: 'date_range' });
    }
    return chips;
  }

  removeFilter(key: string): void {
    switch (key) {
      case 'search': this.searchText = ''; break;
      case 'status': this.selectedStatus = ''; break;
      case 'payment': this.selectedPayment = ''; break;
      case 'partner': this.clearPartner(); return;
      case 'courier': this.clearCourier(); return;
      case 'amountMin': this.amountMin = null; break;
      case 'amountMax': this.amountMax = null; break;
      case 'dateRange':
        this.dateRange.reset();
        this.selectedRange = 'today';
        break;
    }
    this.applyFilters();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedStatus = '';
    this.selectedPayment = '';
    this.selectedRange = 'today';
    this.selectedPartnerId = null;
    this.selectedPartnerName = '';
    this.selectedCourierId = null;
    this.selectedCourierName = '';
    this.partnerSearchCtrl.setValue('');
    this.courierSearchCtrl.setValue('');
    this.amountMin = null;
    this.amountMax = null;
    this.dateRange.reset();
    this.applyFilters();
  }

  get hasActiveAdvancedFilters(): boolean {
    return this.activeFilters.length > 0;
  }

  getStatusLabel(status: string): string {
    const key = this.statusI18n[status] || status;
    return this.translate.instant('orders.status.' + key);
  }

  getStatusClass(status: string): string {
    return 'status-' + (status || '').toLowerCase().replace(/_/g, '-');
  }

  getPaymentLabel(method: string): string {
    return this.translate.instant('orders.payment.' + (method || '').toLowerCase());
  }

  private applyFilters(): void {
    this.currentPage = 1;
    this.loadOrders();
  }

  private loadOrders(): void {
    this.loading = true;
    const filters: OrderFilters = {
      ...this.getDateFilters(),
      ...(this.searchText ? { search: this.searchText } : {}),
      ...(this.selectedStatus ? { status: this.selectedStatus } : {}),
      ...(this.selectedPayment ? { paymentMethod: this.selectedPayment } : {}),
      ...(this.selectedPartnerId ? { partnerId: this.selectedPartnerId } : {}),
      ...(this.selectedCourierId ? { courierId: this.selectedCourierId } : {}),
      ...(this.amountMin != null ? { amountMin: this.amountMin } : {}),
      ...(this.amountMax != null ? { amountMax: this.amountMax } : {}),
    };
    // Merge quick-filter paymentStatus if active
    const activeQf = this.quickFilters.find(q => q.id === this.activeQuickFilter);
    if (activeQf?.filters.paymentStatus) {
      filters.paymentStatus = activeQf.filters.paymentStatus;
    }
    this.ordersService.getOrders(this.currentPage - 1, this.itemsPerPage, filters, 'createdAt,desc').subscribe({
      next: (res) => {
        this.orders = res.content || [];
        this.totalItems = res.totalElements || 0;
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  private getDateFilters(): { startDate?: string; endDate?: string } {
    if (this.selectedRange === 'custom' && this.dateRange.value.start && this.dateRange.value.end) {
      const s = this.dateRange.value.start;
      const e = this.dateRange.value.end;
      const start = new Date(s.getFullYear(), s.getMonth(), s.getDate(), 0, 0, 0);
      const end = new Date(e.getFullYear(), e.getMonth(), e.getDate(), 23, 59, 59);
      return { startDate: this.toLocalISO(start), endDate: this.toLocalISO(end) };
    }
    const now = new Date();
    const endOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
    const endStr = this.toLocalISO(endOfDay);
    switch (this.selectedRange) {
      case 'today': {
        const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        return { startDate: this.toLocalISO(start), endDate: endStr };
      }
      case '7days': {
        const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 6, 0, 0, 0);
        return { startDate: this.toLocalISO(start), endDate: endStr };
      }
      case '30days': {
        const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 29, 0, 0, 0);
        return { startDate: this.toLocalISO(start), endDate: endStr };
      }
      default:
        return {};
    }
  }

  private toLocalISO(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  private loadBadgeCounts(): void {
    this.quickFilters.forEach(qf => {
      if (qf.id === 'all') return;
      const filters: OrderFilters = { ...qf.filters };
      this.ordersService.getOrders(0, 1, filters).subscribe({
        next: (res) => { qf.count = res.totalElements || 0; },
        error: () => { qf.count = 0; },
      });
    });
  }

  // ── Export ────────────────────────────────────────
  exportLoading = false;

  exportExcel(): void {
    this.doExport('excel');
  }

  exportPdf(): void {
    this.doExport('pdf');
  }

  private doExport(format: 'excel' | 'pdf'): void {
    this.exportLoading = true;
    const filters = this.buildCurrentFilters();
    const lang = this.translate.currentLang || 'fr';
    const obs = format === 'excel'
      ? this.ordersService.exportExcel(filters, lang)
      : this.ordersService.exportPdf(filters, lang);

    obs.subscribe({
      next: (blob) => {
        const ext = format === 'excel' ? 'xlsx' : 'pdf';
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `commandes-${new Date().toISOString().slice(0, 10)}.${ext}`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportLoading = false;
      },
      error: () => {
        this.exportLoading = false;
        this.snackBar.open(this.translate.instant('orders.export.error'), '✕', { duration: 3000 });
      },
    });
  }

  private buildCurrentFilters(): OrderFilters {
    const filters: OrderFilters = {
      ...this.getDateFilters(),
      ...(this.searchText ? { search: this.searchText } : {}),
      ...(this.selectedStatus ? { status: this.selectedStatus } : {}),
      ...(this.selectedPayment ? { paymentMethod: this.selectedPayment } : {}),
      ...(this.selectedPartnerId ? { partnerId: this.selectedPartnerId } : {}),
      ...(this.selectedCourierId ? { courierId: this.selectedCourierId } : {}),
      ...(this.amountMin != null ? { amountMin: this.amountMin } : {}),
      ...(this.amountMax != null ? { amountMax: this.amountMax } : {}),
    };
    const activeQf = this.quickFilters.find(q => q.id === this.activeQuickFilter);
    if (activeQf?.filters.paymentStatus) {
      filters.paymentStatus = activeQf.filters.paymentStatus;
    }
    return filters;
  }
}
