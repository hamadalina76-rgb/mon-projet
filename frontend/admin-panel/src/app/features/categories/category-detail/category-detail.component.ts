import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { ToastrService } from 'ngx-toastr';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartData, ChartOptions, registerables } from 'chart.js';
import { CategoriesService } from '../services/categories.service';
import { Category, CategoryStats, AuditLogEntry } from '@core/models/category.model';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '@shared/components/confirmation-dialog/confirmation-dialog.component';

Chart.register(...registerables);

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTabsModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    TranslateModule,
    BaseChartDirective,
  ],
  templateUrl: './category-detail.component.html',
  styleUrls: ['./category-detail.component.scss'],
})
export class CategoryDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private categoriesService = inject(CategoriesService);
  private dialog = inject(MatDialog);
  private toastr = inject(ToastrService);
  private translate = inject(TranslateService);
  private destroy$ = new Subject<void>();

  category = signal<Category | null>(null);
  loading = signal(false);
  categoryId = signal<number | null>(null);
  selectedTabIndex = signal(0);

  currentLang = signal(this.translate.currentLang || 'fr');
  isRtl = computed(() => this.currentLang() === 'ar');

  stats = signal<CategoryStats | null>(null);
  statsLoading = signal(false);
  auditTrail = signal<AuditLogEntry[]>([]);
  auditLoading = signal(false);
  auditPage = signal(0);
  totalAuditElements = signal(0);
  readonly PAGE_SIZE = 5;

  chartData = computed<ChartData<'bar'>>(() => {
    const s = this.stats();
    this.currentLang(); // reactive dependency — re-compute label on language change
    const label = this.translate.instant('categories.chartOrdersLabel');
    if (!s?.dailyOrders?.length) {
      return { labels: [], datasets: [{ data: [], label }] };
    }
    return {
      labels: s.dailyOrders.map(d => d.day),
      datasets: [{
        data: s.dailyOrders.map(d => d.orders),
        label,
        backgroundColor: 'rgba(236, 19, 30, 0.2)',
        borderColor: '#EC131E',
        borderWidth: 2,
        borderRadius: 4,
        borderSkipped: false,
      }]
    };
  });

  chartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx: import('chart.js').TooltipItem<'bar'>) =>
            `${ctx.parsed.y} ${this.translate.instant('categories.chartOrdersLabel').toLowerCase()}`
        }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#9ca3af', font: { size: 11 } }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(0,0,0,0.05)' },
        ticks: { color: '#9ca3af', font: { size: 11 } }
      }
    }
  };

  /** Le backend renvoie déjà la bonne page — on expose directement le signal. */
  paginatedAudit = computed(() => this.auditTrail());

  totalAuditPages = computed(() => Math.ceil(this.totalAuditElements() / this.PAGE_SIZE));

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const numId = Number(id);
      this.categoryId.set(numId);
      this.loadCategory(numId);
      this.loadStats(numId);
      this.loadAuditTrail(numId);
    } else {
      this.router.navigate(['/categories']);
    }
    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe(e => this.currentLang.set(e.lang));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCategory(id: number): void {
    this.loading.set(true);
    this.categoriesService.getCategoryById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (category) => {
          this.category.set(category);
          this.loading.set(false);
        },
        error: () => {
          this.toastr.error(this.translate.instant('categories.loadError'));
          this.loading.set(false);
          this.router.navigate(['/categories']);
        }
      });
  }

  private loadStats(id: number): void {
    this.statsLoading.set(true);
    this.categoriesService.getCategoryStats(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (s) => { this.stats.set(s); this.statsLoading.set(false); },
        error: () => this.statsLoading.set(false)
      });
  }

  private loadAuditTrail(id: number): void {
    this.auditLoading.set(true);
    this.categoriesService.getCategoryAuditTrail(id, this.auditPage(), this.PAGE_SIZE)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ content, totalElements }) => {
          this.auditTrail.set(content);
          this.totalAuditElements.set(totalElements);
          this.auditLoading.set(false);
        },
        error: () => this.auditLoading.set(false)
      });
  }

  onTabChange(index: number): void { this.selectedTabIndex.set(index); }

  goBack(): void { this.router.navigate(['/categories']); }

  editCategory(): void {
    const id  = this.categoryId();
    const cat = this.category();
    if (!id) return;
    if (cat?.parentId) {
      this.router.navigate(['/categories/sub', id, 'edit']);
    } else {
      this.router.navigate(['/categories', id, 'edit']);
    }
  }

  exportReport(): void {
    const id = this.categoryId();
    if (!id) return;
    this.categoriesService.exportCategoryReport(id);
    this.toastr.success(this.translate.instant('categories.exportInProgress'));
  }

  prevAuditPage(): void {
    if (this.auditPage() > 0) {
      this.auditPage.update(p => p - 1);
      const id = this.categoryId();
      if (id) this.loadAuditTrail(id);
    }
  }

  nextAuditPage(): void {
    if (this.auditPage() < this.totalAuditPages() - 1) {
      this.auditPage.update(p => p + 1);
      const id = this.categoryId();
      if (id) this.loadAuditTrail(id);
    }
  }

  toggleStatus(): void {
    const id = this.categoryId();
    if (!id) return;
    this.categoriesService.toggleCategoryStatus(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.category.set(updated);
          this.toastr.success(this.translate.instant('categories.categoryStatusUpdated'));
        },
        error: () => this.toastr.error(this.translate.instant('categories.statusUpdateError'))
      });
  }

  deleteCategory(): void {
    const cat = this.category();
    const id = this.categoryId();
    if (!cat || !id) return;

    const dialogData: ConfirmationDialogData = {
      title: this.translate.instant('categories.deleteTitle'),
      message: this.translate.instant('categories.deleteMessage', { name: this.getDisplayName(cat) }),
      confirmLabel: this.translate.instant('categories.delete'),
      cancelLabel: this.translate.instant('categories.cancelBtn'),
      type: 'danger'
    };

    this.dialog.open(ConfirmationDialogComponent, { width: '420px', data: dialogData })
      .afterClosed().subscribe(result => {
        if (result) {
          this.categoriesService.deleteCategory(id)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: () => {
                this.toastr.success(this.translate.instant('categories.categoryDeleted'));
                this.router.navigate(['/categories']);
              },
              error: () => this.toastr.error(this.translate.instant('categories.deleteError'))
            });
        }
      });
  }

  getDisplayName(cat: Category): string {
    const lang = this.currentLang();
    return cat.nameI18n[lang]
      || cat.nameI18n['fr']
      || cat.nameI18n['en']
      || Object.values(cat.nameI18n)[0]
      || '';
  }

  getNameEntries(): { key: string; value: string }[] {
    const category = this.category();
    if (!category?.nameI18n) return [];
    return Object.entries(category.nameI18n)
      .filter(([, value]) => value)
      .map(([key, value]) => ({ key, value: value! }));
  }

  getActionClass(action: string): string {
    const map: Record<string, string> = {
      CREATE: 'action-create',
      UPDATE: 'action-update',
      DELETE: 'action-delete',
      ACTIVATE: 'action-activate',
      DEACTIVATE: 'action-deactivate',
    };
    return map[action] ?? 'action-default';
  }

  getActionLabel(action: string): string {
    const keyMap: Record<string, string> = {
      CREATE: 'categories.actionCreate',
      UPDATE: 'categories.actionUpdate',
      DELETE: 'categories.actionDelete',
      ACTIVATE: 'categories.actionActivate',
      DEACTIVATE: 'categories.actionDeactivate',
    };
    const key = keyMap[action];
    return key ? this.translate.instant(key) : action;
  }

  formatTrend(value: number | undefined): string {
    if (value == null) return '0%';
    return `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`;
  }
}