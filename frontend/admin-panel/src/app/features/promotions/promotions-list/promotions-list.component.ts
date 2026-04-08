// Promotions List - ADM-005 compliant: copy code, progress bar, sort, filters, CSV, duplicate
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Clipboard, ClipboardModule } from '@angular/cdk/clipboard';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil, filter, switchMap } from 'rxjs/operators';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { PromotionsService } from '../services/promotions.service';
import { Promotion, PromotionStatistics } from '@core/models/promotion.model';
import { PartnersService } from '../../partners/services/partners.service';
import { Zone } from '@core/models/zone.model';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { ConfirmationDialogComponent } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-promotions-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ClipboardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatDialogModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    TranslateModule,
    ListPageComponent,
  ],
  templateUrl: './promotions-list.component.html',
  styleUrls: ['./promotions-list.component.scss'],
})
export class PromotionsListComponent implements OnInit, OnDestroy {
  private service   = inject(PromotionsService);
  private partners   = inject(PartnersService);
  private toastr    = inject(ToastrService);
  private translate = inject(TranslateService);
  private dialog    = inject(MatDialog);
  private clipboard = inject(Clipboard);
  private router    = inject(Router);
  private destroy$  = new Subject<void>();
  private searchInput$ = new Subject<string>();

  promotions = signal<Promotion[]>([]);
  stats = signal<PromotionStatistics | null>(null);
  loading = signal(false);

  searchText = '';
  selectedStatus = 'all';
  selectedTypes: string[] = [];
  selectedPartnerId = '';
  selectedZoneId = '';
  startFrom: Date | null = null;
  startTo:   Date | null = null;
  sortBy = 'created_at';
  sortDir: 'asc' | 'desc' = 'desc';
  itemsPerPage = 20;
  currentPage  = 1;
  totalItems   = 0;

  // Filter data
  partnersList: { id: number; name: string }[] = [];
  zonesList: Zone[] = [];

  Math = Math;

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());
    this.loadPromotions();
    this.loadStats();
    this.loadPartnersList();
    this.loadZonesList();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadPromotions(): void {
    this.loading.set(true);
    const status = this.selectedStatus !== 'all' ? this.selectedStatus : undefined;
    const type   = this.selectedTypes.length > 0 ? this.selectedTypes.join(',') : undefined;
    this.service.getPromotions({
      page:   this.currentPage - 1,
      size:   this.itemsPerPage,
      status,
      type,
      search: this.searchText || undefined,
      startFrom: this.startFrom ? this.startFrom.toISOString() : undefined,
      startTo:   this.startTo   ? this.startTo.toISOString()   : undefined,
      partnerId: this.selectedPartnerId || undefined,
      zoneId:    this.selectedZoneId || undefined,
      sortBy:  this.sortBy,
      sortDir: this.sortDir,
    }).subscribe({
      next: (res) => {
        this.promotions.set(res.content ?? []);
        this.totalItems = res.totalElements ?? 0;
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadStats(): void {
    this.service.getStatistics().pipe(takeUntil(this.destroy$)).subscribe({
      next: (s) => this.stats.set(s),
    });
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadPromotions();
  }

  onSearchChange(): void { this.searchInput$.next(this.searchText); }
  onStatusChange(): void { this.applyFilters(); }
  onTypeChange(): void   { this.applyFilters(); }
  onDateChange(): void   { this.applyFilters(); }
  onPartnerChange(): void { this.applyFilters(); }
  onZoneChange(): void    { this.applyFilters(); }

  private loadPartnersList(): void {
    this.partners.getPartners(0, 200).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        const list = res?.data?.content ?? res?.content ?? (Array.isArray(res) ? res : []);
        this.partnersList = list.map((p: any) => ({
          id: p.id,
          name: p.businessName || p.brandName || p.name || `Partner #${p.id}`,
        }));
      },
    });
  }

  private loadZonesList(): void {
    this.partners.getAllZones().pipe(takeUntil(this.destroy$)).subscribe({
      next: (zones) => { this.zonesList = zones; },
    });
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage  = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadPromotions();
  }

  // ── Sort ──────────────────────────────────────────────────────
  toggleSort(column: string): void {
    if (this.sortBy === column) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDir = 'asc';
    }
    this.loadPromotions();
  }

  get paginatedPromotions(): Promotion[] {
    return this.promotions();
  }

  // ── Helpers ──────────────────────────────────────────────────────

  getStatusLabel(status: string): string {
    return this.translate.instant(`promotions.statuses.${status}`) || status;
  }

  getTypeLabel(type: string): string {
    return this.translate.instant(`promotions.types.${type}`) || type;
  }

  formatValue(promo: Promotion): string {
    if (promo.type === 'PERCENTAGE') return promo.value + '%';
    if (promo.type === 'FIXED_AMOUNT') return promo.value + ' TND';
    return '-';
  }

  copyCode(code: string): void {
    this.clipboard.copy(code);
    this.toastr.success(this.translate.instant('promotions.codeCopied'));
  }

  getUsagePercent(promo: Promotion): number {
    if (!promo.usageLimitTotal) return 0;
    return Math.min(100, Math.round(((promo.usageCount || 0) / promo.usageLimitTotal) * 100));
  }

  getUsageColor(promo: Promotion): string {
    const pct = this.getUsagePercent(promo);
    if (pct >= 100) return 'red';
    if (pct >= 80)  return 'orange';
    return 'green';
  }

  // ── Actions ──────────────────────────────────────────────────────

  exportCsv(): void {
    const status = this.selectedStatus !== 'all' ? this.selectedStatus : undefined;
    const type   = this.selectedTypes.length > 0 ? this.selectedTypes.join(',') : undefined;
    this.service.exportCsv({ search: this.searchText || undefined, status, type });
  }

  togglePromotion(promo: Promotion): void {
    const isActivating = !promo.isActive;
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: isActivating ? 'promotions.confirmActivateTitle' : 'promotions.confirmDeactivateTitle',
        message: isActivating ? 'promotions.confirmActivate' : 'promotions.confirmDeactivate',
        confirmLabel: isActivating ? 'promotions.actions.activate' : 'promotions.actions.deactivate',
        cancelLabel: 'common.cancel',
        type: isActivating ? 'info' : 'warning',
        icon: isActivating ? 'play_circle' : 'pause_circle',
      },
    });
    dialogRef.afterClosed().pipe(
      filter((result) => result === true),
      switchMap(() => this.service.toggle(promo.id)),
      takeUntil(this.destroy$),
    ).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('promotions.toggleSuccess'));
        this.loadPromotions();
        this.loadStats();
      },
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }

  deletePromotion(promo: Promotion): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '420px',
      data: {
        title: 'promotions.confirmDeleteTitle',
        message: 'promotions.confirmDelete',
        confirmLabel: 'common.delete',
        cancelLabel: 'common.cancel',
        type: 'danger',
        icon: 'delete',
      },
    });
    dialogRef.afterClosed().pipe(
      filter((result) => result === true),
      switchMap(() => this.service.delete(promo.id)),
      takeUntil(this.destroy$),
    ).subscribe({
      next: () => {
        this.toastr.success(this.translate.instant('promotions.deleteSuccess'));
        this.loadPromotions();
        this.loadStats();
      },
      error: () => this.toastr.error(this.translate.instant('common.error')),
    });
  }
}
