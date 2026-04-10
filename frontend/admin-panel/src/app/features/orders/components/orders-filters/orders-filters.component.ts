import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { OrderFilters, OrderStatus } from '../../models/admin-order.model';

export type DateRange = 'today' | '7days' | '30days' | 'all';

@Component({
  selector: 'app-orders-filters',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    TranslateModule,
  ],
  template: `
    <div class="filters-wrapper">
      <!-- Date range tags -->
      <div class="date-tags">
        @for (tag of dateTags; track tag.value) {
          <button class="date-tag" [class.active]="selectedRange === tag.value"
                  (click)="onRangeChange(tag.value)">
            <span class="material-icons-round tag-icon">{{ tag.icon }}</span>
            {{ tag.label | translate }}
          </button>
        }
      </div>

      <!-- Filters row -->
      <div class="filters-row">
        <div class="search-box">
          <span class="material-icons-round search-icon">search</span>
          <input type="text"
                 [placeholder]="'orders.filters.searchPlaceholder' | translate"
                 [(ngModel)]="filters.search"
                 (ngModelChange)="emitFilters()">
        </div>

        <div class="select-filters">
          <mat-form-field appearance="outline" class="filter-select">
            <mat-label>{{ 'orders.filters.status' | translate }}</mat-label>
            <mat-select [(ngModel)]="filters.status" (ngModelChange)="emitFilters()">
              <mat-option value="">{{ 'orders.filters.allStatuses' | translate }}</mat-option>
              @for (s of statuses; track s) {
                <mat-option [value]="s">{{ 'orders.status.' + statusKeys[s] | translate }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-select">
            <mat-label>{{ 'orders.filters.payment' | translate }}</mat-label>
            <mat-select [(ngModel)]="filters.paymentMethod" (ngModelChange)="emitFilters()">
              <mat-option value="">{{ 'orders.filters.allPayments' | translate }}</mat-option>
              <mat-option value="CASH">{{ 'orders.payment.cash' | translate }}</mat-option>
              <mat-option value="CARD">{{ 'orders.payment.card' | translate }}</mat-option>
              <mat-option value="WALLET">{{ 'orders.payment.wallet' | translate }}</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./orders-filters.component.scss'],
})
export class OrdersFiltersComponent {
  @Output() filtersChanged = new EventEmitter<OrderFilters>();
  @Output() dateRangeChanged = new EventEmitter<DateRange>();

  filters: OrderFilters = {};
  selectedRange: DateRange = 'today';

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

  statusKeys: Record<string, string> = {
    PENDING: 'pending', CONFIRMED: 'confirmed', PREPARING: 'preparing',
    READY_FOR_PICKUP: 'ready', PICKED_UP: 'pickedUp',
    IN_DELIVERY: 'delivering', DELIVERED: 'delivered', CANCELLED: 'cancelled',
  };

  onRangeChange(range: DateRange): void {
    this.selectedRange = range;
    this.dateRangeChanged.emit(range);
  }

  emitFilters(): void {
    this.filtersChanged.emit({ ...this.filters });
  }
}
