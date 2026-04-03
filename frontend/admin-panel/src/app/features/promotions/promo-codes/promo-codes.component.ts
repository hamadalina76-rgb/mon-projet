// promo-codes/promo-codes.component.ts
// This component is an entry-point alias that redirects to the main promotions list
// filtered to promo-code type promotions. The full list and form are in promotions-list.
import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';

import { PromotionsService } from '../services/promotions.service';
import { Promotion, PromotionStatus, PromotionType } from '@core/models/promotion.model';

@Component({
  selector: 'app-promo-codes',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatInputModule,
    MatFormFieldModule, MatSelectModule, MatTableModule, MatPaginatorModule,
    MatChipsModule, MatTooltipModule, MatProgressSpinnerModule, MatMenuModule,
    MatDividerModule, TranslateModule,
  ],
  templateUrl: './promo-codes.component.html',
  styleUrls: ['./promo-codes.component.scss'],
})
export class PromoCodesComponent implements OnInit {
  private router   = inject(Router);
  private service  = inject(PromotionsService);
  private destroy$ = new Subject<void>();
  private search$  = new Subject<string>();

  promotions: Promotion[] = [];
  loading = false;
  searchTerm     = '';
  selectedStatus = '';
  page           = 0;
  pageSize       = 20;
  totalElements  = 0;

  readonly displayedColumns = ['code', 'name', 'type', 'value', 'usage', 'dates', 'status', 'actions'];

  readonly statuses: { value: PromotionStatus | ''; label: string }[] = [
    { value: '', label: 'Tous' },
    { value: 'ACTIVE',    label: 'Actif' },
    { value: 'INACTIVE',  label: 'Inactif' },
    { value: 'SCHEDULED', label: 'Planifié' },
    { value: 'EXPIRED',   label: 'Expiré' },
  ];

  ngOnInit(): void {
    this.search$
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.page = 0; this.load(); });
    this.load();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.loading = true;
    this.service.getPromotions({
      search: this.searchTerm || undefined,
      status: this.selectedStatus || undefined,
      page: this.page, size: this.pageSize,
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (r) => { this.promotions = r.content; this.totalElements = r.totalElements; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  onSearchChange(): void  { this.search$.next(this.searchTerm); }
  onFilterChange(): void  { this.page = 0; this.load(); }
  onPageChange(e: PageEvent): void { this.page = e.pageIndex; this.pageSize = e.pageSize; this.load(); }

  create(): void             { this.router.navigate(['/promotions/new']); }
  edit(p: Promotion): void   { this.router.navigate(['/promotions', p.id, 'edit']); }
  analytics(p: Promotion): void { this.router.navigate(['/promotions', p.id, 'analytics']); }

  toggle(p: Promotion): void {
    this.service.toggle(p.id).pipe(takeUntil(this.destroy$)).subscribe(() => this.load());
  }

  formatDiscount(p: Promotion): string {
    if (p.type === 'PERCENTAGE')    return `${p.value}%`;
    if (p.type === 'FREE_DELIVERY') return 'Livraison gratuite';
    return `${p.value} TND`;
  }

  statusClass(s: PromotionStatus): string {
    return { ACTIVE: 'chip-active', INACTIVE: 'chip-inactive', SCHEDULED: 'chip-scheduled', EXPIRED: 'chip-expired' }[s] ?? '';
  }

  statusLabel(s: PromotionStatus): string {
    return { ACTIVE: 'Actif', INACTIVE: 'Inactif', SCHEDULED: 'Planifié', EXPIRED: 'Expiré' }[s] ?? s;
  }
}
