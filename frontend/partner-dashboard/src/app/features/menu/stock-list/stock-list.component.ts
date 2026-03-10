import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  signal,
  computed,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { StockService } from '../services/stock.service';
import {
  ProductStockDTO,
  UpdateStockRequest,
  BulkStockUpdateResult,
  StockStatus,
} from '../models/menu.models';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

type StatusFilter = '' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
    TranslateModule,
    LoadingSpinnerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './stock-list.component.html',
  styleUrls: ['./stock-list.component.scss'],
})
export class StockListComponent implements OnInit, OnDestroy {
  private stockService = inject(StockService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  loading = signal(false);
  bulkLoading = signal(false);
  editDialogOpen = signal(false);
  saving = signal(false);
  editingProduct = signal<ProductStockDTO | null>(null);
  bulkResult = signal<BulkStockUpdateResult | null>(null);

  totalCount      = signal(0);
  inStockCount    = signal(0);
  lowStockCount   = signal(0);
  outOfStockCount = signal(0);

  /** Products below threshold (for "Alertes stock" section). */
  lowStockItems = signal<ProductStockDTO[]>([]);
  alertesExpanded = signal(true);

  /** Inline quantity edit: productId when a cell is in edit mode. */
  editingQuantityProductId = signal<number | null>(null);
  restoringAll = signal(false);

  // Server-side pagination state
  totalItems = signal(0);
  pageIndex  = signal(0);
  pageSize   = signal(10);

  statusFilter = signal<StatusFilter>('');
  searchQuery  = signal('');
  hasActiveFilters = computed(() => !!this.searchQuery() || !!this.statusFilter());

  displayedColumns = [
    'photo', 'productName', 'categoryName', 'quantity', 'lowStockThreshold', 'stockStatus',
    'isAvailable', 'tracking', 'updatedAt', 'actions',
  ];
  dataSource = new MatTableDataSource<ProductStockDTO>([]);

  editForm: FormGroup;
  editProductId: number | null = null;

  @ViewChild('fileInput') fileInputRef?: ElementRef<HTMLInputElement>;

  constructor() {
    this.editForm = this.fb.group({
      quantity:          [0, [Validators.required, Validators.min(0)]],
      lowStockThreshold: [0, [Validators.min(0)]],
      isTrackingEnabled: [true],
    });
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadFiltered();
    this.loadLowStockAlertes();
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.pageIndex.set(0);
      this.loadFiltered();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadStats(): void {
    this.stockService.getStockStats().subscribe({
      next: (stats) => {
        this.totalCount.set(stats.total);
        this.inStockCount.set(stats.inStock);
        this.lowStockCount.set(stats.lowStock);
        this.outOfStockCount.set(stats.outOfStock);
      },
      error: () => {},
    });
  }

  loadLowStockAlertes(): void {
    this.stockService.getLowStock().subscribe({
      next: (list) => this.lowStockItems.set(list),
      error: () => {},
    });
  }

  private refreshAfterStockChange(): void {
    this.loadStats();
    this.loadFiltered();
    this.loadLowStockAlertes();
  }

  loadFiltered(): void {
    this.loading.set(true);
    this.stockService.getStockPage({
      search: this.searchQuery() || undefined,
      status: this.statusFilter() || undefined,
      page:   this.pageIndex(),
      size:   this.pageSize(),
    }).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalItems.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.LOAD_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadFiltered();
  }

  setStatusFilter(status: StatusFilter): void {
    this.statusFilter.set(status);
    this.pageIndex.set(0);
    this.loadStats();
    this.loadFiltered();
  }

  onSearchInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchQuery.set(val);
    this.searchSubject.next(val);
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchSubject.next('');
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('');
    this.pageIndex.set(0);
    this.loadStats();
    this.loadFiltered();
  }

  dismissBulkResult(): void {
    this.bulkResult.set(null);
  }

  startInlineQuantityEdit(row: ProductStockDTO): void {
    this.editingQuantityProductId.set(row.productId);
  }

  isEditingQuantity(row: ProductStockDTO): boolean {
    return this.editingQuantityProductId() === row.productId;
  }

  saveInlineQuantity(row: ProductStockDTO, newQty: string | number): void {
    if (this.editingQuantityProductId() !== row.productId) return; // cancelled (e.g. Escape)
    const qty = Math.max(0, Math.floor(Number(newQty)));
    this.editingQuantityProductId.set(null);
    this.stockService.updateStock(row.productId, { quantity: qty }).subscribe({
      next: (updated) => {
        const idx = this.dataSource.data.findIndex(d => d.productId === row.productId);
        if (idx >= 0) {
          this.dataSource.data = this.dataSource.data.slice();
          this.dataSource.data[idx] = updated;
        }
        this.refreshAfterStockChange();
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.SAVE_SUCCESS'),
          undefined,
          { duration: 2500 },
        );
      },
      error: () => {
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.SAVE_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  cancelInlineQuantityEdit(): void {
    this.editingQuantityProductId.set(null);
  }

  toggleTracking(row: ProductStockDTO): void {
    const next = !row.isTrackingEnabled;
    this.stockService.updateStock(row.productId, { isTrackingEnabled: next }).subscribe({
      next: (updated) => {
        const idx = this.dataSource.data.findIndex(d => d.productId === row.productId);
        if (idx >= 0) {
          this.dataSource.data = this.dataSource.data.slice();
          this.dataSource.data[idx] = updated;
        }
        this.refreshAfterStockChange();
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.SAVE_SUCCESS'),
          undefined,
          { duration: 2500 },
        );
      },
      error: () => {
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.SAVE_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  restoreAllOutOfStock(): void {
    this.restoringAll.set(true);
    this.stockService.restoreAllOutOfStock().subscribe({
      next: (res) => {
        this.restoringAll.set(false);
        this.refreshAfterStockChange();
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.RESTORE_ALL_SUCCESS', { count: res.restored }),
          undefined,
          { duration: 3000 },
        );
      },
      error: () => {
        this.restoringAll.set(false);
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.RESTORE_ALL_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  openEdit(row: ProductStockDTO): void {
    this.editingProduct.set(row);
    this.editProductId = row.productId;
    this.editForm.patchValue({
      quantity:          row.quantity,
      lowStockThreshold: row.lowStockThreshold,
      isTrackingEnabled: row.isTrackingEnabled,
    });
    this.editDialogOpen.set(true);
  }

  closeEdit(): void {
    this.editDialogOpen.set(false);
    this.editProductId = null;
    this.editingProduct.set(null);
  }

  saveStock(): void {
    if (!this.editForm.valid || this.editProductId == null) return;
    this.saving.set(true);
    const req: UpdateStockRequest = {
      quantity:          this.editForm.get('quantity')!.value,
      lowStockThreshold: this.editForm.get('lowStockThreshold')!.value ?? 0,
      isTrackingEnabled: this.editForm.get('isTrackingEnabled')!.value,
    };
    this.stockService.updateStock(this.editProductId, req).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeEdit();
        this.refreshAfterStockChange();
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.SAVE_SUCCESS'),
          undefined,
          { duration: 2500 },
        );
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.SAVE_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  exportExcel(): void {
    this.stockService.getAllFiltered({
      search: this.searchQuery() || undefined,
      status: this.statusFilter() || undefined,
    }).subscribe({
      next: (items) => {
        const headers = ['ID', 'Product', 'Category', 'Quantity', 'Status', 'Available', 'Threshold'];
        const rows = items.map(i => [
          i.productId,
          `"${(i.productName ?? '').replace(/"/g, '""')}"`,
          `"${(i.categoryName ?? '').replace(/"/g, '""')}"`,
          i.quantity,
          i.stockStatus ?? '',
          i.isAvailable ? 'Yes' : 'No',
          i.lowStockThreshold ?? 0,
        ]);
        const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `stock-export-${new Date().toISOString().slice(0, 10)}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.EXPORT_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  triggerFileInput(): void {
    this.fileInputRef?.nativeElement?.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.bulkLoading.set(true);
    this.stockService.bulkUpdateStock(file).subscribe({
      next: (result) => {
        this.bulkResult.set(result);
        this.bulkLoading.set(false);
        this.refreshAfterStockChange();
        input.value = '';
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.BULK_SUCCESS'),
          undefined,
          { duration: 3000 },
        );
      },
      error: () => {
        this.bulkLoading.set(false);
        input.value = '';
        this.snackBar.open(
          this.translate.instant('MENU.STOCK.BULK_ERROR'),
          undefined,
          { duration: 3000 },
        );
      },
    });
  }

  stockStatusIcon(status: StockStatus | undefined): string {
    if (!status || status === 'IN_STOCK') return 'check_circle';
    if (status === 'LOW_STOCK') return 'warning_amber';
    return 'cancel';
  }

  stockStatusLabel(status: StockStatus | undefined): string {
    if (!status) return this.translate.instant('MENU.STOCK.STATUS_IN_STOCK');
    const key = status === 'IN_STOCK'
      ? 'STATUS_IN_STOCK'
      : status === 'LOW_STOCK' ? 'STATUS_LOW_STOCK' : 'STATUS_OUT_OF_STOCK';
    return this.translate.instant('MENU.STOCK.' + key);
  }

  downloadTemplate(): void {
    const csv = 'productId,quantity\n1,10\n2,5';
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'stock-template.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  }
}
