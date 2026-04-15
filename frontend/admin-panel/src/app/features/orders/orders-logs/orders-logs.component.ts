import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { OrdersService } from '../services/orders.service';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';

interface LogEntry {
  id: number;
  orderId: number;
  orderNumber: string;
  status: string;
  previousStatus: string;
  description: string;
  notes: string;
  actorType: string;
  actorId: number;
  updatedBy: string;
  timestamp: string;
}

@Component({
  selector: 'app-orders-logs',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatFormFieldModule, MatSelectModule, MatInputModule,
    MatButtonModule, MatIconModule, MatTooltipModule,
    TranslateModule, ListPageComponent,
  ],
  templateUrl: './orders-logs.component.html',
  styleUrls: ['./orders-logs.component.scss'],
})
export class OrdersLogsComponent implements OnInit, OnDestroy {
  private ordersService = inject(OrdersService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();

  logs: LogEntry[] = [];
  loading = false;
  totalItems = 0;
  currentPage = 1;
  itemsPerPage = 20;

  // Filters
  selectedActorType = '';
  selectedStatus = '';
  searchOrderId = '';

  actorTypes = ['SYSTEM', 'CUSTOMER', 'PARTNER', 'COURIER', 'ADMIN'];
  statuses = [
    'PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP',
    'PICKED_UP', 'IN_DELIVERY', 'DELIVERED', 'CANCELLED',
  ];

  ngOnInit(): void {
    this.loadLogs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadLogs();
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadLogs();
  }

  resetFilters(): void {
    this.selectedActorType = '';
    this.selectedStatus = '';
    this.searchOrderId = '';
    this.currentPage = 1;
    this.loadLogs();
  }

  private loadLogs(): void {
    this.loading = true;
    const filters: any = {};
    if (this.selectedActorType) filters.actorType = this.selectedActorType;
    if (this.selectedStatus) filters.status = this.selectedStatus;
    if (this.searchOrderId) filters.orderId = this.searchOrderId;

    this.ordersService.getLogs(this.currentPage - 1, this.itemsPerPage, filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.logs = res.content || [];
          this.totalItems = res.totalElements || 0;
          this.loading = false;
        },
        error: () => { this.loading = false; },
      });
  }

  getActorIcon(type: string): string {
    switch (type) {
      case 'ADMIN':    return 'admin_panel_settings';
      case 'CUSTOMER': return 'person';
      case 'PARTNER':  return 'store';
      case 'COURIER':  return 'delivery_dining';
      case 'SYSTEM':   return 'smart_toy';
      default:         return 'help_outline';
    }
  }

  getStatusClass(status: string): string {
    return 'status-' + (status || '').toLowerCase().replace(/_/g, '-');
  }
}
