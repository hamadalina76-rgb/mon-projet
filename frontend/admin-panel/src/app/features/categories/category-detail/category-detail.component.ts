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
import { Category, CategoryBusinessType, CategoryStats, AuditLogEntry } from '@core/models/category.model';
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

  stats = signal<CategoryStats | null>(null);
  statsLoading = signal(false);
  auditTrail = signal<AuditLogEntry[]>([]);
  auditLoading = signal(false);
  auditPage = signal(0);
  readonly PAGE_SIZE = 5;

  chartData = computed<ChartData<'bar'>>(() => {
    const s = this.stats();
    if (!s?.dailyOrders?.length) {
      return { labels: [], datasets: [{ data: [], label: 'Commandes' }] };
    }
    return {
      labels: s.dailyOrders.map(d => d.day),
      datasets: [{
        data: s.dailyOrders.map(d => d.orders),
        label: 'Commandes',
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
          label: (ctx: import('chart.js').TooltipItem<'bar'>) => `${ctx.parsed.y} commandes`
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

  paginatedAudit = computed(() => {
    const all = this.auditTrail();
    const page = this.auditPage();
    return all.slice(page * this.PAGE_SIZE, (page + 1) * this.PAGE_SIZE);
  });

  totalAuditPages = computed(() => Math.ceil(this.auditTrail().length / this.PAGE_SIZE));

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
          this.toastr.error('Impossible de charger la catégorie');
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
    this.categoriesService.getCategoryAuditTrail(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (entries) => { this.auditTrail.set(entries); this.auditLoading.set(false); },
        error: () => this.auditLoading.set(false)
      });
  }

  onTabChange(index: number): void { this.selectedTabIndex.set(index); }

  goBack(): void { this.router.navigate(['/categories']); }

  editCategory(): void {
    const id = this.categoryId();
    if (id) this.router.navigate(['/categories', id, 'edit']);
  }

  exportReport(): void {
    const cat = this.category();
    const s = this.stats();
    if (!cat) return;

    const name = this.getDisplayName(cat);
    const now = new Date().toLocaleDateString('fr-FR').replace(/\//g, '-');

    // ── Contenu CSV ──────────────────────────────────────────────────────────
    const lines: string[] = [];

    // Infos générales
    lines.push('=== RAPPORT CATÉGORIE ===');
    lines.push(`Catégorie;${name}`);
    lines.push(`Slug;${cat.slug ?? ''}`);
    lines.push(`Type métier;${this.getBusinessTypeLabel(cat.categoryBusinessType)}`);
    lines.push(`Statut;${cat.isActive ? 'Active' : 'Inactive'}`);
    lines.push(`Mise en avant;${cat.isFeatured ? 'Oui' : 'Non'}`);
    lines.push(`Créée le;${cat.createdAt ? new Date(cat.createdAt).toLocaleDateString('fr-FR') : ''}`);
    lines.push('');

    // Stats
    if (s) {
      lines.push('=== STATISTIQUES (30 JOURS) ===');
      lines.push(`Produits liés;${s.productCount}`);
      lines.push(`Partenaires actifs;${s.partnerCount}`);
      lines.push(`Commandes (30j);${s.ordersLast30Days}`);
      lines.push(`Tendance commandes;${s.orderTrendPercent >= 0 ? '+' : ''}${s.orderTrendPercent}%`);
      lines.push(`TOP Catégorie;${s.topCategory ? 'Oui' : 'Non'}`);
      lines.push('');

      // Graphique journalier
      if (s.dailyOrders?.length) {
        lines.push('=== COMMANDES JOURNALIÈRES ===');
        lines.push('Date;Commandes');
        s.dailyOrders.forEach(d => lines.push(`${d.day};${d.orders}`));
        lines.push('');
      }
    }

    // Audit trail
    const audit = this.auditTrail();
    if (audit.length) {
      lines.push('=== JOURNAL D\'AUDIT ===');
      lines.push('Date;Utilisateur;Rôle;Action;Détails;Statut');
      audit.forEach(e => {
        const ts = new Date(e.timestamp).toLocaleDateString('fr-FR') + ' ' + new Date(e.timestamp).toLocaleTimeString('fr-FR');
        lines.push(`${ts};${e.adminName};${e.adminRole};${this.getActionLabel(e.action)};${e.changesAfter};${e.status}`);
      });
    }

    // Téléchargement
    const csv = '\uFEFF' + lines.join('\n'); // BOM pour Excel
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `rapport-categorie-${cat.slug ?? cat.id}-${now}.csv`;
    link.click();
    URL.revokeObjectURL(url);

    this.toastr.success('Rapport exporté avec succès');
  }

  prevAuditPage(): void {
    if (this.auditPage() > 0) this.auditPage.update(p => p - 1);
  }

  nextAuditPage(): void {
    if (this.auditPage() < this.totalAuditPages() - 1) this.auditPage.update(p => p + 1);
  }

  toggleStatus(): void {
    const id = this.categoryId();
    if (!id) return;
    this.categoriesService.toggleCategoryStatus(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.category.set(updated);
          this.toastr.success('Statut mis à jour');
        },
        error: () => this.toastr.error('Erreur lors de la mise à jour')
      });
  }

  deleteCategory(): void {
    const cat = this.category();
    const id = this.categoryId();
    if (!cat || !id) return;

    const dialogData: ConfirmationDialogData = {
      title: 'Supprimer la catégorie',
      message: `Êtes-vous sûr de vouloir supprimer "${this.getDisplayName(cat)}" ?`,
      confirmLabel: 'Supprimer',
      cancelLabel: 'Annuler',
      type: 'danger'
    };

    this.dialog.open(ConfirmationDialogComponent, { width: '420px', data: dialogData })
      .afterClosed().subscribe(result => {
        if (result) {
          this.categoriesService.deleteCategory(id)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: () => {
                this.toastr.success('Catégorie supprimée');
                this.router.navigate(['/categories']);
              },
              error: () => this.toastr.error('Erreur lors de la suppression')
            });
        }
      });
  }

  getDisplayName(cat: Category): string {
    const lang = this.translate.currentLang || 'fr';
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

  getBusinessTypeLabel(type: CategoryBusinessType): string {
    const labels: Record<CategoryBusinessType, string> = {
      RESTAURANT: 'Restaurant',
      GROCERY: 'Épicerie',
      PHARMACY: 'Pharmacie',
      OTHER: 'Autre',
    };
    return labels[type] ?? type;
  }

  getBusinessTypeIcon(type: CategoryBusinessType): string {
    const icons: Record<CategoryBusinessType, string> = {
      RESTAURANT: 'restaurant',
      GROCERY: 'local_grocery_store',
      PHARMACY: 'local_pharmacy',
      OTHER: 'category',
    };
    return icons[type] ?? 'category';
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
    const map: Record<string, string> = {
      CREATE: 'Créé',
      UPDATE: 'Modifié',
      DELETE: 'Supprimé',
      ACTIVATE: 'Activé',
      DEACTIVATE: 'Désactivé',
    };
    return map[action] ?? action;
  }

  formatTrend(value: number | undefined): string {
    if (value == null) return '0%';
    return `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`;
  }
}