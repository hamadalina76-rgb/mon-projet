import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { CustomersService, Customer } from '../services/customers.service';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';

@Component({
  selector: 'app-customers-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    TranslateModule,
    ListPageComponent,
  ],
  templateUrl: './customers-list.component.html',
  styleUrls: ['./customers-list.component.scss'],
})
export class CustomersListComponent implements OnInit, OnDestroy {
  private customersService = inject(CustomersService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();

  customers = signal<Customer[]>([]);
  loading = signal(false);

  selectedStatus = 'ALL';
  itemsPerPage = 20;
  currentPage = 1;
  totalItems = 0;
  sortBy = 'createdAt';
  sortDir = 'DESC';
  searchFilter = '';
  dateFrom = '';
  dateTo = '';

  chartData = signal<{ labels: string[]; values: number[] }>({ labels: [], values: [] });

  get chartMax(): number {
    const v = this.chartData().values;
    return v.length ? Math.max(...v) : 1;
  }

  ngOnInit(): void {
    this.loadCustomers();
    this.loadChartData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCustomers(): void {
    this.loading.set(true);
    const status = this.selectedStatus !== 'ALL' ? this.selectedStatus : undefined;
    this.customersService
      .getCustomers(this.currentPage - 1, this.itemsPerPage, {
        sort: this.sortBy,
        sortDir: this.sortDir,
        status,
        search: this.searchFilter || undefined,
        dateFrom: this.dateFrom || undefined,
        dateTo: this.dateTo || undefined,
      })
      .subscribe({
        next: (res) => {
          this.customers.set(res.content ?? []);
          this.totalItems = res.totalElements ?? 0;
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  loadChartData(): void {
    this.customersService.getNewCustomersByMonth().subscribe({
      next: (data) => {
        const labels = Object.keys(data).sort();
        const values = labels.map((k) => data[k]);
        this.chartData.set({ labels, values });
      },
    });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadCustomers();
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadCustomers();
  }

  get paginatedCustomers(): Customer[] {
    return this.customers();
  }

  getFullName(c: Customer): string {
    const first = c.firstName ?? '';
    const last = c.lastName ?? '';
    return [first, last].filter(Boolean).join(' ') || c.email || '-';
  }

  getCity(c: Customer): string {
    return c.defaultAddress?.city ?? '-';
  }

  getStatusLabel(status: string): string {
    const key = `users.customers.status.${(status || '').toLowerCase()}`;
    const t = this.translate.instant(key);
    return t !== key ? t : status;
  }

  exportCustomers(format: 'csv' | 'xlsx'): void {
    const status = this.selectedStatus !== 'ALL' ? this.selectedStatus : undefined;
    this.customersService
      .exportCustomers(format, { status, search: this.searchFilter || undefined, dateFrom: this.dateFrom || undefined, dateTo: this.dateTo || undefined })
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `clients_export.${format === 'xlsx' ? 'xlsx' : 'csv'}`;
          a.click();
          URL.revokeObjectURL(url);
          this.toastr.success(this.translate.instant('common.exportSuccess'));
        },
        error: () => this.toastr.error(this.translate.instant('common.exportError')),
      });
  }

  blockCustomer(c: Customer): void {
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
            this.loadCustomers();
          },
          error: () => this.toastr.error(this.translate.instant('common.error')),
        });
      }
    });
  }

  unblockCustomer(c: Customer): void {
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
            this.loadCustomers();
          },
          error: () => this.toastr.error(this.translate.instant('common.error')),
        });
      }
    });
  }

  deleteCustomer(c: Customer): void {
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
            this.loadCustomers();
          },
          error: () => this.toastr.error(this.translate.instant('common.error')),
        });
      }
    });
  }
}
