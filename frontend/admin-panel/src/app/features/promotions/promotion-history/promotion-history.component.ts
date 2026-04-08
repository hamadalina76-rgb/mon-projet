import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ListPageComponent } from '@shared/components/list-page/list-page.component';
import { PromotionsService } from '../services/promotions.service';
import { PromotionAuditLog, AuditAction } from '@core/models/promotion.model';

@Component({
  selector: 'app-promotion-history',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, ListPageComponent, DatePipe],
  templateUrl: './promotion-history.component.html',
  styleUrls: ['./promotion-history.component.scss'],
})
export class PromotionHistoryComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private promotionsService = inject(PromotionsService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  promotionId!: number;
  promotionCode = '';
  logs: PromotionAuditLog[] = [];
  loading = false;
  totalElements = 0;
  currentPage = 1;
  itemsPerPage = 20;

  // Filters
  searchText = '';
  selectedAction = '';

  actionOptions: AuditAction[] = [
    'CREATED', 'UPDATED', 'DELETED',
    'ACTIVATED', 'DEACTIVATED', 'TOGGLED',
    'APPLIED', 'REVOKED', 'EXPIRED',
  ];

  get hasActiveFilters(): boolean {
    return !!this.searchText || !!this.selectedAction;
  }

  Math = Math;

  ngOnInit(): void {
    this.promotionId = +this.route.snapshot.paramMap.get('id')!;
    this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.applyFilters());
    this.loadHistory();
    this.loadPromotionInfo();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadHistory(): void {
    this.loading = true;
    this.promotionsService
      .getHistory(
        this.promotionId,
        this.currentPage - 1,
        this.itemsPerPage,
        this.selectedAction || undefined,
        this.searchText || undefined,
      )
      .subscribe({
        next: (res) => {
          this.logs = res.content;
          this.totalElements = res.totalElements;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
        },
      });
  }

  private loadPromotionInfo(): void {
    this.promotionsService.getById(this.promotionId).subscribe({
      next: (res) => {
        this.promotionCode = res.promotion?.code ?? '';
      },
    });
  }

  onSearchChange(): void {
    this.searchInput$.next(this.searchText);
  }

  onActionChange(): void {
    this.applyFilters();
  }

  applyFilters(): void {
    this.currentPage = 1;
    this.loadHistory();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedAction = '';
    this.currentPage = 1;
    this.loadHistory();
  }

  onPageChange(event: { page: number; pageSize: number }): void {
    this.currentPage = event.page;
    this.itemsPerPage = event.pageSize;
    this.loadHistory();
  }

  getActionLabel(action: string): string {
    return this.translate.instant('promotions.history.actions.' + action);
  }
}
